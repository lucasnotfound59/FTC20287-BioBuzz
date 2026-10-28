package org.firstinspires.ftc.teamcode.teleops;

import com.seattlesolvers.solverslib.command.CommandOpMode;
import com.seattlesolvers.solverslib.command.CommandScheduler;

import org.firstinspires.ftc.teamcode.commands.DriveCommand;
import org.firstinspires.ftc.teamcode.subsystems.Drive;
import org.firstinspires.ftc.teamcode.subsystems.GamepadConfig;
import org.firstinspires.ftc.teamcode.subsystems.Intake;
import org.firstinspires.ftc.teamcode.subsystems.Shooter;

/** Shared hardware, controls, and lifecycle for drive TeleOps. */
public abstract class BaseDriveTeleOp extends CommandOpMode {

    private Drive drive;
    private Shooter shooter;
    private Intake intake;
    private GamepadConfig controls;

    @Override
    public final void initialize() {
        configureTelemetry();
        drive = new Drive(hardwareMap);
        shooter = new Shooter(hardwareMap);
        intake = new Intake(hardwareMap);
        controls = new GamepadConfig(gamepad1);
        drive.setDefaultCommand(createDriveCommand(drive, controls));
    }

    protected abstract DriveCommand createDriveCommand(Drive drive, GamepadConfig controls);

    protected void configureTelemetry() {
    }

    protected double getShooterVelocity() {
        return Shooter.SHOOT_VEL;
    }

    protected double getIntakePower() {
        return Intake.INTAKE_POWER;
    }

    @Override
    public final void initialize_loop() {
        updateDriveInitialization();
        telemetry.update();
        sleep(50);
    }

    protected void updateDriveInitialization() {
        telemetry.addLine("Ready for START.");
    }

    @Override
    public final void run() {
        if (isStopRequested() || Thread.currentThread().isInterrupted()) {
            end();
            return;
        }
        beforeDriveRun();
        if (isStopRequested() || Thread.currentThread().isInterrupted()) {
            end();
            return;
        }
        super.run();
        if (controls.isShootHeld()) {
            shooter.setVelocity(getShooterVelocity());
        } else {
            shooter.stop();
        }
        if (controls.isIntakeHeld()) {
            intake.setPower(getIntakePower());
        } else {
            intake.stop();
        }

        addDriveTelemetry();
        telemetry.addData("Shooter velocity command", shooter.getCommandedVel());
        telemetry.addData("Intake power command", intake.getCommandedPower());
        telemetry.update();
        idle();
    }

    protected void beforeDriveRun() {
    }

    protected abstract void addDriveTelemetry();

    @Override
    public final void end() {
        CommandScheduler.getInstance().cancelAll();
        drive.stop();
        shooter.stop();
        intake.stop();
    }
}
