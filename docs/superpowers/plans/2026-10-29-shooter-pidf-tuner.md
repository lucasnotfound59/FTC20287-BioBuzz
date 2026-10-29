# Shooter PIDF Tuner Implementation Plan

**Goal:** Manually tune the existing Hub velocity controller and compare four-ball speed loss/recovery at a fixed shooting location.

**Architecture:** A standalone Testing OpMode uses the same shooter direction and Hub PIDF API as production. A hardware-independent measurement class tracks readiness and four manually marked shot windows. Dashboard and gamepad edit parameters; a CSV summary records results. No autonomous firing or assumed ball sensor.

**Tech Stack:** FTC SDK 12.0.0, FTC Dashboard 0.6.0, Java 8; existing dependencies only.

## Scope and decisions

- Prefer native Hub PIDF to an extra software PID loop: it matches existing `setVelocity` behavior. Open-loop power alone cannot measure closed-loop recovery.
- Use ticks/s, initially 1560 from `Shooter.SHOOT_VEL`; no invented encoder-to-RPM conversion.
- RB holds shooter, LB optionally holds existing intake, A manually marks each shot, X clears a trial, B stops until RB release, Y saves a summary while stopped.
- Default continuous intake exposes actual burst losses. Optional readiness gating requires speed within a tolerance for a dwell period; it cannot guarantee one ball per pulse.
- INIT applies no nonzero motor output. Invalid settings stop both motors and require RB release. STOP/exception attempts both stops and restores original Hub coefficients.
- Record four manual markers, pre-shot speed, minimum speed, target-relative drop, overshoot, and recovery only after an observed departure from tolerance. End an observation on the next marker, timeout, or motor stop; distinguish interruption and missing recovery.
- Reset a trial after any parameter change so results never mix tunings. Restore Hub coefficients on exit. Persist selected production values explicitly in Shooter constants; never silently install experimental values.
- Keep existing Localizer edits and untracked tools untouched. Work in the current checkout as requested.

## Tasks and validation

- [x] Add pure readiness/four-shot measurement logic and deterministic checks for dips, overlapping windows, timeout, interruption, reset, and four-marker limit.
- [x] Add Shooter opt-in PIDF constants (disabled by default) and explicit velocity run mode.
- [x] Add tuner OpMode, Dashboard graphs, manual parameter controls, optional intake, stop handling, and CSV export.
- [x] Add Chinese operator instructions covering F/P/I/D tuning, comparison of burst versus readiness gating, fixed geometry, parameter transfer, and unverified hardware behavior.
- [x] Run the affected JVM checks and TeamCode debug build; review lifecycle/control paths. No robot deployment or live shot validation is implied.

## Validation results

- `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' sh tests/shooter/run-checks.sh`: 30 checks passed.
- `./gradlew :TeamCode:assembleDebug --offline --console=plain` with the installed JDK/Android SDK: BUILD SUCCESSFUL; existing Java 8/Gradle deprecation warnings only.
- Independent review found the SDK power-cache stop issue below; fixed and re-reviewed with no remaining concrete blocker.
- `git diff --check`: passed. Existing Localizer changes and untracked tools preserved. No commit, push, deployment, or real-ball validation performed.

## Review finding addressed

SDK 12.0 `LynxDcMotorController.setMotorVelocity()` does not invalidate the cached power used by `internalSetMotorPower()`. Repeated `setPower(0)` can therefore be suppressed after a brief velocity command. The tuner and production Shooter now stop via `setVelocity(0)` consistently. A source-derived one-off Java cache simulation failed with the old `Shooter.stop()` (target stayed 1560) and passed after the fix (target 0). This is a desk regression check, not a hardware timing measurement.
