package org.firstinspires.ftc.teamcode.framework.command;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Instance-owned command scheduler.
 *
 * <p>Each OpMode creates one scheduler so no command state leaks into the next OpMode.</p>
 */
public final class CommandScheduler {
    private final Set<Subsystem> subsystems = new LinkedHashSet<>();
    private final Set<Command> scheduled = new LinkedHashSet<>();
    private final Map<Subsystem, Command> owners = new LinkedHashMap<>();
    private final Map<Subsystem, Command> defaults = new LinkedHashMap<>();
    private boolean shutdown;

    public void registerSubsystem(Subsystem subsystem) {
        requireNonNull(subsystem, "subsystem");
        ensureActive();
        subsystems.add(subsystem);
    }

    public void setDefaultCommand(Subsystem subsystem, Command command) {
        requireNonNull(subsystem, "subsystem");
        requireNonNull(command, "command");
        ensureActive();
        if (!command.getRequirements().contains(subsystem)) {
            throw new IllegalArgumentException("default command must require its subsystem");
        }
        subsystems.add(subsystem);
        defaults.put(subsystem, command);
    }

    public void schedule(Command command) {
        requireNonNull(command, "command");
        ensureActive();
        if (scheduled.contains(command)) {
            return;
        }

        for (Subsystem requirement : command.getRequirements()) {
            requireNonNull(requirement, "command requirement");
            Command owner = owners.get(requirement);
            if (owner != null && owner != command) {
                cancel(owner);
            }
        }

        for (Subsystem requirement : command.getRequirements()) {
            owners.put(requirement, command);
        }
        scheduled.add(command);
        command.initialize();
    }

    public void run() {
        ensureActive();

        for (Subsystem subsystem : new ArrayList<>(subsystems)) {
            subsystem.periodic();
        }

        for (Map.Entry<Subsystem, Command> entry : new ArrayList<>(defaults.entrySet())) {
            if (!owners.containsKey(entry.getKey()) && !scheduled.contains(entry.getValue())) {
                schedule(entry.getValue());
            }
        }

        for (Command command : new ArrayList<>(scheduled)) {
            command.execute();
            if (command.isFinished()) {
                finish(command, false);
            }
        }
    }

    public void cancel(Command command) {
        if (command != null && scheduled.contains(command)) {
            finish(command, true);
        }
    }

    public void cancelAll() {
        for (Command command : new ArrayList<>(scheduled)) {
            finish(command, true);
        }
    }

    public boolean isScheduled(Command command) {
        return scheduled.contains(command);
    }

    public void shutdown() {
        if (shutdown) {
            return;
        }
        cancelAll();
        for (Subsystem subsystem : new ArrayList<>(subsystems)) {
            subsystem.stop();
        }
        defaults.clear();
        owners.clear();
        subsystems.clear();
        shutdown = true;
    }

    private void finish(Command command, boolean interrupted) {
        scheduled.remove(command);
        owners.entrySet().removeIf(entry -> entry.getValue() == command);
        command.end(interrupted);
    }

    private void ensureActive() {
        if (shutdown) {
            throw new IllegalStateException("scheduler is shut down");
        }
    }

    private static void requireNonNull(Object value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " cannot be null");
        }
    }
}
