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

        runCheck("numeric assertion guard", this::testNumericAssertionGuard);
        runCheck("scheduler lifecycle", this::testSchedulerLifecycle);
        runCheck("requirement conflict", this::testRequirementConflict);
        runCheck("default command", this::testDefaultCommand);
        runCheck("same-cycle cancellation", this::testSameCycleCancellation);
        runCheck("initialization cleanup", this::testInitializationCleanup);
        runCheck("multi-requirement default", this::testMultiRequirementDefault);
        runCheck("shutdown cleanup and terminal state", this::testShutdownCleanup);
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
            String message = failure.getMessage();
            results.add("FAIL: " + name + " - "
                    + (message == null || message.trim().isEmpty()
                    ? failure.getClass().getName() : message));
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void requireClose(double expected, double actual, String label) {
        if (Double.isNaN(expected) || Double.isInfinite(expected)
                || Double.isNaN(actual) || Double.isInfinite(actual)
                || Math.abs(expected - actual) > 1e-9) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }

    private void testNumericAssertionGuard() {
        requireClose(1.0, 1.0, "equal finite values");
        requireClose(1.0, 1.0 + 1e-10, "within tolerance");
        requireCloseRejected(0.0, 1.0);
        for (double invalid : new double[] {
                Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            requireCloseRejected(0.0, invalid);
            requireCloseRejected(invalid, 0.0);
            requireCloseRejected(invalid, invalid);
        }
    }

    private static void requireCloseRejected(double expected, double actual) {
        boolean rejected = false;
        try {
            requireClose(expected, actual, "invalid comparison");
        } catch (AssertionError expectedFailure) {
            rejected = true;
        }
        require(rejected, "numeric assertion accepted " + expected + " versus " + actual);
    }

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
        checkShutdownCleanup(false);
        checkShutdownCleanup(true);
    }

    private void checkShutdownCleanup(boolean failCleanup) {
        CommandScheduler scheduler = new CommandScheduler();
        FakeSubsystem firstSubsystem = new FakeSubsystem();
        FakeSubsystem secondSubsystem = new FakeSubsystem();
        FakeCommand first = new FakeCommand(firstSubsystem);
        FakeCommand second = new FakeCommand(secondSubsystem);
        first.throwOnEnd = failCleanup;
        firstSubsystem.throwOnStop = failCleanup;
        scheduler.registerSubsystem(firstSubsystem);
        scheduler.registerSubsystem(secondSubsystem);
        scheduler.schedule(first);
        scheduler.schedule(second);
        scheduler.run();
        int firstExecutions = first.executeCalls;
        int secondExecutions = second.executeCalls;
        boolean threw = false;
        try {
            scheduler.shutdown();
        } catch (IllegalStateException expected) {
            threw = true;
            require(expected.getSuppressed().length == 1, "cleanup failure was not aggregated");
        }
        require(threw == failCleanup, "unexpected cleanup failure propagation");
        require(first.interruptedEnds == 1 && second.interruptedEnds == 1,
                "not every command was ended");
        require(firstSubsystem.stopCalls == 1 && secondSubsystem.stopCalls == 1,
                "not every subsystem was stopped");
        require(!scheduler.isScheduled(first) && !scheduler.isScheduled(second),
                "shutdown command remained scheduled");

        boolean runRejected = false;
        try {
            scheduler.run();
        } catch (IllegalStateException expected) {
            runRejected = true;
        }
        require(runRejected, "shutdown scheduler accepted run");
        require(first.executeCalls == firstExecutions && second.executeCalls == secondExecutions,
                "command executed after shutdown");
    }

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
}
