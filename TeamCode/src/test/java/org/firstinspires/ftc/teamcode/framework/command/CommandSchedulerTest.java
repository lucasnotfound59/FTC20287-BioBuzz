package org.firstinspires.ftc.teamcode.framework.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.Set;
import org.junit.Test;

public class CommandSchedulerTest {
    private static final class FakeSubsystem implements Subsystem {
        int periodicCalls;
        int stopCalls;

        @Override
        public void periodic() {
            periodicCalls++;
        }

        @Override
        public void stop() {
            stopCalls++;
        }
    }

    private static final class FakeCommand implements Command {
        final Set<Subsystem> requirements;
        int initializeCalls;
        int executeCalls;
        int interruptedEnds;
        int normalEnds;
        boolean finished;

        FakeCommand(Subsystem requirement) {
            requirements = Collections.singleton(requirement);
        }

        @Override
        public void initialize() {
            initializeCalls++;
        }

        @Override
        public void execute() {
            executeCalls++;
        }

        @Override
        public void end(boolean interrupted) {
            if (interrupted) {
                interruptedEnds++;
            } else {
                normalEnds++;
            }
        }

        @Override
        public boolean isFinished() {
            return finished;
        }

        @Override
        public Set<Subsystem> getRequirements() {
            return requirements;
        }
    }

    @Test
    public void scheduleRunsLifecycleAndReleasesRequirement() {
        CommandScheduler scheduler = new CommandScheduler();
        FakeSubsystem subsystem = new FakeSubsystem();
        FakeCommand command = new FakeCommand(subsystem);

        scheduler.registerSubsystem(subsystem);
        scheduler.schedule(command);
        assertEquals(1, command.initializeCalls);

        scheduler.run();
        assertEquals(1, command.executeCalls);

        command.finished = true;
        scheduler.run();
        assertEquals(1, command.normalEnds);
        assertFalse(scheduler.isScheduled(command));
    }

    @Test
    public void requirementConflictInterruptsExistingCommand() {
        CommandScheduler scheduler = new CommandScheduler();
        FakeSubsystem subsystem = new FakeSubsystem();
        FakeCommand first = new FakeCommand(subsystem);
        FakeCommand replacement = new FakeCommand(subsystem);

        scheduler.schedule(first);
        scheduler.schedule(replacement);

        assertEquals(1, first.interruptedEnds);
        assertFalse(scheduler.isScheduled(first));
        assertTrue(scheduler.isScheduled(replacement));
    }

    @Test
    public void defaultCommandRunsWhenSubsystemIsFree() {
        CommandScheduler scheduler = new CommandScheduler();
        FakeSubsystem subsystem = new FakeSubsystem();
        FakeCommand defaultCommand = new FakeCommand(subsystem);

        scheduler.registerSubsystem(subsystem);
        scheduler.setDefaultCommand(subsystem, defaultCommand);
        scheduler.run();

        assertEquals(1, defaultCommand.initializeCalls);
        assertEquals(1, defaultCommand.executeCalls);
        assertEquals(1, subsystem.periodicCalls);
    }

    @Test
    public void shutdownInterruptsCommandsStopsSubsystemsAndClearsState() {
        CommandScheduler scheduler = new CommandScheduler();
        FakeSubsystem subsystem = new FakeSubsystem();
        FakeCommand command = new FakeCommand(subsystem);

        scheduler.registerSubsystem(subsystem);
        scheduler.schedule(command);
        scheduler.shutdown();

        assertEquals(1, command.interruptedEnds);
        assertEquals(1, subsystem.stopCalls);
        assertFalse(scheduler.isScheduled(command));
        try {
            scheduler.run();
            fail("run after shutdown must fail");
        } catch (IllegalStateException expected) {
            assertEquals("scheduler is shut down", expected.getMessage());
        }
        assertEquals(0, command.executeCalls);
    }
}
