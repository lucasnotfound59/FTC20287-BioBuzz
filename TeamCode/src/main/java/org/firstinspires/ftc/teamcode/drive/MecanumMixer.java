package org.firstinspires.ftc.teamcode.drive;

/** Pure robot-centric mecanum drive mixing. */
public final class MecanumMixer {
    private MecanumMixer() {
    }

    /** Field inputs use forward/right; heading is counterclockwise-positive radians. */
    public static WheelPowers mixFieldCentric(
            double forward, double strafe, double turn, double headingRadians) {
        if (!Double.isFinite(headingRadians)) {
            throw new IllegalArgumentException("heading must be finite");
        }
        double cosine=Math.cos(headingRadians);
        double sine=Math.sin(headingRadians);
        double robotForward=forward*cosine-strafe*sine;
        double robotStrafe=forward*sine+strafe*cosine;
        return mix(robotForward, robotStrafe, turn);
    }

    public static WheelPowers mix(double forward, double strafe, double turn) {
        double frontLeft = forward + strafe + turn;
        double frontRight = forward - strafe - turn;
        double backLeft = forward - strafe + turn;
        double backRight = forward + strafe - turn;
        double denominator = Math.max(
                1.0,
                Math.max(
                        Math.max(Math.abs(frontLeft), Math.abs(frontRight)),
                        Math.max(Math.abs(backLeft), Math.abs(backRight))));

        return new WheelPowers(
                frontLeft / denominator,
                frontRight / denominator,
                backLeft / denominator,
                backRight / denominator);
    }
}
