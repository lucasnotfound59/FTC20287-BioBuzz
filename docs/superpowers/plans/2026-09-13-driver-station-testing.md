# Driver Station Testing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the JUnit source set with hardware-free and guarded hardware test OpModes under the normal TeamCode package tree.

**Architecture:** `FrameworkSelfTest` runs deterministic command and mecanum checks using private fakes and a telemetry reporter, without reading `hardwareMap`. `DriveHardwareTest` uses the production `RobotHardware` and `DriveSubsystem` paths so only the drivetrain subsystem writes motor power.

**Tech Stack:** Java 8 source compatibility, FTC SDK 12.0 OpMode API, Gradle Android application module, FTC Knowledge Bank v2.

## Global Constraints

- Put test code only in `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/testing/`.
- Delete `TeamCode/src/test/` and remove JUnit from `TeamCode/build.gradle`.
- Do not add FTCLib or another testing dependency.
- Hardware test power is exactly `0.15`.
- A wheel moves only while exactly one mapped button is held; all other states stop all wheels.
- Keep hardware names and directions centralized in `RobotConfig` and initialization in `RobotHardware`.
- Do not insert meaningless Limelight calls to bypass the pinned FTCKB regex limitation.
- Robot validation remains unverified until both OpModes run on a lifted robot.

---

### Task 1: Hardware-free framework self-test OpMode

**Files:**
- Create: `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/testing/FrameworkSelfTest.java`

**Interfaces:**
- Consumes: `CommandScheduler`, `Command`, `Subsystem`, `MecanumMixer`, and `WheelPowers`.
- Produces: Driver Station OpMode `TEST - Framework Self-Test` in group `Testing`.

- [ ] **Step 1: Create the self-test shell and reporter**

Create a `LinearOpMode` with this exact lifecycle and reporting API:

```java
package org.firstinspires.ftc.teamcode.testing;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.firstinspires.ftc.teamcode.drive.MecanumMixer;
import org.firstinspires.ftc.teamcode.drive.WheelPowers;
import org.firstinspires.ftc.teamcode.framework.command.Command;
import org.firstinspires.ftc.teamcode.framework.command.CommandScheduler;
import org.firstinspires.ftc.teamcode.framework.command.Subsystem;

@TeleOp(name = "TEST - Framework Self-Test", group = "Testing")
public final class FrameworkSelfTest extends LinearOpMode {
    private final List<String> results = new ArrayList<>();
    private int passed;
    private int failed;

    @Override
    public void runOpMode() {
        telemetry.addLine("No hardware is used by this test.");
        telemetry.addLine("Press PLAY to run framework checks.");
        telemetry.update();
        waitForStart();
        if (isStopRequested()) {
            return;
        }

        runCheck("scheduler lifecycle", this::testSchedulerLifecycle);
        runCheck("requirement conflict", this::testRequirementConflict);
        runCheck("default command", this::testDefaultCommand);
        runCheck("same-cycle cancellation", this::testSameCycleCancellation);
        runCheck("initialization cleanup", this::testInitializationCleanup);
        runCheck("multi-requirement default", this::testMultiRequirementDefault);
        runCheck("shutdown cleanup", this::testShutdownCleanup);
        runCheck("mecanum mixing", this::testMecanumMixing);

        while (opModeIsActive()) {
            telemetry.clearAll();
            telemetry.addData("Result", failed == 0 ? "PASS" : "FAIL");
            telemetry.addData("Checks", "%d passed, %d failed", passed, failed);
            for (String result : results) {
                telemetry.addLine(result);
            }
            telemetry.addLine("Press STOP when finished.");
            telemetry.update();
            idle();
        }
    }

    private void runCheck(String name, Runnable check) {
        try {
            check.run();
            passed++;
            results.add("PASS: " + name);
        } catch (Throwable failure) {
            failed++;
            results.add("FAIL: " + name + " - " + failure.getMessage());
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void requireClose(double expected, double actual, String label) {
        if (Math.abs(expected - actual) > 1e-9) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }
}
```

- [ ] **Step 2: Add scheduler fakes and exact checks**

Inside `FrameworkSelfTest`, add these exact private fakes:

