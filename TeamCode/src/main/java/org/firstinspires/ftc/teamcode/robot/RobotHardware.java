package org.firstinspires.ftc.teamcode.robot;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import org.firstinspires.ftc.teamcode.drive.DriveSubsystem;

/** Creates configured SDK hardware without exposing hardware lookup to commands. */
public final class RobotHardware {
    private RobotHardware() {
    }

    public static DriveSubsystem createDrive(HardwareMap hardwareMap) {
        if (hardwareMap == null) {
            throw new IllegalArgumentException("hardwareMap cannot be null");
        }

        DcMotorEx frontLeft = motor(
                hardwareMap,
                RobotConfig.FRONT_LEFT_DRIVE,
                RobotConfig.LEFT_DRIVE_DIRECTION);
        DcMotorEx frontRight = motor(
                hardwareMap,
                RobotConfig.FRONT_RIGHT_DRIVE,
                RobotConfig.RIGHT_DRIVE_DIRECTION);
        DcMotorEx backLeft = motor(
                hardwareMap,
                RobotConfig.BACK_LEFT_DRIVE,
                RobotConfig.LEFT_DRIVE_DIRECTION);
        DcMotorEx backRight = motor(
                hardwareMap,
                RobotConfig.BACK_RIGHT_DRIVE,
                RobotConfig.RIGHT_DRIVE_DIRECTION);

        return new DriveSubsystem(frontLeft, frontRight, backLeft, backRight);
    }

    private static DcMotorEx motor(
            HardwareMap hardwareMap,
            String name,
            DcMotorSimple.Direction direction) {
        DcMotorEx motor;
        try {
            motor = hardwareMap.get(DcMotorEx.class, name);
        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "Missing or invalid DcMotorEx configuration: " + name,
                    exception);
        }

        motor.setPower(0.0);
        motor.setDirection(direction);
        motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        return motor;
    }
}
