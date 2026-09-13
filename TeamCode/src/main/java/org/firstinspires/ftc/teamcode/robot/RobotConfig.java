package org.firstinspires.ftc.teamcode.robot;

import com.qualcomm.robotcore.hardware.DcMotorSimple;

/** Central configuration for values that must be verified on the current robot. */
public final class RobotConfig {
    private RobotConfig() {
    }

    // UNVERIFIED FOR THE 2026-2027 ROBOT: match these to the Robot Controller configuration.
    public static final String FRONT_LEFT_DRIVE = "front_left_drive";
    public static final String FRONT_RIGHT_DRIVE = "front_right_drive";
    public static final String BACK_LEFT_DRIVE = "back_left_drive";
    public static final String BACK_RIGHT_DRIVE = "back_right_drive";

    // UNVERIFIED FOR THE CURRENT DRIVETRAIN: lift the robot before checking wheel directions.
    public static final DcMotorSimple.Direction LEFT_DRIVE_DIRECTION =
            DcMotorSimple.Direction.REVERSE;
    public static final DcMotorSimple.Direction RIGHT_DRIVE_DIRECTION =
            DcMotorSimple.Direction.FORWARD;

    public static final double DRIVE_DEADBAND = 0.05;
    public static final double DRIVE_POWER_SCALE = 1.0;
}