```java
private static final class FakeSubsystem implements Subsystem {
    int periodicCalls;
    int stopCalls;
    boolean throwOnStop;

    @Override
    public void periodic() {
        periodicCalls++;
    }

    @Override
    public void stop() {
        stopCalls++;
        if (throwOnStop) {
            throw new IllegalStateException("stop failed");
        }
    }
}

private static final class FakeCommand implements Command {
    private final Set<Subsystem> requirements;
    int initializeCalls;
    int executeCalls;
    int interruptedEnds;
    int normalEnds;
    boolean finished;
    boolean throwOnInitialize;
    boolean throwOnEnd;
    Runnable onExecute = () -> { };

    FakeCommand(Subsystem... requirements) {
        this.requirements = new LinkedHashSet<>(Arrays.asList(requirements));
    }

    @Override
    public void initialize() {
        initializeCalls++;
        if (throwOnInitialize) {
            throw new IllegalStateException("initialize failed");
        }
    }

    @Override
    public void execute() {
        executeCalls++;
        onExecute.run();
    }

    @Override
    public void end(boolean interrupted) {
        if (interrupted) {
            interruptedEnds++;
        } else {
            normalEnds++;
        }
        if (throwOnEnd) {
            throw new IllegalStateException("end failed");
        }
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public Set<Subsystem> getRequirements() {
        return requirements;
    }
}
```

Implement the seven scheduler methods with these required assertions:

```java
private void testSchedulerLifecycle() {
    CommandScheduler scheduler = new CommandScheduler();
    FakeSubsystem subsystem = new FakeSubsystem();
    FakeCommand command = new FakeCommand(subsystem);
    scheduler.registerSubsystem(subsystem);
    scheduler.schedule(command);
    scheduler.run();
    command.finished = true;
    scheduler.run();
    require(command.initializeCalls == 1, "initialize count");
    require(command.executeCalls == 2, "execute count");
    require(command.normalEnds == 1, "normal end count");
    require(!scheduler.isScheduled(command), "finished command remained scheduled");
}

private void testRequirementConflict() {
    CommandScheduler scheduler = new CommandScheduler();
    FakeSubsystem subsystem = new FakeSubsystem();
    FakeCommand first = new FakeCommand(subsystem);
    FakeCommand replacement = new FakeCommand(subsystem);
    scheduler.schedule(first);
    scheduler.schedule(replacement);
    require(first.interruptedEnds == 1, "first command was not interrupted");
    require(!scheduler.isScheduled(first), "first command remained scheduled");
    require(scheduler.isScheduled(replacement), "replacement was not scheduled");
}

private void testDefaultCommand() {
    CommandScheduler scheduler = new CommandScheduler();
    FakeSubsystem subsystem = new FakeSubsystem();
    FakeCommand command = new FakeCommand(subsystem);
    scheduler.registerSubsystem(subsystem);
    scheduler.setDefaultCommand(subsystem, command);
    scheduler.run();
    require(command.initializeCalls == 1, "default was not initialized");
    require(command.executeCalls == 1, "default did not execute");
    require(subsystem.periodicCalls == 1, "subsystem periodic did not run");
}

private void testSameCycleCancellation() {
    CommandScheduler scheduler = new CommandScheduler();
    FakeSubsystem firstSubsystem = new FakeSubsystem();
    FakeSubsystem secondSubsystem = new FakeSubsystem();
    FakeCommand canceling = new FakeCommand(firstSubsystem);
    FakeCommand canceled = new FakeCommand(secondSubsystem);
    FakeCommand replacement = new FakeCommand(secondSubsystem);
    canceling.onExecute = () -> scheduler.schedule(replacement);
    scheduler.schedule(canceling);
    scheduler.schedule(canceled);
    scheduler.run();
    require(canceled.interruptedEnds == 1, "canceled command was not ended");
    require(canceled.executeCalls == 0, "canceled command executed later in cycle");
    require(scheduler.isScheduled(replacement), "replacement was not scheduled");
}

private void testInitializationCleanup() {
    CommandScheduler scheduler = new CommandScheduler();
    FakeSubsystem subsystem = new FakeSubsystem();
    FakeCommand failed = new FakeCommand(subsystem);
    FakeCommand replacement = new FakeCommand(subsystem);
    failed.throwOnInitialize = true;
    boolean threw = false;
    try {
        scheduler.schedule(failed);
    } catch (IllegalStateException expected) {
        threw = true;
    }
    require(threw, "initialization failure was not propagated");
    require(!scheduler.isScheduled(failed), "failed command remained scheduled");
    require(failed.interruptedEnds == 1, "failed command was not cleaned up");
    scheduler.schedule(replacement);
    require(scheduler.isScheduled(replacement), "requirement was not released");
}

private void testMultiRequirementDefault() {
    CommandScheduler scheduler = new CommandScheduler();
    FakeSubsystem owner = new FakeSubsystem();
    FakeSubsystem shared = new FakeSubsystem();
    FakeCommand active = new FakeCommand(shared);
    FakeCommand defaultCommand = new FakeCommand(owner, shared);
    scheduler.registerSubsystem(owner);
    scheduler.registerSubsystem(shared);
    scheduler.setDefaultCommand(owner, defaultCommand);
    scheduler.schedule(active);
    scheduler.run();
    require(scheduler.isScheduled(active), "active command was interrupted");
    require(!scheduler.isScheduled(defaultCommand), "blocked default was scheduled");
}

private void testShutdownCleanup() {
    CommandScheduler scheduler = new CommandScheduler();
    FakeSubsystem firstSubsystem = new FakeSubsystem();
    FakeSubsystem secondSubsystem = new FakeSubsystem();
    FakeCommand first = new FakeCommand(firstSubsystem);
    FakeCommand second = new FakeCommand(secondSubsystem);
    first.throwOnEnd = true;
    firstSubsystem.throwOnStop = true;
    scheduler.registerSubsystem(firstSubsystem);
    scheduler.registerSubsystem(secondSubsystem);
    scheduler.schedule(first);
    scheduler.schedule(second);
    boolean threw = false;
    try {
        scheduler.shutdown();
    } catch (IllegalStateException expected) {
        threw = true;
        require(expected.getSuppressed().length == 1, "cleanup failure was not aggregated");
    }
    require(threw, "cleanup failure was not propagated");
    require(first.interruptedEnds == 1 && second.interruptedEnds == 1,
            "not every command was ended");
    require(firstSubsystem.stopCalls == 1 && secondSubsystem.stopCalls == 1,
            "not every subsystem was stopped");
}
```

