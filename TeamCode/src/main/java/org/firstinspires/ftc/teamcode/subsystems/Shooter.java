package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.seattlesolvers.solverslib.command.SubsystemBase;

/** Owns a single shooter motor. Shooter control behavior belongs in commands. */
public final class Shooter extends SubsystemBase {
    private final DcMotorEx motor;

    public Shooter(final HardwareMap hMap) {
        if (hMap == null) {
            throw new IllegalArgumentException("hardwareMap cannot be null");
        }
        motor = hMap.get(DcMotorEx.class, "shooter");
        motor.setPower(0.0);
        motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
    }

    public void setPower(double power) {
        if (!Double.isFinite(power)) {
            throw new IllegalArgumentException("shooter power must be finite");
        }
        motor.setPower(Math.max(-1.0, Math.min(1.0, power)));
    }

    public void stop() {
        motor.setPower(0.0);
    }
}
