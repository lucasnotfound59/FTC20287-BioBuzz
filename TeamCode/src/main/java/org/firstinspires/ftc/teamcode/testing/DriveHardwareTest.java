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
        drive = null;
        error = null;
        try {
            drive = RobotHardware.createDrive(hardwareMap);
        } catch (RuntimeException failure) {
            latchFault("INIT", failure);
        }
        showInstructions("STOPPED");
    }

    @Override
    public void loop() {
        if (error != null || drive == null) {
            safeStop();
            showInstructions("FAULT: drive unavailable; re-INIT required");
            return;
        }

        int pressed = (gamepad1.y ? 1 : 0)
                + (gamepad1.b ? 1 : 0)
                + (gamepad1.x ? 1 : 0)
                + (gamepad1.a ? 1 : 0);
        if (pressed != 1) {
            boolean stopped = safeStop();
            showInstructions(stopped
                    ? (pressed == 0 ? "STOPPED" : "STOPPED: hold one button only")
                    : "FAULT: stop failed; re-INIT required");
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
            latchFault("DRIVE", failure);
            safeStop();
            showInstructions("FAULT: drive failed; re-INIT required");
        }
    }

    private boolean safeStop() {
        if (drive == null) {
            return false;
        }
        try {
            // DriveSubsystem.stop attempts every motor even when one write fails.
            drive.stop();
            return true;
        } catch (RuntimeException failure) {
            latchFault("STOP", failure);
            return false;
        }
    }

    private void latchFault(String operation, RuntimeException failure) {
        if (error == null) {
            String message = failure.getMessage();
            error = operation + ": " + failure.getClass().getName()
                    + (message == null || message.trim().isEmpty() ? "" : " - " + message);
        }
    }

    private void showInstructions(String state) {
        telemetry.clearAll();
        telemetry.addLine("LIFT THE ROBOT: all wheels must be clear.");
        telemetry.addData("State", error == null ? state : "FAULT: " + error);
        telemetry.addData("Power", TEST_POWER);
        telemetry.addLine("Hold exactly one button:");
        telemetry.addLine("Y front-left | B front-right");
        telemetry.addLine("X back-left  | A back-right");
        telemetry.addData("front-left name", RobotConfig.FRONT_LEFT_DRIVE);
        telemetry.addData("front-right name", RobotConfig.FRONT_RIGHT_DRIVE);
        telemetry.addData("back-left name", RobotConfig.BACK_LEFT_DRIVE);
        telemetry.addData("back-right name", RobotConfig.BACK_RIGHT_DRIVE);
        if (error != null) {
            telemetry.addLine("Fault latched: STOP and re-INIT before retrying.");
        }
        telemetry.update();
    }

    @Override
    public void stop() {
        boolean stopped = safeStop();
        showInstructions(stopped ? "STOPPED" : "FAULT: drive unavailable or stop failed");
    }
}
