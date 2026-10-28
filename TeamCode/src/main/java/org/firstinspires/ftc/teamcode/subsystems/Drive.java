package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.seattlesolvers.solverslib.command.SubsystemBase;

/** Owns the four mecanum drive motors. */
public final class Drive extends SubsystemBase {
    private final DcMotorEx frontLeft;
    private final DcMotorEx frontRight;
    private final DcMotorEx backLeft;
    private final DcMotorEx backRight;

    public Drive(final HardwareMap hMap) {
        if (hMap == null) {
            throw new IllegalArgumentException("hardwareMap cannot be null");
        }
        frontLeft = hMap.get(DcMotorEx.class, "frontLeft");
        frontRight = hMap.get(DcMotorEx.class, "frontRight");
        backLeft = hMap.get(DcMotorEx.class, "backLeft");
        backRight = hMap.get(DcMotorEx.class, "backRight");

        configure(this.frontLeft, DcMotorSimple.Direction.REVERSE);
        configure(this.frontRight, DcMotorSimple.Direction.FORWARD);
        configure(this.backLeft, DcMotorSimple.Direction.REVERSE);
        configure(this.backRight, DcMotorSimple.Direction.FORWARD);
    }

    /** Sets normalized robot-centric forward, right-strafe, and counterclockwise-turn inputs. */
    public void drive(double forward, double strafe, double turn) {
        requireFinite(forward, "forward");
        requireFinite(strafe, "strafe");
        requireFinite(turn, "turn");

        double fl = forward + strafe + turn;
        double fr = forward - strafe - turn;
        double bl = forward - strafe + turn;
        double br = forward + strafe - turn;
        double scale = Math.max(1.0, Math.max(
                Math.max(Math.abs(fl), Math.abs(fr)), Math.max(Math.abs(bl), Math.abs(br))));

        frontLeft.setPower(fl / scale);
        frontRight.setPower(fr / scale);
        backLeft.setPower(bl / scale);
        backRight.setPower(br / scale);
    }

    public void stop() {
        frontLeft.setPower(0.0);
        frontRight.setPower(0.0);
        backLeft.setPower(0.0);
        backRight.setPower(0.0);
    }

    private static void configure(DcMotorEx motor, DcMotorSimple.Direction direction) {
        motor.setPower(0.0);
        motor.setDirection(direction);
        motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " input must be finite");
        }
    }
}
