package org.firstinspires.ftc.teamcode.drive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.qualcomm.robotcore.hardware.DcMotorEx;
import java.lang.reflect.Proxy;
import org.junit.Test;

public class DriveSubsystemTest {
    private static final class MotorRecorder {
        int setPowerCalls;
        boolean throwOnZero;

        DcMotorEx proxy() {
            return (DcMotorEx) Proxy.newProxyInstance(
                    DcMotorEx.class.getClassLoader(),
                    new Class<?>[] {DcMotorEx.class},
                    (proxy, method, arguments) -> {
                        if (method.getName().equals("setPower")) {
                            setPowerCalls++;
                            double power = (double) arguments[0];
                            if (throwOnZero && power == 0.0) {
                                throw new IllegalStateException("motor stop failed");
                            }
                            return null;
                        }
                        throw new UnsupportedOperationException(method.getName());
                    });
        }
    }

    @Test
    public void stopAttemptsAllMotorsBeforePropagatingFailure() {
        MotorRecorder frontLeft = new MotorRecorder();
        MotorRecorder frontRight = new MotorRecorder();
        MotorRecorder backLeft = new MotorRecorder();
        MotorRecorder backRight = new MotorRecorder();
        DriveSubsystem drive = new DriveSubsystem(
                frontLeft.proxy(),
                frontRight.proxy(),
                backLeft.proxy(),
                backRight.proxy());
        frontLeft.setPowerCalls = 0;
        frontRight.setPowerCalls = 0;
        backLeft.setPowerCalls = 0;
        backRight.setPowerCalls = 0;
        frontLeft.throwOnZero = true;

        try {
            drive.stop();
            fail("motor stop failure must be propagated");
        } catch (IllegalStateException expected) {
            assertEquals("motor stop failed", expected.getMessage());
        }

        assertEquals(1, frontLeft.setPowerCalls);
        assertEquals(1, frontRight.setPowerCalls);
        assertEquals(1, backLeft.setPowerCalls);
        assertEquals(1, backRight.setPowerCalls);
    }
}
