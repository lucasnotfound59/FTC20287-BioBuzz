package org.firstinspires.ftc.teamcode.opmodes;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.firstinspires.ftc.teamcode.drive.DriveSubsystem;
import org.firstinspires.ftc.teamcode.drive.OctoQuadHeadingSubsystem;
import org.firstinspires.ftc.teamcode.drive.TeleopDriveCommand;
import org.firstinspires.ftc.teamcode.framework.command.CommandOpMode;
import org.firstinspires.ftc.teamcode.framework.command.CommandScheduler;
import org.firstinspires.ftc.teamcode.robot.RobotConfig;
import org.firstinspires.ftc.teamcode.robot.RobotHardware;

/** Field-centric mecanum drive using the OctoQuad MK2 localizer's IMU heading. */
@TeleOp(name="20287 Headless TeleOp", group="BIOBUZZ")
public final class HeadlessTeleOp extends CommandOpMode {
    private OctoQuadHeadingSubsystem heading;
    private boolean previousY;

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
                () -> gamepad1.left_bumper
                        ? Math.min(RobotConfig.SLOW_DRIVE_POWER_SCALE, RobotConfig.DRIVE_POWER_SCALE)
                        : RobotConfig.DRIVE_POWER_SCALE,
                heading::getHeadingRadians));

        while (opModeInInit() && !isStopRequested()) {
            heading.periodic();
            telemetry.addData("OctoQuad", heading.getStatus());
            telemetry.addLine("Keep robot still for calibration; point forward before START.");
            telemetry.addLine("Left stick: field translation | Right X: turn | LB: slow | Y: zero");
            telemetry.update();
            sleep(50);
        }
        previousY=gamepad1.y;
    }

    @Override
    protected void onLoop() {
        boolean currentY=gamepad1.y;
        if (currentY && !previousY) {
            heading.zeroHeading();
        }
        previousY=currentY;

        telemetry.addData("OctoQuad", heading.getStatus());
        telemetry.addData("Drive", !heading.isValid() ? "STOPPED: heading unavailable"
                : gamepad1.left_bumper ? "Field / Slow" : "Field / Normal");
        telemetry.addData("Heading deg", Math.toDegrees(heading.getHeadingRadians()));
        telemetry.addData("CRC errors", heading.getBadCrcPackets());
        telemetry.addLine("Y: current front becomes field forward | LB: slow");
        telemetry.update();
    }
}
