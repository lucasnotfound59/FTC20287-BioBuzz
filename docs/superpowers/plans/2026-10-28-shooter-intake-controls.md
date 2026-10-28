# Shooter and Intake controls

> Historical document: FTC Knowledge Bank was removed from this project on 2026-10-28. FTCKB integration steps, rules, and check commands below are retained as history and no longer apply.

Goal: complete the single-motor Shooter and add a single-motor Intake in the existing SolversLib 0.3.4 architecture, then bind both in the headless TeleOp.

Implementation:

- Shooter owns motor `shooter`; Intake owns motor `intake`. Both initialize to zero power, FORWARD direction, BRAKE and RUN_WITHOUT_ENCODER. Public tunable power constants start at 1.0; these are commands, not measured or tuned performance.
- Shooter exposes shoot(), setPower(double), stop() and getCommandedPower(). Intake exposes intake(), setPower(double), stop() and getCommandedPower(). Preserve the original Shooter setPower/stop interface. Reject nonfinite input, clamp to [-1,1], attempt zero on write/configuration failure, and preserve the original error.
- ShootCommand and IntakeCommand each own only their corresponding subsystem and read a live BooleanSupplier every execute. Gamepad 1 A holds intake on; B holds shooter on. Release or interruption stops it. Both may run at once; no motion is scheduled during INIT.
- FieldCentricDrive creates/registers both via SubsystemBase, sets their default commands, reports commanded power and explicitly stops every available subsystem even if cancellation or another stop fails. Keep headless START reference, LB slow control, hardware/dependency versions and existing uncommitted work intact.
- Use if/else only. Update the current readme with hardware names, controls, power constants and physical verification boundaries.

Validation:

- A host harness using the actual pinned SolversLib AAR covers both motor classes, live/held/released/combined buttons, no INIT output, command requirements/interruption, STOP, invalid input, supplier failures, missing hardware and independent cleanup with shared RuntimeException/Error objects.
- Run existing headless actual-library regressions with the newly required motor fixtures, offline APK build, normal scoped/full-workspace FTCKB checks, whitespace checks and independent read-only review. Keep harness files outside TeamCode; no JUnit, deployment, commit or push.
