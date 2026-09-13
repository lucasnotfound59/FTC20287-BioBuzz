# Team 20287 BIOBUZZ TeamCode

This module contains the team-owned command framework for the 2026–2027 BIOBUZZ season. It uses FTC SDK 12.0 and does not depend on FTCLib.

## Structure

- `framework/command`: command lifecycle, subsystem requirements, defaults, and shutdown
- `drive`: pure mecanum math, drivetrain ownership, and the default TeleOp drive command
- `robot`: centralized hardware names and initialization
- `opmodes`: Driver Station entry points
- `testing`: Driver Station diagnostic OpModes

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

After editing, run the Android build, project check, and integration verification with JDK 21 and a configured Android SDK:

```bash
./gradlew :TeamCode:assembleDebug
python3 tools/FTC-Knowledge-Bank/.agents/skills/ftckb-integrate/scripts/project.py check --project .
python3 tools/FTC-Knowledge-Bank/.agents/skills/ftckb-integrate/scripts/verify.py --project .
```

Compilation does not prove Android Studio Sync on another computer, Control Hub deployment, or physical robot correctness.

## Driver Station tests

- `TEST - Framework Self-Test` uses no robot hardware. Press PLAY and confirm every telemetry line reports PASS.
- `TEST - Drive Motors` requires the configured drivetrain. Lift the robot first, then hold exactly one face button: Y front-left, B front-right, X back-left, or A back-right. Power is limited to 0.15 and releasing the button stops every wheel.

These tests live in `org.firstinspires.ftc.teamcode.testing` and use no JUnit dependency. Physical motor names, directions, and stop behavior remain unverified until the drive test is completed on the current robot.

## FTC Knowledge Bank status

This project pins FTC Knowledge Bank commit `105c8e47ddee734365af50dd2305caa13646ab21` for team 20287, season 2026–2027, and the `command-based` profile.

The installation and current-worktree verification pass. A full branch-diff check reports `shared.limelight-check-result-validity` and `shared.limelight-enforce-freshness-policy` against `DriveSubsystem.java`, even though TeamCode contains no Limelight result read. The pinned rules currently apply their required regular expressions to every added Java file. Do not add meaningless `isValid()` or timestamp calls to silence this known false positive; report the two rule IDs until the Knowledge Bank narrows their applicability.
