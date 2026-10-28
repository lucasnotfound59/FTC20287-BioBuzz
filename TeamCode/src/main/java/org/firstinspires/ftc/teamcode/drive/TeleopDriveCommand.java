package org.firstinspires.ftc.teamcode.drive;

import java.util.function.DoubleSupplier;
import org.firstinspires.ftc.teamcode.framework.command.CommandBase;

/** Default drive command that reads live gamepad values every scheduler cycle. */
public final class TeleopDriveCommand extends CommandBase {
    private final DriveSubsystem drive;
    private final DoubleSupplier forward;
    private final DoubleSupplier strafe;
    private final DoubleSupplier turn;
    private final double deadband;
    private final double scale;
    private final DoubleSupplier outputScale;
    private final DoubleSupplier headingRadians;

    public TeleopDriveCommand(
            DriveSubsystem drive,
            DoubleSupplier forward,
            DoubleSupplier strafe,
            DoubleSupplier turn,
            double deadband,
            double scale) {
        this(drive, forward, strafe, turn, deadband, scale, null, null);
    }

    /** Live wheel-output limit, applied after mecanum normalization. */
    public TeleopDriveCommand(
            DriveSubsystem drive,
            DoubleSupplier forward,
            DoubleSupplier strafe,
            DoubleSupplier turn,
            double deadband,
            DoubleSupplier outputScale) {
        this(drive, forward, strafe, turn, deadband, 1.0, requireScaleSupplier(outputScale), null);
    }

    /** Field-centric mode; a nonfinite heading stops output until a valid reading arrives. */
    public TeleopDriveCommand(
            DriveSubsystem drive,
            DoubleSupplier forward,
            DoubleSupplier strafe,
            DoubleSupplier turn,
            double deadband,
            DoubleSupplier outputScale,
            DoubleSupplier headingRadians) {
        this(drive, forward, strafe, turn, deadband, 1.0,
                requireScaleSupplier(outputScale), requireHeadingSupplier(headingRadians));
    }

    private TeleopDriveCommand(
            DriveSubsystem drive,
            DoubleSupplier forward,
            DoubleSupplier strafe,
            DoubleSupplier turn,
            double deadband,
            double scale,
            DoubleSupplier outputScale,
            DoubleSupplier headingRadians) {
        if (drive == null || forward == null || strafe == null || turn == null) {
            throw new IllegalArgumentException("drive and input suppliers cannot be null");
        }
        if (!Double.isFinite(deadband) || deadband < 0.0 || deadband >= 1.0) {
            throw new IllegalArgumentException("deadband must be in [0, 1)");
        }
        if (!Double.isFinite(scale) || scale < 0.0 || scale > 1.0) {
            throw new IllegalArgumentException("scale must be in [0, 1]");
        }

        this.drive = drive;
        this.forward = forward;
        this.strafe = strafe;
        this.turn = turn;
        this.deadband = deadband;
        this.scale = scale;
        this.outputScale = outputScale;
        this.headingRadians=headingRadians;
        addRequirements(drive);
    }

    @Override
    public void execute() {
        try {
            double limit;
            if (outputScale == null) {
                limit=1.0;
            } else {
                limit=outputScale.getAsDouble();
            }
            if (!Double.isFinite(limit) || limit < 0.0 || limit > 1.0) {
                throw new IllegalArgumentException("output scale must be finite and in [0, 1]");
            }
            double forwardValue=applyDeadband(forward.getAsDouble())*scale;
            double strafeValue=applyDeadband(strafe.getAsDouble())*scale;
            double turnValue=applyDeadband(turn.getAsDouble())*scale;
            double heading;
            if (headingRadians == null) {
                heading=0.0;
            } else {
                heading=headingRadians.getAsDouble();
            }
            if (!Double.isFinite(heading)) {
                drive.stop();
                return;
            }
            WheelPowers powers;
            if (headingRadians == null) {
                powers=MecanumMixer.mix(forwardValue, strafeValue, turnValue);
            } else {
                powers=MecanumMixer.mixFieldCentric(forwardValue, strafeValue, turnValue, heading);
            }
            drive.drive(new WheelPowers(
                    powers.frontLeft*limit, powers.frontRight*limit,
                    powers.backLeft*limit, powers.backRight*limit));
        } catch (RuntimeException | Error failure) {
            try {
                drive.stop();
            } catch (RuntimeException | Error stopFailure) {
                if (stopFailure != failure) {
                    failure.addSuppressed(stopFailure);
                }
            }
            throw failure;
        }
    }

    @Override
    public void end(boolean interrupted) {
        drive.stop();
    }

    private static DoubleSupplier requireScaleSupplier(DoubleSupplier supplier) {
        if (supplier == null) {
            throw new IllegalArgumentException("output scale supplier cannot be null");
        }
        return supplier;
    }

    private static DoubleSupplier requireHeadingSupplier(DoubleSupplier supplier) {
        if (supplier == null) {
            throw new IllegalArgumentException("heading supplier cannot be null");
        }
        return supplier;
    }

    private double applyDeadband(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("drive input must be finite");
        }
        if (Math.abs(value) <= deadband) {
            return 0.0;
        }
        return Math.copySign((Math.abs(value) - deadband) / (1.0 - deadband), value);
    }
}
