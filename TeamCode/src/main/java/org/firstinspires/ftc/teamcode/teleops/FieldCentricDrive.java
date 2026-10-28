package org.firstinspires.ftc.teamcode.teleops;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.seattlesolvers.solverslib.command.CommandOpMode;
import com.seattlesolvers.solverslib.command.CommandScheduler;

import org.firstinspires.ftc.teamcode.commands.DriveCommand;
import org.firstinspires.ftc.teamcode.commands.IntakeCommand;
import org.firstinspires.ftc.teamcode.commands.ShootCommand;
import org.firstinspires.ftc.teamcode.subsystems.Drive;
import org.firstinspires.ftc.teamcode.subsystems.OctoQuadSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.Intake;
import org.firstinspires.ftc.teamcode.subsystems.Shooter;

/** Field-centric mecanum drive with its forward direction locked when START is pressed. */
@TeleOp(name="20287 Headless TeleOp", group="BIOBUZZ")
public final class FieldCentricDrive extends CommandOpMode {
    private static final double DRIVE_DEADBAND = 0.05;
    private static final double NORMAL_POWER=1.0;
    private static final double SLOW_POWER=0.30;

    private Drive drive;
    private OctoQuadSubsystem heading;
    private Shooter shooter;
    private Intake intake;
    private boolean startHeadingCaptured;
    private Throwable failure;

    @Override
    public void initialize() {
        startHeadingCaptured=false;
        failure=null;
        try {
            drive=new Drive(hardwareMap);
            heading=new OctoQuadSubsystem(hardwareMap);
            shooter=new Shooter(hardwareMap);
            intake=new Intake(hardwareMap);
            DriveCommand driveCommand=new DriveCommand(
                    drive,
                    () -> -gamepad1.left_stick_y,
                    () -> gamepad1.left_stick_x,
                    () -> gamepad1.right_stick_x,
                    heading,
                    DRIVE_DEADBAND,
                    () -> {
                        if (gamepad1.left_bumper) {
                            return Math.min(SLOW_POWER, NORMAL_POWER);
                        } else {
                            return NORMAL_POWER;
                        }
                    });
            drive.setDefaultCommand(driveCommand);
            shooter.setDefaultCommand(new ShootCommand(shooter, () -> gamepad1.b));
            intake.setDefaultCommand(new IntakeCommand(intake, () -> gamepad1.a));
        } catch (RuntimeException | Error originalFailure) {
            failure=originalFailure;
            // SolversLib 0.3.4 calls initialize() outside its try/finally block.
            try {
                end();
            } finally {
                reset();
            }
            throw originalFailure;
        }
    }

    @Override
    public void initialize_loop() {
        try {
            heading.periodic();
            telemetry.addData("OctoQuad", heading.getStatus());
            telemetry.addLine("Keep still; wait for RUNNING before START.");
            telemetry.addLine("START locks this facing as forward for the entire run.");
            telemetry.addLine("Left stick: translate | Right X: turn | Hold LB: slow");
            telemetry.addLine("Hold A: intake | Hold B: shooter | Release: stop");
            telemetry.update();
            sleep(50);
        } catch (RuntimeException | Error originalFailure) {
            failure=originalFailure;
            throw originalFailure;
        }
    }

    @Override
    public void run() {
        try {
            if (isStopRequested() || Thread.currentThread().isInterrupted()) {
                end();
                return;
            }
            // 0.3.4 has no preRun hook: capture before the first active scheduler run.
            if (!startHeadingCaptured) {
                heading.periodic();
                heading.captureStartHeading();
                startHeadingCaptured=true;
            }
            if (isStopRequested() || Thread.currentThread().isInterrupted()) {
                end();
                return;
            }
            super.run();
            telemetry.addData("OctoQuad", heading.getStatus());
            telemetry.addData("Heading valid", heading.isValid());
            telemetry.addData("Heading from START (deg)", Math.toDegrees(heading.getHeadingRadians()));
            telemetry.addData("Bad CRC packets", heading.getBadCrcPackets());
            telemetry.addData("Shooter power command", shooter.getCommandedPower());
            telemetry.addData("Intake power command", intake.getCommandedPower());
            if (!heading.isValid()) {
                telemetry.addData("Drive", "STOPPED: heading unavailable");
            } else if (gamepad1.left_bumper) {
                telemetry.addData("Drive", "START reference / Slow");
            } else {
                telemetry.addData("Drive", "START reference / Normal");
            }
            telemetry.update();
            idle();
        } catch (RuntimeException | Error originalFailure) {
            failure=originalFailure;
            throw originalFailure;
        }
    }

    @Override
    public void end() {
        Throwable cleanupFailure=null;
        try {
            CommandScheduler.getInstance().cancelAll();
        } catch (RuntimeException | Error cancelFailure) {
            cleanupFailure=cancelFailure;
        }
        // Cancellation can fail early; explicitly attempt every independent subsystem stop.
        if (drive != null) {
            cleanupFailure=stopSafely(drive::stop, cleanupFailure);
        }
        if (shooter != null) {
            cleanupFailure=stopSafely(shooter::stop, cleanupFailure);
        }
        if (intake != null) {
            cleanupFailure=stopSafely(intake::stop, cleanupFailure);
        }
        if (cleanupFailure != null) {
            if (failure != null) {
                if (failure != cleanupFailure) {
                    failure.addSuppressed(cleanupFailure);
                }
            } else if (cleanupFailure instanceof RuntimeException) {
                throw (RuntimeException) cleanupFailure;
            } else {
                throw (Error) cleanupFailure;
            }
        }
    }

    private static Throwable stopSafely(Runnable stop, Throwable failure) {
        try {
            stop.run();
        } catch (RuntimeException | Error stopFailure) {
            if (failure == null) {
                return stopFailure;
            }
            if (failure != stopFailure) {
                failure.addSuppressed(stopFailure);
            }
        }
        return failure;
    }
}
