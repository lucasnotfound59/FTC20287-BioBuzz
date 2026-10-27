package org.firstinspires.ftc.teamcode.framework.command;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

/** FTCLib-inspired lifecycle with an instance-owned scheduler and guaranteed cleanup. */
public abstract class CommandOpMode extends LinearOpMode {
    /** Assemble hardware and bind commands; do not start motion during INIT. */
    protected abstract void initialize(CommandScheduler scheduler) throws InterruptedException;

    /** Optional OpMode work after the scheduler, such as telemetry. */
    protected void onLoop() {
    }

    @Override
    public final void runOpMode() throws InterruptedException {
        CommandScheduler scheduler=new CommandScheduler();
        Throwable failure=null;
        try {
            initialize(scheduler);
            waitForStart();
            if (isStopRequested() || Thread.currentThread().isInterrupted()) {
                return;
            }
            while (opModeIsActive() && !isStopRequested()
                    && !Thread.currentThread().isInterrupted()) {
                scheduler.run();
                onLoop();
                idle();
            }
        } catch (InterruptedException | RuntimeException | Error originalFailure) {
            failure=originalFailure;
            throw originalFailure;
        } finally {
            try {
                scheduler.shutdown();
            } catch (RuntimeException | Error cleanupFailure) {
                if (failure == null) {
                    throw cleanupFailure;
                }
                if (cleanupFailure != failure) {
                    failure.addSuppressed(cleanupFailure);
                }
            }
        }
    }
}
