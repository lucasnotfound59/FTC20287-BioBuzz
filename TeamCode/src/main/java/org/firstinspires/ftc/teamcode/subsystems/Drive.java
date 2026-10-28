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
        DcMotorEx[] motors=new DcMotorEx[4];
        try {
            motors[0]=hMap.get(DcMotorEx.class, "frontLeft");
            motors[1]=hMap.get(DcMotorEx.class, "frontRight");
            motors[2]=hMap.get(DcMotorEx.class, "backLeft");
            motors[3]=hMap.get(DcMotorEx.class, "backRight");
            configure(motors[0], DcMotorSimple.Direction.REVERSE);
            configure(motors[1], DcMotorSimple.Direction.FORWARD);
            configure(motors[2], DcMotorSimple.Direction.REVERSE);
            configure(motors[3], DcMotorSimple.Direction.FORWARD);
        } catch (RuntimeException | Error failure) {
            // A failed constructor cannot be reached by the OpMode's cleanup field yet.
            for (DcMotorEx motor : motors) {
                if (motor != null) {
                    stopMotor(motor, failure);
                }
            }
            throw failure;
        }
        frontLeft=motors[0];
        frontRight=motors[1];
        backLeft=motors[2];
        backRight=motors[3];
    }

    /** Sets normalized robot-centric forward, right-strafe, and turn inputs. */
    public void drive(double forward, double strafe, double turn) {
        drive(forward, strafe, turn, 1.0);
    }

    /** Limits each wheel after normalization, preserving the requested movement direction. */
    public void drive(double forward, double strafe, double turn, double outputLimit) {
        requireFinite(forward, "forward");
        requireFinite(strafe, "strafe");
        requireFinite(turn, "turn");
        if (!Double.isFinite(outputLimit) || outputLimit<0.0 || outputLimit>1.0) {
            throw new IllegalArgumentException("output limit must be finite and in [0, 1]");
        }

        double fl = forward + strafe + turn;
        double fr = forward - strafe - turn;
        double bl = forward - strafe + turn;
        double br = forward + strafe - turn;
        double scale = Math.max(1.0, Math.max(
                Math.max(Math.abs(fl), Math.abs(fr)), Math.max(Math.abs(bl), Math.abs(br))));

        frontLeft.setPower(fl/scale*outputLimit);
        frontRight.setPower(fr/scale*outputLimit);
        backLeft.setPower(bl/scale*outputLimit);
        backRight.setPower(br/scale*outputLimit);
    }

    public void stop() {
        Throwable failure=null;
        failure=stopMotor(frontLeft, failure);
        failure=stopMotor(frontRight, failure);
        failure=stopMotor(backLeft, failure);
        failure=stopMotor(backRight, failure);
        if (failure instanceof RuntimeException) {
            throw (RuntimeException) failure;
        }
        if (failure != null) {
            throw (Error) failure;
        }
    }

    private static Throwable stopMotor(DcMotorEx motor, Throwable failure) {
        try {
            motor.setPower(0.0);
        } catch (RuntimeException | Error nextFailure) {
            if (failure == null) {
                return nextFailure;
            }
            if (failure != nextFailure) {
                failure.addSuppressed(nextFailure);
            }
        }
        return failure;
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
