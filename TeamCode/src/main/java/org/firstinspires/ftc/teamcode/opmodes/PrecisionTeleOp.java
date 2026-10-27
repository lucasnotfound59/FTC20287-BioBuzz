package org.firstinspires.ftc.teamcode.opmodes;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.firstinspires.ftc.teamcode.drive.DriveSubsystem;
import org.firstinspires.ftc.teamcode.drive.TeleopDriveCommand;
import org.firstinspires.ftc.teamcode.framework.command.CommandOpMode;
import org.firstinspires.ftc.teamcode.framework.command.CommandScheduler;
import org.firstinspires.ftc.teamcode.robot.RobotConfig;
import org.firstinspires.ftc.teamcode.robot.RobotHardware;

/** Robot-centric drive with a hold-to-slow wheel-output limit. */
@TeleOp(name="20287 Precision TeleOp", group="BIOBUZZ")
public final class PrecisionTeleOp extends CommandOpMode {
    @Override
    protected void initialize(CommandScheduler scheduler) {
        DriveSubsystem drive=RobotHardware.createDrive(hardwareMap);
        scheduler.registerSubsystem(drive);
        scheduler.setDefaultCommand(drive, new TeleopDriveCommand(
                drive,
                () -> -gamepad1.left_stick_y,
                () -> gamepad1.left_stick_x,
                () -> gamepad1.right_stick_x,
                RobotConfig.DRIVE_DEADBAND,
                () -> gamepad1.left_bumper
                        ? Math.min(RobotConfig.SLOW_DRIVE_POWER_SCALE, RobotConfig.DRIVE_POWER_SCALE)
                        : RobotConfig.DRIVE_POWER_SCALE));

        telemetry.addLine("Left stick: translate; right stick X: turn; hold LB: slow.");
        telemetry.addLine("Hardware names/directions unverified; lift robot for first check.");
        telemetry.update();
    }

    @Override
    protected void onLoop() {
        telemetry.addData("Drive", gamepad1.left_bumper ? "Slow (LB held)" : "Normal");
        telemetry.update();
    }
}
