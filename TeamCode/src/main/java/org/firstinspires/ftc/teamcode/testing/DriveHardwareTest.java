package org.firstinspires.ftc.teamcode.testing;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.firstinspires.ftc.teamcode.drive.DriveSubsystem;
import org.firstinspires.ftc.teamcode.drive.WheelPowers;
import org.firstinspires.ftc.teamcode.robot.RobotConfig;
import org.firstinspires.ftc.teamcode.robot.RobotHardware;

@TeleOp(name = "TEST - Drive Motors", group = "Testing")
public final class DriveHardwareTest extends OpMode {
    private static final double TEST_POWER = 0.15;
    private DriveSubsystem drive;
    private String error;

    @Override
    public void init() {
        try {
            drive = RobotHardware.createDrive(hardwareMap);
        } catch (RuntimeException failure) {
            error = failure.getMessage();
        }
        showInstructions("STOPPED");
    }

    @Override
    public void loop() {
        if (drive == null) {
            showInstructions("ERROR: " + error);
            return;
        }

        int pressed = (gamepad1.y ? 1 : 0)
                + (gamepad1.b ? 1 : 0)
                + (gamepad1.x ? 1 : 0)
                + (gamepad1.a ? 1 : 0);
        if (pressed != 1) {
            safeStop();
            showInstructions(pressed == 0 ? "STOPPED" : "STOPPED: hold one button only");
            return;
        }

        WheelPowers powers;
        String selected;
        if (gamepad1.y) {
            powers = new WheelPowers(TEST_POWER, 0, 0, 0);
            selected = "front-left";
        } else if (gamepad1.b) {
            powers = new WheelPowers(0, TEST_POWER, 0, 0);
            selected = "front-right";
        } else if (gamepad1.x) {
            powers = new WheelPowers(0, 0, TEST_POWER, 0);
            selected = "back-left";
        } else {
            powers = new WheelPowers(0, 0, 0, TEST_POWER);
            selected = "back-right";
        }

        try {
            drive.drive(powers);
            showInstructions("RUNNING: " + selected);
        } catch (RuntimeException failure) {
            error = failure.getMessage();
            safeStop();
            showInstructions("ERROR: " + error);
        }
    }

    private void safeStop() {
        if (drive == null) {
            return;
        }
        try {
            drive.stop();
        } catch (RuntimeException failure) {
            error = failure.getMessage();
        }
    }

    private void showInstructions(String state) {
        telemetry.clearAll();
        telemetry.addLine("LIFT THE ROBOT: all wheels must be clear.");
        telemetry.addData("State", state);
        telemetry.addData("Power", TEST_POWER);
        telemetry.addLine("Hold exactly one button:");
        telemetry.addLine("Y front-left | B front-right");
        telemetry.addLine("X back-left  | A back-right");
        telemetry.addData("front-left name", RobotConfig.FRONT_LEFT_DRIVE);
        telemetry.addData("front-right name", RobotConfig.FRONT_RIGHT_DRIVE);
        telemetry.addData("back-left name", RobotConfig.BACK_LEFT_DRIVE);
        telemetry.addData("back-right name", RobotConfig.BACK_RIGHT_DRIVE);
        if (error != null) {
            telemetry.addData("Last error", error);
        }
        telemetry.update();
    }

    @Override
    public void stop() {
        safeStop();
    }
}