- [ ] **Step 3: Add the mecanum calculation checks**

Implement one method that checks zero, forward, strafe, turn, and normalized mixed input:

```java
private void testMecanumMixing() {
    requirePowers(MecanumMixer.mix(0, 0, 0), 0, 0, 0, 0);
    requirePowers(MecanumMixer.mix(1, 0, 0), 1, 1, 1, 1);
    requirePowers(MecanumMixer.mix(0, 1, 0), 1, -1, -1, 1);
    requirePowers(MecanumMixer.mix(0, 0, 1), 1, -1, 1, -1);
    WheelPowers mixed = MecanumMixer.mix(1, 1, 1);
    requirePowers(mixed, 1, -1.0 / 3.0, 1.0 / 3.0, 1.0 / 3.0);
    require(mixed.maxMagnitude() <= 1.0, "mixed output exceeded one");
}

private static void requirePowers(
        WheelPowers actual,
        double frontLeft,
        double frontRight,
        double backLeft,
        double backRight) {
    requireClose(frontLeft, actual.frontLeft, "front-left");
    requireClose(frontRight, actual.frontRight, "front-right");
    requireClose(backLeft, actual.backLeft, "back-left");
    requireClose(backRight, actual.backRight, "back-right");
}
```

- [ ] **Step 4: Compile the new OpMode**

Run:

```bash
./gradlew :TeamCode:compileDebugJavaWithJavac
```

Expected: `BUILD SUCCESSFUL`; `FrameworkSelfTest` compiles as production TeamCode without JUnit.

- [ ] **Step 5: Commit the framework self-test**

```bash
git add -- TeamCode/src/main/java/org/firstinspires/ftc/teamcode/testing/FrameworkSelfTest.java
git commit -m "test: add Driver Station framework self-test"
```

---

### Task 2: Guarded individual motor test OpMode

**Files:**
- Create: `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/testing/DriveHardwareTest.java`

**Interfaces:**
- Consumes: `RobotHardware.createDrive(HardwareMap)`, `RobotConfig` motor names, `DriveSubsystem.drive(WheelPowers)`, and `DriveSubsystem.stop()`.
- Produces: Driver Station OpMode `TEST - Drive Motors` in group `Testing`.

