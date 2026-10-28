package org.firstinspires.ftc.teamcode.commands;

import com.seattlesolvers.solverslib.command.CommandBase;
import java.util.function.BooleanSupplier;
import org.firstinspires.ftc.teamcode.subsystems.Shooter;

/** Runs the shooter only while its live driver input is held. */
public final class ShootCommand extends CommandBase {
    private final Shooter shooter;
    private final BooleanSupplier held;

    public ShootCommand(Shooter shooter, BooleanSupplier held) {
        if (shooter == null || held == null) {
            throw new IllegalArgumentException("shooter and button supplier cannot be null");
        }
        this.shooter=shooter;
        this.held=held;
        addRequirements(shooter);
    }

    @Override
    public void execute() {
        try {
            if (held.getAsBoolean()) {
                shooter.shoot();
            } else {
                shooter.stop();
            }
        } catch (RuntimeException | Error failure) {
            try {
                shooter.stop();
            } catch (RuntimeException | Error stopFailure) {
                if (failure != stopFailure) {
                    failure.addSuppressed(stopFailure);
                }
            }
            throw failure;
        }
    }

    @Override
    public void end(boolean interrupted) {
        shooter.stop();
    }
}
