package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.seattlesolvers.solverslib.command.SubsystemBase;

import org.jetbrains.annotations.NotNull;

/** Owns the shooter motor; the TeleOp decides when to fire. */
public final class Shooter extends SubsystemBase {
    // Default velocity in encoder ticks per second; tune in ConfigTeleOp.
    public static double SHOOT_VEL = 1560;
    private final DcMotorEx shooter;
    private double commandedVel;

    public Shooter(@NotNull final HardwareMap hMap) {
        shooter = hMap.get(DcMotorEx.class, "shooter");
        shooter.setDirection(DcMotorSimple.Direction.FORWARD);
        shooter.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        shooter.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        stop();
    }

    public void shoot() {
        setVelocity(SHOOT_VEL);
    }

    public void setVelocity(double velocity) {
        shooter.setVelocity(velocity);
        commandedVel = velocity;
    }

    public void stop() {
        shooter.setPower(0.0);
        commandedVel = 0.0;
    }

    /** Last successfully written command, not an RPM or motion measurement. */
    public double getCommandedVel() {
        return commandedVel;
    }
}