- [ ] **Step 1: Implement initialization and fixed safety settings**

Create an iterative `OpMode` with `private static final double TEST_POWER = 0.15`, a nullable `DriveSubsystem`, and an initialization error string. `init()` must catch `RuntimeException`, retain the message for telemetry, and leave the drive unavailable rather than starting motors.

```java
package org.firstinspires.ftc.teamcode.testing;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.firstinspires.ftc.teamcode.drive.DriveSubsystem;
import org.firstinspires.ftc.teamcode.drive.WheelPowers;
import org.firstinspires.ftc.teamcode.robot.RobotConfig;
import org.firstinspires.ftc.teamcode.robot.RobotHardware;

@TeleOp(name = "TEST - Drive Motors", group = "Testing")
public final class DriveHardwareTest extends OpMode {
    private static final double TEST_POWER = 0.15;
    private DriveSubsystem drive;
    private String error;

    @Override
    public void init() {
        try {
            drive = RobotHardware.createDrive(hardwareMap);
        } catch (RuntimeException failure) {
            error = failure.getMessage();
        }
        showInstructions("STOPPED");
    }
}
```

- [ ] **Step 2: Implement hold-to-run single-wheel control**

`loop()` must count Y/B/X/A presses, stop for zero or multiple presses, and otherwise send exactly one non-zero wheel value:

```java
@Override
public void loop() {
    if (drive == null) {
        showInstructions("ERROR: " + error);
        return;
    }

    int pressed = (gamepad1.y ? 1 : 0)
            + (gamepad1.b ? 1 : 0)
            + (gamepad1.x ? 1 : 0)
            + (gamepad1.a ? 1 : 0);
    if (pressed != 1) {
        safeStop();
        showInstructions(pressed == 0 ? "STOPPED" : "STOPPED: hold one button only");
        return;
    }

    WheelPowers powers;
    String selected;
    if (gamepad1.y) {
        powers = new WheelPowers(TEST_POWER, 0, 0, 0);
        selected = "front-left";
    } else if (gamepad1.b) {
        powers = new WheelPowers(0, TEST_POWER, 0, 0);
        selected = "front-right";
    } else if (gamepad1.x) {
        powers = new WheelPowers(0, 0, TEST_POWER, 0);
        selected = "back-left";
    } else {
        powers = new WheelPowers(0, 0, 0, TEST_POWER);
        selected = "back-right";
    }

    try {
        drive.drive(powers);
        showInstructions("RUNNING: " + selected);
    } catch (RuntimeException failure) {
        error = failure.getMessage();
        safeStop();
        showInstructions("ERROR: " + error);
    }
}
```

- [ ] **Step 3: Implement stop and telemetry paths**

Use `safeStop()` in all non-running paths and from `stop()`. Telemetry must show the lift-robot warning, button mapping, configured motor names, power, and current state:

```java
private void safeStop() {
    if (drive == null) {
        return;
    }
    try {
        drive.stop();
    } catch (RuntimeException failure) {
        error = failure.getMessage();
    }
}

private void showInstructions(String state) {
    telemetry.clearAll();
    telemetry.addLine("LIFT THE ROBOT: all wheels must be clear.");
    telemetry.addData("State", state);
    telemetry.addData("Power", TEST_POWER);
    telemetry.addLine("Hold exactly one button:");
    telemetry.addLine("Y front-left | B front-right");
    telemetry.addLine("X back-left  | A back-right");
    telemetry.addData("front-left name", RobotConfig.FRONT_LEFT_DRIVE);
    telemetry.addData("front-right name", RobotConfig.FRONT_RIGHT_DRIVE);
    telemetry.addData("back-left name", RobotConfig.BACK_LEFT_DRIVE);
    telemetry.addData("back-right name", RobotConfig.BACK_RIGHT_DRIVE);
    if (error != null) {
        telemetry.addData("Last error", error);
    }
    telemetry.update();
}

@Override
public void stop() {
    safeStop();
}
```

- [ ] **Step 4: Compile the hardware test**

Run:

```bash
./gradlew :TeamCode:compileDebugJavaWithJavac
```

Expected: `BUILD SUCCESSFUL`; both testing OpModes are packaged into TeamCode.

