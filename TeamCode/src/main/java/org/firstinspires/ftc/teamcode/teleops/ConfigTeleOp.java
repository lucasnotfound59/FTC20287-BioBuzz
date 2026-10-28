package org.firstinspires.ftc.teamcode.teleops;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.commands.DriveCommand;
import org.firstinspires.ftc.teamcode.subsystems.Drive;
import org.firstinspires.ftc.teamcode.subsystems.GamepadConfig;
import org.firstinspires.ftc.teamcode.subsystems.Intake;
import org.firstinspires.ftc.teamcode.subsystems.Shooter;

/** Robot-centric TeleOp for tuning mechanism commands live in FTC Dashboard. */
@Config
@TeleOp(name = "Config TeleOp")
public final class ConfigTeleOp extends BaseDriveTeleOp {
    // Encoder ticks per second.
    public static double SHOOT_VEL = Shooter.SHOOT_VEL;
    // Limited to [-1, 1] each active loop, including Dashboard edits.
    public static double INTAKE_POWER = Intake.INTAKE_POWER;

    @Override
    protected void configureTelemetry() {
        telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());
    }

    @Override
    protected DriveCommand createDriveCommand(Drive drive, GamepadConfig controls) {
        return new DriveCommand(
                drive,
                controls::getForward,
                controls::getStrafe,
                controls::getTurn,
                GamepadConfig.DRIVE_DEADBAND,
                GamepadConfig.TURN_LIMIT);
    }

    @Override
    protected double getShooterVelocity() {
        return SHOOT_VEL;
    }

    @Override
    protected double getIntakePower() {
        return INTAKE_POWER;
    }

    @Override
    protected void beforeDriveRun() {
        if (gamepad1.dpadUpWasReleased()) {
            SHOOT_VEL += 10;
        } else if (gamepad1.dpadDownWasReleased()) {
            SHOOT_VEL -= 10;
        } else if (gamepad1.dpadRightWasReleased()) {
            INTAKE_POWER += 0.05;
        } else if (gamepad1.dpadLeftWasReleased()) {
            INTAKE_POWER -= 0.05;
        }
        INTAKE_POWER = Math.max(-1.0, Math.min(1.0, INTAKE_POWER));

    }

    @Override
    protected void updateDriveInitialization() {
        telemetry.addLine("Edit ConfigTeleOp in Dashboard and save changes.");
        telemetry.addLine("After START: hold LB for intake, RB for shooter; release to stop.");
        addTuningTelemetry();
    }

    @Override
    protected void addDriveTelemetry() {
        telemetry.addData("Drive", "Robot reference");
        addTuningTelemetry();
    }

    private void addTuningTelemetry() {
        telemetry.addData("Shooter velocity setting (ticks/s)", SHOOT_VEL);
        telemetry.addData("Intake power setting", INTAKE_POWER);
    }
}
