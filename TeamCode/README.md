# Team 20287 BIOBUZZ TeamCode

This module contains the team-owned command framework for the 2026–2027 BIOBUZZ season. It uses FTC SDK 12.0 and does not depend on FTCLib.

## Structure

- `framework/command`: command lifecycle, subsystem requirements, defaults, and shutdown
- `drive`: pure mecanum math, drivetrain ownership, and the default TeleOp drive command
- `robot`: centralized hardware names and initialization
- `opmodes`: Driver Station entry points
- `testing`: Driver Station diagnostic OpModes

## FTCLib-inspired command structure

This is a small team-owned implementation, with no FTCLib runtime dependency or API-compatibility promise. It borrows the responsibility split from the official [CommandOpMode guide](https://docs.ftclib.org/ftclib/command-base/command-system/robot-and-commandopmode) and [scheduler guide](https://docs.ftclib.org/ftclib/command-base/command-system/command-scheduler):

- `PrecisionTeleOp.initialize(scheduler)` assembles hardware and binds its default drive command; `onLoop()` reports telemetry.
- `framework/command/CommandOpMode` owns START, scheduler iterations, and STOP cleanup, including initialization and loop exceptions. A cleanup failure is suppressed onto the original failure rather than replacing its diagnostic.
- `DriveSubsystem` owns motor output and stop; `TeleopDriveCommand` reads live inputs and declares its drive requirement.
- The existing `CommandScheduler` manages requirements, default commands and command lifecycle. It remains instance-owned per OpMode instead of adopting FTCLib's global singleton. Its existing scheduling order is preserved.

`MainTeleOp` keeps its original entry point and lifecycle. This structure follows the idea of [FTCLib's CommandOpMode source](https://github.com/FTCLib/FTCLib/blob/master/core/src/main/java/com/arcrobotics/ftclib/command/CommandOpMode.java), without copying the library or adding unused command groups/button bindings.

## Driver controls

- Left stick Y: forward and backward
- Left stick X: strafe
- Right stick X: turn

The command reads these values live during every scheduler cycle. Input deadband and output scale are centralized in `RobotConfig`.

`20287 Precision TeleOp` adds hold-to-slow control: hold gamepad 1 LB for a maximum 0.30 wheel-power limit (or the configured normal limit if lower), and release it to restore `DRIVE_POWER_SCALE`. The limit is applied after mecanum normalization. `20287 Main TeleOp` keeps its existing controls and fixed-scale behavior.

### OctoQuad headless drive

Select `20287 Headless TeleOp`. This requires an **OctoQuad FTC Edition MK2** with an IMU and SDK-compatible firmware major 3, configured under the I2C hardware name `octoquad` (`RobotConfig.OCTOQUAD_NAME`). MK1 cannot provide this heading source. No new library is required.

- Left stick: translate relative to the direction the robot faced at START, regardless of its current facing. After a 90-degree turn, stick-left still moves toward the driver's original left, and stick-forward-left still moves toward the driver's original forward-left.
- Right stick X: turn; LB: hold for the existing 30% wheel-output limit.
- The START reference stays fixed for the entire run; no gamepad button changes it.

Keep the robot stationary during INIT calibration and wait for `RUNNING`. Point its front toward the driver's chosen forward direction before START. The START hook reads a fresh valid OctoQuad packet and locks that heading before any drive command runs; if it is not ready, the OpMode exits with motors stopped so a later robot rotation cannot silently become the reference. Directional controls follow this fixed starting view; they do not track the driver's subsequent movement or turning. OctoQuad must be mounted in one of its supported axis-orthogonal orientations; it detects the vertical IMU axis automatically, so Control Hub mounting parameters are not used. Heading calibration/scale and physical performance still need verification on this robot.

The program uses only `heading_rad`; it preserves existing OctoQuad encoder direction, pod scale/offset, and heading-scalar settings, does not save flash, and resets/calibrates its localizer during INIT. It does not claim validated X/Y odometry. Each cycle requires a valid CRC, `RUNNING` status and finite heading; otherwise all wheel commands are zero. A reported missing IMU or communication exception exits through the normal scheduler cleanup.

With wheels raised, verify zero-heading forward/strafe/turn, then rotate the robot counterclockwise 90 degrees by hand and confirm heading increases about 90 degrees and stick-left produces robot-forward output, while stick-forward-left produces robot-forward-right output. Both are the original driver's left/forward-left directions. Repeat at 180 degrees and verify LB scaling and STOP before driving on the floor. Existing motor names/directions in `RobotConfig` still need to match your robot.

API references: the bundled `SensorOctoQuadLocalization` SDK sample and [DigitalChickenLabs localizer documentation](https://github.com/DigitalChickenLabs/OctoQuad/tree/master/documentation).

## Before running on a robot

1. Match the four names in `RobotConfig` to the Robot Controller configuration.
2. Lift the robot so every wheel is clear of the floor.
3. Verify each motor direction at low commanded power.
4. Verify forward, strafe, and turn controls independently.
5. Verify STOP immediately sets all drive powers to zero.
6. Record the verified names and directions in the review or commit that changes them.

Current status: software validation may pass, but hardware names, directions, controls, and emergency-stop behavior are unverified until the checklist above is completed on the current robot.

## Developer checks

Resolve FTC Knowledge Bank rules before editing:

```bash
python3 tools/FTC-Knowledge-Bank/.agents/skills/ftckb-integrate/scripts/project.py resolve --project .
```

After editing, run the Android build, project check, and integration verification with JDK 21 and a configured Android SDK:

```bash
./gradlew :TeamCode:assembleDebug
python3 tools/FTC-Knowledge-Bank/.agents/skills/ftckb-integrate/scripts/project.py check --project .
python3 tools/FTC-Knowledge-Bank/.agents/skills/ftckb-integrate/scripts/verify.py --project .
```

Compilation does not prove Android Studio Sync on another computer, Control Hub deployment, or physical robot correctness.

## Driver Station tests

- `TEST - Framework Self-Test` uses no robot hardware. Press PLAY and confirm every telemetry line reports PASS.
- `TEST - Drive Motors` requires the configured drivetrain. Lift the robot first, then hold exactly one face button: Y front-left, B front-right, X back-left, or A back-right. Power is limited to 0.15 and releasing the button commands every wheel to stop. An initialization, drive, or stop exception latches a FAULT and blocks further nonzero output until STOP and re-INIT; subsequent loops keep attempting to stop an available drivetrain. A communication failure can prevent a motor from receiving the stop command.

These tests live in `org.firstinspires.ftc.teamcode.testing` and use no JUnit dependency. Physical motor names, directions, and stop behavior remain unverified until the drive test is completed on the current robot.

## FTC Knowledge Bank status

This project pins FTC Knowledge Bank commit `5b652e8111769bfe259234d8f99e14f62aa549db` for team 20287, season 2026–2027, and the `command-based` profile.

The Knowledge Bank Dashboard uses this project's actual submodule knowledge directory. Follow the installed Agent Skill for `doctor`, Dashboard connection, `work resolve`, and the explicit receipt-associated `work check`. Rules, local builds, and simulated motor checks do not prove Driver Station or physical robot operation. Existing unrelated dependency changes are included in the full-workspace check and require their own vendor-evidence review.
