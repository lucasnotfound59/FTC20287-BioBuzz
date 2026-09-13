package org.firstinspires.ftc.teamcode.opmodes;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.firstinspires.ftc.teamcode.drive.DriveSubsystem;
import org.firstinspires.ftc.teamcode.drive.TeleopDriveCommand;
import org.firstinspires.ftc.teamcode.framework.command.CommandScheduler;
import org.firstinspires.ftc.teamcode.robot.RobotConfig;
import org.firstinspires.ftc.teamcode.robot.RobotHardware;

/** Main robot-centric mecanum TeleOp for the BIOBUZZ season. */
@TeleOp(name = "20287 Main TeleOp", group = "BIOBUZZ")
public final class MainTeleOp extends LinearOpMode {
    @Override
    public void runOpMode() throws InterruptedException {
        CommandScheduler scheduler = new CommandScheduler();
        try {
            DriveSubsystem drive = RobotHardware.createDrive(hardwareMap);
            scheduler.registerSubsystem(drive);
            scheduler.setDefaultCommand(
                    drive,
                    new TeleopDriveCommand(
                            drive,
                            () -> -gamepad1.left_stick_y,
                            () -> gamepad1.left_stick_x,
                            () -> gamepad1.right_stick_x,
                            RobotConfig.DRIVE_DEADBAND,
                            RobotConfig.DRIVE_POWER_SCALE));

            telemetry.addLine("Framework ready; drivetrain configuration is not robot-verified.");
            telemetry.addLine("Lift the robot before the first direction test.");
            telemetry.update();
            waitForStart();
            if (isStopRequested()) {
                return;
            }

            while (opModeIsActive() && !isStopRequested()) {
                scheduler.run();
                telemetry.addData("Status", "Running");
                telemetry.update();
                idle();
            }
        } finally {
            scheduler.shutdown();
        }
    }
}
