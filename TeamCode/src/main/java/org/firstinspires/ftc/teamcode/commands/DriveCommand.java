package org.firstinspires.ftc.teamcode.commands;

import com.seattlesolvers.solverslib.command.CommandBase;
import java.util.function.DoubleSupplier;
import org.firstinspires.ftc.teamcode.subsystems.Drive;
import org.firstinspires.ftc.teamcode.subsystems.OctoQuadSubsystem;

/** Continuously sends live driver inputs to the drivetrain. */
public final class DriveCommand extends CommandBase {
    private final Drive drive;
    private final DoubleSupplier forward;
    private final DoubleSupplier strafe;
    private final DoubleSupplier turn;
    private final OctoQuadSubsystem heading;
    private final double deadband;
    private final DoubleSupplier outputLimit;

    public DriveCommand(Drive drive, DoubleSupplier forward,
                        DoubleSupplier strafe, DoubleSupplier turn,
                        OctoQuadSubsystem heading, double deadband) {
        this(drive, forward, strafe, turn, heading, deadband, () -> 1.0);
    }

    public DriveCommand(Drive drive, DoubleSupplier forward,
                        DoubleSupplier strafe, DoubleSupplier turn,
                        OctoQuadSubsystem heading, double deadband, DoubleSupplier outputLimit) {
        if (drive == null || forward == null || strafe == null || turn == null
                || heading == null || outputLimit == null) {
            throw new IllegalArgumentException("drive and input suppliers cannot be null");
        }
        if (!Double.isFinite(deadband) || deadband < 0.0 || deadband >= 1.0) {
            throw new IllegalArgumentException("deadband must be in [0, 1)");
        }
        this.drive = drive;
        this.forward = forward;
        this.strafe = strafe;
        this.turn = turn;
        this.heading = heading;
        this.deadband = deadband;
        this.outputLimit=outputLimit;
        addRequirements(drive, heading);
    }

    @Override
    public void execute() {
        try {
            double headingRadians=heading.getHeadingRadians();
            if (!Double.isFinite(headingRadians)) {
                drive.stop();
                return;
            }

            double fieldForward=applyDeadband(forward.getAsDouble());
            double fieldStrafe=applyDeadband(strafe.getAsDouble());
            double turnValue=applyDeadband(turn.getAsDouble());
            double cosine=Math.cos(headingRadians);
            double sine=Math.sin(headingRadians);
            double robotForward=fieldForward*cosine-fieldStrafe*sine;
            double robotStrafe=fieldForward*sine+fieldStrafe*cosine;
            drive.drive(robotForward, robotStrafe, turnValue, outputLimit.getAsDouble());
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

    private double applyDeadband(double input) {
        if (!Double.isFinite(input)) {
            throw new IllegalArgumentException("driver input must be finite");
        }
        if (Math.abs(input) <= deadband) {
            return 0.0;
        }
        return Math.copySign((Math.abs(input) - deadband) / (1.0 - deadband), input);
    }
}
