# Team 20287 BIOBUZZ TeamCode

This module contains the team-owned command framework for the 2026–2027 BIOBUZZ season. It uses FTC SDK 12.0 and does not depend on FTCLib.

## Structure

- `framework/command`: command lifecycle, subsystem requirements, defaults, and shutdown
- `drive`: pure mecanum math, drivetrain ownership, and the default TeleOp drive command
- `robot`: centralized hardware names and initialization
- `opmodes`: Driver Station entry points

## Driver controls

- Left stick Y: forward and backward
- Left stick X: strafe
- Right stick X: turn

The command reads these values live during every scheduler cycle. Input deadband and output scale are centralized in `RobotConfig`.

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

After editing, run the unit tests, Android build, project check, and integration verification with JDK 21 and a configured Android SDK:

```bash
./gradlew :TeamCode:testDebugUnitTest :TeamCode:assembleDebug
python3 tools/FTC-Knowledge-Bank/.agents/skills/ftckb-integrate/scripts/project.py check --project .
python3 tools/FTC-Knowledge-Bank/.agents/skills/ftckb-integrate/scripts/verify.py --project .
```

Compilation does not prove Android Studio Sync on another computer, Control Hub deployment, or physical robot correctness.
