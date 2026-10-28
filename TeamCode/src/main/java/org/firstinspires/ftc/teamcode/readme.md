# TeamCode

Team-owned FTC code for BIOBUZZ. The command architecture uses SolversLib's official command system (`org.solverslib:core:0.3.4`), following its subsystem, command, and `CommandOpMode` lifecycle model.

## Source layout

- `teleops/`: Driver Station TeleOp entry points. `FieldCentricDrive` extends SolversLib's `CommandOpMode` directly and installs `DriveCommand` as the drivetrain default.
- `autos/`: autonomous OpModes and autonomous command compositions.
- `subsystems/`: hardware ownership. `Drive` owns the four drive motors; `Shooter` owns one shooter motor; `OctoQuadSubsystem` provides a validated heading relative to the direction captured at START.
- `commands/`: high-level actions. `DriveCommand` reads live input suppliers and requires `Drive`.
- `tests/`: Driver Station diagnostic OpModes.

Subsystems encapsulate hardware. Commands declare their subsystem requirements with `addRequirements(...)`, implement the command lifecycle, and use subsystem methods instead of writing hardware directly. SolversLib's `CommandOpMode` runs its scheduler and handles scheduler reset when the OpMode ends.

`FieldCentricDrive` is registered in the Driver Station. The left stick translates relative to the forward direction captured at START, and right stick X turns the robot. OctoQuad MK2 is required; invalid heading data stops drive output. Shooter speed control is intentionally unspecified; its constructor fetches the motor named `shooter` from `HardwareMap`, and desired behavior belongs in a command.

`Drive` and `Shooter` fetch their hardware directly from `HardwareMap` in their constructors. The drive motor names are written at the lookup calls, and the class preserves the current left-reverse/right-forward direction setup. Verify the names and mapping on the robot before operating it.

## Reference

- [SolversLib command system](https://docs.seattlesolvers.com/command-base/command-system)
- [SolversLib CommandOpMode](https://docs.seattlesolvers.com/command-base/command-system/robot-and-commandopmode)
- [SolversLib installation](https://docs.seattlesolvers.com/installation)
