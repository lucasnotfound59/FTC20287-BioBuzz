package org.firstinspires.ftc.teamcode.opmodes;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.firstinspires.ftc.teamcode.drive.DriveSubsystem;
import org.firstinspires.ftc.teamcode.drive.OctoQuadHeadingSubsystem;
import org.firstinspires.ftc.teamcode.drive.TeleopDriveCommand;
import org.firstinspires.ftc.teamcode.framework.command.CommandOpMode;
import org.firstinspires.ftc.teamcode.framework.command.CommandScheduler;
import org.firstinspires.ftc.teamcode.robot.RobotConfig;
import org.firstinspires.ftc.teamcode.robot.RobotHardware;

/** Mecanum translation stays relative to the direction the robot faced at START. */
@TeleOp(name="20287 Headless TeleOp", group="BIOBUZZ")
public final class HeadlessTeleOp extends CommandOpMode {
    private OctoQuadHeadingSubsystem heading;

    @Override
    protected void initialize(CommandScheduler scheduler) throws InterruptedException {
        DriveSubsystem drive=RobotHardware.createDrive(hardwareMap);
        scheduler.registerSubsystem(drive);
        heading=RobotHardware.createOctoQuadHeading(hardwareMap);
        scheduler.registerSubsystem(heading);
        scheduler.setDefaultCommand(drive, new TeleopDriveCommand(
                drive,
                () -> -gamepad1.left_stick_y,
                () -> gamepad1.left_stick_x,
                () -> gamepad1.right_stick_x,
                RobotConfig.DRIVE_DEADBAND,
                () -> {
                    if (gamepad1.left_bumper) {
                        return Math.min(RobotConfig.SLOW_DRIVE_POWER_SCALE, RobotConfig.DRIVE_POWER_SCALE);
                    } else {
                        return RobotConfig.DRIVE_POWER_SCALE;
                    }
                },
                heading::getHeadingRadians));

        while (opModeInInit() && !isStopRequested()) {
            heading.periodic();
            telemetry.addData("OctoQuad", heading.getStatus());
            telemetry.addLine("Keep still; wait for RUNNING. Face your forward direction at START.");
            telemetry.addLine("START locks forward for the whole run | Right X: turn | LB: slow");
            telemetry.update();
            sleep(50);
        }
    }

    @Override
    protected void onStart() {
        heading.periodic();
        heading.captureStartHeading();
    }

    @Override
    protected void onLoop() {
        telemetry.addData("OctoQuad", heading.getStatus());
        if (!heading.isValid()) {
            telemetry.addData("Drive", "STOPPED: heading unavailable");
        } else if (gamepad1.left_bumper) {
            telemetry.addData("Drive", "START reference / Slow");
        } else {
            telemetry.addData("Drive", "START reference / Normal");
        }
        telemetry.addData("Rotation from START deg", Math.toDegrees(heading.getHeadingRadians()));
        telemetry.addData("CRC errors", heading.getBadCrcPackets());
        telemetry.addLine("Stick directions stay relative to START | LB: slow");
        telemetry.update();
    }
}
