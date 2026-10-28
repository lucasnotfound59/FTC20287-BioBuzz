package org.firstinspires.ftc.teamcode.commands;

import com.seattlesolvers.solverslib.command.CommandBase;
import java.util.function.BooleanSupplier;
import org.firstinspires.ftc.teamcode.subsystems.Intake;

/** Runs the intake only while its live driver input is held. */
public final class IntakeCommand extends CommandBase {
    private final Intake intake;
    private final BooleanSupplier held;

    public IntakeCommand(Intake intake, BooleanSupplier held) {
        if (intake == null || held == null) {
            throw new IllegalArgumentException("intake and button supplier cannot be null");
        }
        this.intake=intake;
        this.held=held;
        addRequirements(intake);
    }

    @Override
    public void execute() {
        try {
            if (held.getAsBoolean()) {
                intake.intake();
            } else {
                intake.stop();
            }
        } catch (RuntimeException | Error failure) {
            try {
                intake.stop();
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
        intake.stop();
    }
}