- [ ] **Step 5: Commit the hardware test**

```bash
git add -- TeamCode/src/main/java/org/firstinspires/ftc/teamcode/testing/DriveHardwareTest.java
git commit -m "test: add guarded drivetrain hardware opmode"
```

---

### Task 3: Remove JUnit source set and document Driver Station testing

**Files:**
- Delete: `TeamCode/src/test/java/org/firstinspires/ftc/teamcode/framework/command/CommandSchedulerTest.java`
- Delete: `TeamCode/src/test/java/org/firstinspires/ftc/teamcode/drive/MecanumMixerTest.java`
- Delete: `TeamCode/src/test/java/org/firstinspires/ftc/teamcode/drive/DriveSubsystemTest.java`
- Modify: `TeamCode/build.gradle`
- Modify: `TeamCode/README.md`

**Interfaces:**
- Consumes: the two OpModes from Tasks 1 and 2.
- Produces: a JUnit-free TeamCode module and operator-facing test instructions.

- [ ] **Step 1: Remove the JUnit tests and dependency**

Delete `TeamCode/src/test/` and change the dependency block to exactly:

```groovy
dependencies {
    implementation project(':FtcRobotController')
}
```

- [ ] **Step 2: Update TeamCode documentation**

Add `testing` to the Structure section. Replace the JUnit developer command with `./gradlew :TeamCode:assembleDebug`. Add a `Driver Station tests` section containing:

```markdown
## Driver Station tests

- `TEST - Framework Self-Test` uses no robot hardware. Press PLAY and confirm every telemetry line reports PASS.
- `TEST - Drive Motors` requires the configured drivetrain. Lift the robot first, then hold exactly one face button: Y front-left, B front-right, X back-left, or A back-right. Power is limited to 0.15 and releasing the button stops every wheel.

These tests live in `org.firstinspires.ftc.teamcode.testing` and use no JUnit dependency. Physical motor names, directions, and stop behavior remain unverified until the drive test is completed on the current robot.
```

- [ ] **Step 3: Prove JUnit is absent and the layout is correct**

Run:

```bash
test ! -d TeamCode/src/test
! rg -n 'org\.junit|@Test|testImplementation[^\n]*junit' TeamCode/src TeamCode/build.gradle
find TeamCode/src/main/java/org/firstinspires/ftc/teamcode/testing -maxdepth 1 -type f -print | sort
```

Expected: the first two commands exit 0; the last command prints only `DriveHardwareTest.java` and `FrameworkSelfTest.java`.

- [ ] **Step 4: Run the Android build**

Run:

```bash
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
ANDROID_HOME='/Users/xinlu/Library/Android/sdk' \
GRADLE_USER_HOME=/private/tmp/ftc20287-baseline-gradle \
./gradlew :TeamCode:assembleDebug --no-daemon
```

Expected: `BUILD SUCCESSFUL` and `TeamCode/build/outputs/apk/debug/TeamCode-debug.apk` exists.

- [ ] **Step 5: Run FTCKB checks**

Run the default check before committing:

```bash
GRADLE_USER_HOME=/private/tmp/ftc20287-baseline-gradle \
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
python3 tools/FTC-Knowledge-Bank/.agents/skills/ftckb-integrate/scripts/project.py check --project .
```

Expected: no path/customization violation. The pinned knowledge base may return exit 1 only for `shared.limelight-check-result-validity` and `shared.limelight-enforce-freshness-policy` against newly added Java files; confirm no `Limelight3A`, `LLResult`, or `getLatestResult` usage and report the documented false positive without bypass code.

- [ ] **Step 6: Commit the migration**

```bash
git add -- TeamCode/build.gradle TeamCode/README.md TeamCode/src/test
git commit -m "test: replace JUnit with Driver Station diagnostics"
```

- [ ] **Step 7: Verify the clean committed project**

```bash
GRADLE_USER_HOME=/private/tmp/ftc20287-baseline-gradle \
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
python3 tools/FTC-Knowledge-Bank/.agents/skills/ftckb-integrate/scripts/verify.py --project .
git diff --check HEAD~3..HEAD
git status --short
```

Expected: `installationOk: true`, `projectCheck.ok: true`, `git diff --check` exits 0, and `git status --short` prints nothing. Report that Driver Station and physical robot execution were not performed locally.
