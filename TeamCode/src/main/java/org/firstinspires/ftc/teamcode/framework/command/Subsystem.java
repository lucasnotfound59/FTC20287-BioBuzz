package org.firstinspires.ftc.teamcode.framework.command;

/** A robot capability with periodic work and an explicit safe-stop operation. */
public interface Subsystem {
    default void periodic() {
    }

    void stop();
}
