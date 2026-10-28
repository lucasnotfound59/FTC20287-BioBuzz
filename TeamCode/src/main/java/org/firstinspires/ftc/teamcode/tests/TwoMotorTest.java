package org.firstinspires.ftc.teamcode.tests;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.Gamepad;

@Config
@TeleOp(name="Two Motor Test", group="Testing")
public class TwoMotorTest extends LinearOpMode {

    public static double targetVel = 1800;
    public static double targetPow = 1.0;
    public DcMotorEx intake, shooter;


    @Override
    public void runOpMode() {

        FtcDashboard dashboard = FtcDashboard.getInstance();
        MultipleTelemetry m_telemetry = new MultipleTelemetry(telemetry, dashboard.getTelemetry());

        intake = hardwareMap.get(DcMotorEx.class, "intake");
        shooter = hardwareMap.get(DcMotorEx.class, "shooter");
        intake.setDirection(DcMotorSimple.Direction.REVERSE);
        shooter.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        m_telemetry.addData("Status", "Initialized");
        m_telemetry.update();
        // Wait for the game to start (driver presses PLAY)
        waitForStart();

        // run until the end of the match (driver presses STOP)
        while (opModeIsActive()) {
            if (gamepad1.dpadUpWasReleased()) {
                targetVel += 10;
            } else if (gamepad1.dpadDownWasReleased()) {
                targetVel -= 10;
            } else if (gamepad1.dpadRightWasReleased()) {
                targetPow += 0.05;
            } else if (gamepad1.dpadLeftWasReleased()) {
                targetPow -= 0.05;
            }

            if (targetVel < 0) {
                targetVel = 0;
            }

            targetPow = Math.max(Math.min(1.0, targetPow), 0.0);

            intake.setPower(targetPow);
            shooter.setVelocity(targetVel);

            m_telemetry.addData("Status", "Running");
            m_telemetry.addData("Target Vel", targetVel);
            m_telemetry.addData("Error", targetVel - shooter.getVelocity());
            m_telemetry.addData("Shooter Pow", shooter.getPower());
            m_telemetry.addData("Shooter Vel", shooter.getVelocity());
            m_telemetry.addLine();

            m_telemetry.addData("Target Pow", targetPow);

            m_telemetry.update();
            idle();
        }
    }
}

