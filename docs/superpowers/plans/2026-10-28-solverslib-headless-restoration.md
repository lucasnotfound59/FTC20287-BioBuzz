# Restore fixed-reference headless drive in SolversLib

Goal: retain the current SolversLib 0.3.4 architecture and restore the driver's fixed START-frame mecanum controls.

Evidence: the exact official `core-0.3.4-sources.jar` CommandOpMode calls initialize, initialize_loop, run and end; it never calls preRun. Its reset replaces the scheduler without stopping motors. The unmodified Gradle baseline also fails because SolversLib's transitive SDK 11.1 constraints conflict with the app's SDK 12.0.

Implementation:

- Keep teleops/FieldCentricDrive, commands/DriveCommand and subsystems/Drive/OctoQuadSubsystem. Do not restore the deleted custom framework or change the fixed SDK/library versions.
- Capture one fresh valid OctoQuad heading before the first active scheduler run. Reject invalid START data. Never rebase during the run; left and forward-left stay relative to the original driver's view at any robot heading. Use if/else throughout.
- Restore live LB slow control, capped at 0.30 after mecanum normalization. Preserve the migrated frontLeft/frontRight/backLeft/backRight hardware names and direction configuration.
- Stop every wheel on command interruption, OpMode exit and exceptions, retain the original failure if cleanup also fails, and clean the singleton scheduler after failed initialization.
- Exclude only SolversLib's transitive org.firstinspires.ftc SDK dependencies so the app's explicit SDK 12.0 artifacts own the version selection.
- Update the current TeamCode readme with actual entry name, controls, lifecycle, hardware requirements and validation limits.

Verification:

- Reproduce the missing START capture against the unmodified source using the actual SolversLib 0.3.4 AAR in a host harness with FTC device stubs.
- Verify first-active capture, nonzero raw START angle, no INIT output, 90/180-degree left and diagonal movement, no runtime rebase, live LB cap, CRC/calibration/NaN handling, all-wheel stop and original-error preservation.
- Run Android TeamCode compilation and inspect resolved SDK dependencies; run normal receipt-associated full-workspace FTCKB checks and diff whitespace checks.
- Request an independent read-only code review. Keep temporary harnesses/artifacts outside the repository. No hardware actuation, deployment, commit, push or dependency upgrade.
