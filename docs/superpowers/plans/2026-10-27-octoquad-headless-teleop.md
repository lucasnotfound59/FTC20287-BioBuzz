# OctoQuad Headless TeleOp Implementation Plan

**Goal:** Add field-centric mecanum TeleOp using OctoQuad MK2 heading.

**Architecture:** An OctoQuad heading subsystem owns bulk reads and validates CRC/status. The existing drive command accepts an optional heading supplier and uses pure field-to-robot mixing. A new CommandOpMode binds gamepad controls and reports heading health.

**Tech Stack:** FTC SDK 12.0 OctoQuad API and the existing team-owned Command framework.

## Constraints

- Normal FTCKB mode, receipt-associated checks; no dependency or firmware changes.
- Preserve existing robot-centric entry points and constructor behavior.
- Left stick translation relative to the fixed START direction, right stick X turn, LB slow. No runtime heading-reference reset.
- Use configured name `octoquad`. Require MK2 and firmware major supported by SDK 12.0.
- Read heading only; preserve stored pod parameters and IMU scalar. INIT recalibrates with robot stationary. No motor output before START; invalid heading stops every wheel.
- Keep CCW-positive heading in radians internally, forward-positive and right-positive drive inputs.
- Keep the existing fixed FTCKB source; the submodule index was aligned with its already configured checkout. No commit/push.

## Tasks

- [x] Add `MecanumMixer.mixFieldCentric` and an optional heading-supplier constructor in `TeleopDriveCommand`. Existing callers retain robot-centric behavior. Apply joystick deadband before rotating, normalize wheels, then apply the live output limit. Stop when the heading is nonfinite.
- [x] Add `drive/OctoQuadHeadingSubsystem.java`; verify chip/firmware, reset/calibrate once, bulk-read once per cycle, and expose valid heading relative to a reference captured exactly once. Reading never changes the reference. Do not re-use a previous packet after CRC/status failure.
- [x] Add `RobotConfig.OCTOQUAD_NAME` and `RobotHardware.createOctoQuadHeading`; create `opmodes/HeadlessTeleOp.java`, register heading and drive, and show INIT calibration/runtime health. Use `CommandOpMode.onStart()` to lock a fresh valid heading before the scheduler runs. Stop if START data is not ready; never defer reference capture until later motion.
- [x] Update `TeamCode/README.md` with controls, MK2 requirement, orthogonal mounting/stationary calibration, hardware names and physical validation steps.
- [x] Verify actual command/mixer/sensor behavior in a temporary no-JUnit host harness: cardinal headings, wraparound, output scaling, live button inputs, bad CRC/calibration/NaN stops, and command end cleanup. Keep harness files outside the repository.
- [x] Run `./gradlew :TeamCode:compileDebugJavaWithJavac --offline --console=plain`, `git diff --check`, and receipt-associated normal FTCKB checks for the task patch and default whole workspace. Record soft review and hardware limitations. Do not deploy or operate motors.
