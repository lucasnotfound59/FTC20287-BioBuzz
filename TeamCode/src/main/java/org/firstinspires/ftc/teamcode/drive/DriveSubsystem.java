package org.firstinspires.ftc.teamcode.drive;

import com.qualcomm.robotcore.hardware.DcMotorEx;
import org.firstinspires.ftc.teamcode.framework.command.Subsystem;

/** Owns all drivetrain motor output. */
public final class DriveSubsystem implements Subsystem {
    private final DcMotorEx frontLeft;
    private final DcMotorEx frontRight;
    private final DcMotorEx backLeft;
    private final DcMotorEx backRight;

    public DriveSubsystem(
            DcMotorEx frontLeft,
            DcMotorEx frontRight,
            DcMotorEx backLeft,
            DcMotorEx backRight) {
        this.frontLeft = requireMotor(frontLeft, "frontLeft");
        this.frontRight = requireMotor(frontRight, "frontRight");
        this.backLeft = requireMotor(backLeft, "backLeft");
        this.backRight = requireMotor(backRight, "backRight");
        stop();
    }

    public void drive(WheelPowers powers) {
        if (powers == null) {
            throw new IllegalArgumentException("powers cannot be null");
        }
        frontLeft.setPower(clamp(powers.frontLeft));
        frontRight.setPower(clamp(powers.frontRight));
        backLeft.setPower(clamp(powers.backLeft));
        backRight.setPower(clamp(powers.backRight));
    }

    @Override
    public void stop() {
        frontLeft.setPower(0.0);
        frontRight.setPower(0.0);
        backLeft.setPower(0.0);
        backRight.setPower(0.0);
    }

    private static double clamp(double power) {
        return Math.max(-1.0, Math.min(1.0, power));
    }

    private static DcMotorEx requireMotor(DcMotorEx motor, String name) {
        if (motor == null) {
            throw new IllegalArgumentException(name + " cannot be null");
        }
        return motor;
    }
}
