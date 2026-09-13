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

    public TeleopDriveCommand(
            DriveSubsystem drive,
            DoubleSupplier forward,
            DoubleSupplier strafe,
            DoubleSupplier turn,
            double deadband,
            double scale) {
        if (drive == null || forward == null || strafe == null || turn == null) {
            throw new IllegalArgumentException("drive and input suppliers cannot be null");
        }
        if (deadband < 0.0 || deadband >= 1.0) {
            throw new IllegalArgumentException("deadband must be in [0, 1)");
        }
        if (scale < 0.0 || scale > 1.0) {
            throw new IllegalArgumentException("scale must be in [0, 1]");
        }

        this.drive = drive;
        this.forward = forward;
        this.strafe = strafe;
        this.turn = turn;
        this.deadband = deadband;
        this.scale = scale;
        addRequirements(drive);
    }

    @Override
    public void execute() {
        double forwardValue = applyDeadband(forward.getAsDouble()) * scale;
        double strafeValue = applyDeadband(strafe.getAsDouble()) * scale;
        double turnValue = applyDeadband(turn.getAsDouble()) * scale;
        drive.drive(MecanumMixer.mix(forwardValue, strafeValue, turnValue));
    }

    @Override
    public void end(boolean interrupted) {
        drive.stop();
    }

    private double applyDeadband(double value) {
        if (Math.abs(value) <= deadband) {
            return 0.0;
        }
        return Math.copySign((Math.abs(value) - deadband) / (1.0 - deadband), value);
    }
}
