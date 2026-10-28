package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.seattlesolvers.solverslib.command.SubsystemBase;

/** Owns the intake motor; the command decides when to collect. */
public final class Intake extends SubsystemBase {
    public static final String HARDWARE_NAME="intake";
    // Initial commanded power; verify direction and tune on the current mechanism.
    public static final double INTAKE_POWER=1.0;
    public static final DcMotorSimple.Direction DIRECTION=DcMotorSimple.Direction.FORWARD;
    private final DcMotorEx motor;
    private double commandedPower;

    public Intake(final HardwareMap hMap) {
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

    public void intake() {
        setPower(INTAKE_POWER);
    }

    public void setPower(double power) {
        try {
            if (!Double.isFinite(power)) {
                throw new IllegalArgumentException("intake power must be finite");
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

    /** Last successfully written command, not a measurement of mechanism motion. */
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
