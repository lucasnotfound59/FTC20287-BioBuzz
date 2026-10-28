package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.seattlesolvers.solverslib.command.SubsystemBase;

/** Owns the shooter motor; the command decides when to fire. Uses open-loop power control. */
public final class Shooter extends SubsystemBase {
    public static final String HARDWARE_NAME="shooter";
    // Initial commanded power; verify and tune on the current mechanism.
    public static final double SHOOT_POWER=1.0;
    public static final DcMotorSimple.Direction DIRECTION=DcMotorSimple.Direction.FORWARD;
    private final DcMotorEx motor;
    private double commandedPower;

    public Shooter(final HardwareMap hMap) {
        if (hMap == null) {
            throw new IllegalArgumentException("hardwareMap cannot be null");
        }
        motor=hMap.get(DcMotorEx.class, HARDWARE_NAME);
        try {
            stop();
            motor.setDirection(DIRECTION);
            motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
            motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        } catch (RuntimeException | Error failure) {
            stopAfterFailure(failure);
            throw failure;
        }
    }

    public void shoot() {
        setPower(SHOOT_POWER);
    }

    public void setPower(double power) {
        try {
            if (!Double.isFinite(power)) {
                throw new IllegalArgumentException("shooter power must be finite");
            }
            double limitedPower=Math.max(-1.0, Math.min(1.0, power));
            motor.setPower(limitedPower);
            commandedPower=limitedPower;
        } catch (RuntimeException | Error failure) {
            stopAfterFailure(failure);
            throw failure;
        }
    }

    public void stop() {
        setPower(0.0);
    }

    /** Last successfully written command, not an RPM or motion measurement. */
    public double getCommandedPower() {
        return commandedPower;
    }

    private void stopAfterFailure(Throwable failure) {
        try {
            motor.setPower(0.0);
            commandedPower=0.0;
        } catch (RuntimeException | Error stopFailure) {
            if (failure != stopFailure) {
                failure.addSuppressed(stopFailure);
            }
        }
    }
}
