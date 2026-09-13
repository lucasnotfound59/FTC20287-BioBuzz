package org.firstinspires.ftc.teamcode.framework.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.Test;

public class CommandSchedulerTest {
    private static final class FakeSubsystem implements Subsystem {
        int periodicCalls;
        int stopCalls;
        boolean throwOnStop;

        @Override
        public void periodic() {
            periodicCalls++;
        }

        @Override
        public void stop() {
            stopCalls++;
            if (throwOnStop) {
                throw new IllegalStateException("stop failed");
            }
        }
    }

    private static final class FakeCommand implements Command {
        final Set<Subsystem> requirements;
        int initializeCalls;
        int executeCalls;
        int interruptedEnds;
        int normalEnds;
        boolean finished;
        boolean throwOnInitialize;
        boolean throwOnEnd;
        Runnable onExecute = () -> { };

        FakeCommand(Subsystem... requirements) {
            this.requirements = new LinkedHashSet<>(Arrays.asList(requirements));
        }

        @Override
        public void initialize() {
            initializeCalls++;
            if (throwOnInitialize) {
                throw new IllegalStateException("initialize failed");
            }
        }

        @Override
        public void execute() {
            executeCalls++;
            onExecute.run();
        }

        @Override
        public void end(boolean interrupted) {
            if (interrupted) {
                interruptedEnds++;
            } else {
                normalEnds++;
            }
            if (throwOnEnd) {
                throw new IllegalStateException("end failed");
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

    @Test
    public void commandCanceledDuringRunDoesNotExecuteLaterInSameCycle() {
        CommandScheduler scheduler = new CommandScheduler();
        FakeSubsystem firstSubsystem = new FakeSubsystem();
        FakeSubsystem secondSubsystem = new FakeSubsystem();
        FakeCommand cancelingCommand = new FakeCommand(firstSubsystem);
        FakeCommand canceledCommand = new FakeCommand(secondSubsystem);
        FakeCommand replacement = new FakeCommand(secondSubsystem);
        cancelingCommand.onExecute = () -> scheduler.schedule(replacement);

        scheduler.schedule(cancelingCommand);
        scheduler.schedule(canceledCommand);
        scheduler.run();

        assertEquals(1, canceledCommand.interruptedEnds);
        assertEquals(0, canceledCommand.executeCalls);
        assertTrue(scheduler.isScheduled(replacement));
    }

    @Test
    public void failedInitializationReleasesRequirements() {
        CommandScheduler scheduler = new CommandScheduler();
        FakeSubsystem subsystem = new FakeSubsystem();
        FakeCommand failedCommand = new FakeCommand(subsystem);
        FakeCommand replacement = new FakeCommand(subsystem);
        failedCommand.throwOnInitialize = true;

        try {
            scheduler.schedule(failedCommand);
            fail("initialization failure must be propagated");
        } catch (IllegalStateException expected) {
            assertEquals("initialize failed", expected.getMessage());
        }

        assertFalse(scheduler.isScheduled(failedCommand));
        assertEquals(1, failedCommand.interruptedEnds);
        scheduler.schedule(replacement);
        assertTrue(scheduler.isScheduled(replacement));
    }

    @Test
    public void defaultCommandWaitsUntilAllRequirementsAreFree() {
        CommandScheduler scheduler = new CommandScheduler();
        FakeSubsystem defaultSubsystem = new FakeSubsystem();
        FakeSubsystem sharedSubsystem = new FakeSubsystem();
        FakeCommand activeCommand = new FakeCommand(sharedSubsystem);
        FakeCommand defaultCommand = new FakeCommand(defaultSubsystem, sharedSubsystem);

        scheduler.registerSubsystem(defaultSubsystem);
        scheduler.registerSubsystem(sharedSubsystem);
        scheduler.setDefaultCommand(defaultSubsystem, defaultCommand);
        scheduler.schedule(activeCommand);
        scheduler.run();

        assertTrue(scheduler.isScheduled(activeCommand));
        assertFalse(scheduler.isScheduled(defaultCommand));
        assertEquals(1, activeCommand.executeCalls);
        assertEquals(0, activeCommand.interruptedEnds);
    }

    @Test
    public void shutdownAttemptsEveryCleanupBeforePropagatingFailure() {
        CommandScheduler scheduler = new CommandScheduler();
        FakeSubsystem firstSubsystem = new FakeSubsystem();
        FakeSubsystem secondSubsystem = new FakeSubsystem();
        FakeCommand firstCommand = new FakeCommand(firstSubsystem);
        FakeCommand secondCommand = new FakeCommand(secondSubsystem);
        firstCommand.throwOnEnd = true;
        firstSubsystem.throwOnStop = true;

        scheduler.registerSubsystem(firstSubsystem);
        scheduler.registerSubsystem(secondSubsystem);
        scheduler.schedule(firstCommand);
        scheduler.schedule(secondCommand);

        try {
            scheduler.shutdown();
            fail("cleanup failure must be propagated");
        } catch (IllegalStateException expected) {
            assertEquals("end failed", expected.getMessage());
            assertEquals(1, expected.getSuppressed().length);
            assertEquals("stop failed", expected.getSuppressed()[0].getMessage());
        }

        assertEquals(1, firstCommand.interruptedEnds);
        assertEquals(1, secondCommand.interruptedEnds);
        assertEquals(1, firstSubsystem.stopCalls);
        assertEquals(1, secondSubsystem.stopCalls);
        assertFalse(scheduler.isScheduled(firstCommand));
        assertFalse(scheduler.isScheduled(secondCommand));
        try {
            scheduler.run();
            fail("scheduler must remain shut down after cleanup failure");
        } catch (IllegalStateException expected) {
            assertEquals("scheduler is shut down", expected.getMessage());
        }
    }
}
