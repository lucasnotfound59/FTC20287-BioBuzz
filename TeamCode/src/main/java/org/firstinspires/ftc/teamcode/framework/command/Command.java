package org.firstinspires.ftc.teamcode.framework.command;

import java.util.Set;

/** A schedulable robot action with an explicit lifecycle and subsystem requirements. */
public interface Command {
    default void initialize() {
    }

    void execute();

    default void end(boolean interrupted) {
    }

    default boolean isFinished() {
        return false;
    }

    Set<Subsystem> getRequirements();
}
