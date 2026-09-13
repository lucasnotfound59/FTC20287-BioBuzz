package org.firstinspires.ftc.teamcode.framework.command;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** Base class that owns a command's immutable public requirement set. */
public abstract class CommandBase implements Command {
    private final Set<Subsystem> requirements = new LinkedHashSet<>();

    protected final void addRequirements(Subsystem... subsystems) {
        if (subsystems == null) {
            throw new IllegalArgumentException("subsystems cannot be null");
        }
        for (Subsystem subsystem : subsystems) {
            if (subsystem == null) {
                throw new IllegalArgumentException("subsystem cannot be null");
            }
            requirements.add(subsystem);
        }
    }

    @Override
    public final Set<Subsystem> getRequirements() {
        return Collections.unmodifiableSet(requirements);
    }
}
