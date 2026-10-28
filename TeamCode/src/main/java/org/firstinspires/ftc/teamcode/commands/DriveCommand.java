package org.firstinspires.ftc.teamcode.commands;

import com.seattlesolvers.solverslib.command.CommandBase;

import org.firstinspires.ftc.teamcode.subsystems.Drive;
import org.firstinspires.ftc.teamcode.subsystems.Localizer;
import org.jetbrains.annotations.NotNull;

import java.util.function.DoubleSupplier;

/** Continuously sends live driver inputs to the drivetrain. */
public final class DriveCommand extends CommandBase {
    private final Drive drive;
    private final DoubleSupplier forward;
    private final DoubleSupplier strafe;
    private final DoubleSupplier turn;
    private final DoubleSupplier heading;
    private final double deadband;
    private final double turnLimit;

    /** Robot-centric drive; no heading sensor is required. */
    public DriveCommand(@NotNull final Drive drive, DoubleSupplier forward,
                        DoubleSupplier strafe, DoubleSupplier turn,
                        double deadband, double turnLimit) {
        this(drive, forward, strafe, turn, () -> 0.0, deadband, turnLimit);
    }

    public DriveCommand(@NotNull final Drive drive, DoubleSupplier forward,
                        DoubleSupplier strafe, DoubleSupplier turn,
                        Localizer localizer, double deadband, double turnLimit) {
        this(drive, forward, strafe, turn, localizer::getHeadingRadians, deadband, turnLimit);
        addRequirements(localizer);
    }

    private DriveCommand(@NotNull final Drive drive, DoubleSupplier forward,
                         DoubleSupplier strafe, DoubleSupplier turn,
                         DoubleSupplier heading, double deadband, double turnLimit) {
        this.drive = drive;
        this.forward = forward;
        this.strafe = strafe;
        this.turn = turn;
        this.heading = heading;
        this.deadband = deadband;
        this.turnLimit = turnLimit;
        addRequirements(drive);
    }

    @Override
    public void execute() {
        double headingRadians = heading.getAsDouble();
        if (!Double.isFinite(headingRadians)) {
            drive.stop();
            return;
        }

        double forwardValue = applyDeadband(forward.getAsDouble());
        double strafeValue = applyDeadband(strafe.getAsDouble());
        double turnValue = applyDeadband(turn.getAsDouble()) * turnLimit;
        double cosine = Math.cos(headingRadians);
        double sine = Math.sin(headingRadians);
        double robotForward = forwardValue * cosine - strafeValue * sine;
        double robotStrafe = forwardValue * sine + strafeValue * cosine;
        drive.drive(robotForward, robotStrafe, turnValue);
    }

    @Override
    public void end(boolean interrupted) {
        drive.stop();
    }

    private double applyDeadband(double input) {
        if (Math.abs(input) <= deadband) {
            return 0.0;
        }
        return Math.copySign((Math.abs(input) - deadband) / (1.0 - deadband), input);
    }
}
