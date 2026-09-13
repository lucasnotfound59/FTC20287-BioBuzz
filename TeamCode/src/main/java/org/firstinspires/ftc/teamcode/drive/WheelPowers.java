package org.firstinspires.ftc.teamcode.drive;

/** Immutable normalized powers for a four-motor mecanum drivetrain. */
public final class WheelPowers {
    public final double frontLeft;
    public final double frontRight;
    public final double backLeft;
    public final double backRight;

    public WheelPowers(
            double frontLeft,
            double frontRight,
            double backLeft,
            double backRight) {
        this.frontLeft = frontLeft;
        this.frontRight = frontRight;
        this.backLeft = backLeft;
        this.backRight = backRight;
    }

    public double maxMagnitude() {
        return Math.max(
                Math.max(Math.abs(frontLeft), Math.abs(frontRight)),
                Math.max(Math.abs(backLeft), Math.abs(backRight)));
    }
}
