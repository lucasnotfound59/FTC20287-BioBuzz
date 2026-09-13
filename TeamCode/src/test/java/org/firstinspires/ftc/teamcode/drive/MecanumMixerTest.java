package org.firstinspires.ftc.teamcode.drive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MecanumMixerTest {
    private static final double EPSILON = 1e-9;

    @Test
    public void zeroInputStopsAllWheels() {
        assertPowers(MecanumMixer.mix(0, 0, 0), 0, 0, 0, 0);
    }

    @Test
    public void forwardDrivesAllWheelsForward() {
        assertPowers(MecanumMixer.mix(1, 0, 0), 1, 1, 1, 1);
    }

    @Test
    public void strafeUsesMecanumSigns() {
        assertPowers(MecanumMixer.mix(0, 1, 0), 1, -1, -1, 1);
    }

    @Test
    public void turnUsesOppositeSideSigns() {
        assertPowers(MecanumMixer.mix(0, 0, 1), 1, -1, 1, -1);
    }

    @Test
    public void mixedInputIsNormalized() {
        WheelPowers powers = MecanumMixer.mix(1, 1, 1);

        assertPowers(powers, 1, -1.0 / 3.0, 1.0 / 3.0, 1.0 / 3.0);
        assertTrue(powers.maxMagnitude() <= 1.0);
    }

    private static void assertPowers(
            WheelPowers actual,
            double frontLeft,
            double frontRight,
            double backLeft,
            double backRight) {
        assertEquals(frontLeft, actual.frontLeft, EPSILON);
        assertEquals(frontRight, actual.frontRight, EPSILON);
        assertEquals(backLeft, actual.backLeft, EPSILON);
        assertEquals(backRight, actual.backRight, EPSILON);
    }
}
