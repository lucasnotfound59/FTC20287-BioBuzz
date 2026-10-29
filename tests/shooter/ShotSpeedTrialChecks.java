import org.firstinspires.ftc.teamcode.tests.shooter.ShotSpeedTrial;

/** Hardware-free behavioral checks. Run using tests/shooter/run-checks.sh. */
public final class ShotSpeedTrialChecks {
    private static int checks;

    public static void main(String[] args) {
        ShotSpeedTrial trial = new ShotSpeedTrial(1560, 40, 0.15, 2);
        trial.sample(0, 1560, true);
        check(!trial.isReady(), "first in-band sample is not readiness");
        trial.sample(0.2, 1555, true);
        check(trial.isReady(), "continuous dwell becomes ready");
        trial.sample(0.21, 1500, true);
        check(!trial.isReady(), "a dip immediately revokes readiness");
        trial.sample(0.3, 1560, true);
        trial.sample(0.4, 1560, true);
        check(!trial.isReady(), "readiness dwell restarts after a dip");
        trial.sample(0.5, 1560, true);
        check(trial.mark(0.5, 1560), "first marker accepted");
        trial.sample(0.7, 1555, true);
        check(Double.isNaN(trial.getShots().get(0).recoverySeconds), "no fake zero recovery before the ball arrives");
        trial.sample(0.8, 1200, true);
        trial.sample(0.9, 1620, true);
        trial.sample(1.0, 1560, true);
        trial.sample(1.1, 1550, true);
        check(Double.isNaN(trial.getShots().get(0).recoverySeconds), "recovery requires dwell");
        trial.sample(1.2, 1560, true);
        ShotSpeedTrial.Shot first = trial.getShots().get(0);
        near(first.recoverySeconds, 0.7, "recovery includes confirmation dwell from manual marker");
        near(first.minimum, 1200, "minimum captures load dip");
        near(first.maximum, 1620, "overshoot captured");
        check(first.readyAtMark, "readiness recorded at marker");

        trial.mark(1.3, 1500);
        check(!trial.getShots().get(1).readyAtMark, "out-of-band marker cannot inherit stale readiness");
        check(first.endReason.equals("next marker"), "next marker closes previous window");
        trial.sample(1.4, 900, true);
        near(first.minimum, 1200, "next ball cannot contaminate previous observation");
        check(Double.isNaN(trial.getShots().get(1).recoverySeconds), "unrecovered shot is unknown, not zero");
        trial.mark(1.5, 1450);
        trial.sample(1.6, 1000, true);
        trial.mark(1.7, 1440);
        check(!trial.mark(1.8, 1430), "fifth marker rejected");
        check(!trial.isComplete(), "fourth marker still needs its observation window");
        trial.sample(4, 500, true);
        check(trial.isComplete(), "fourth window completes after timeout");
        near(trial.getShots().get(3).minimum, 1440, "late sample excluded from completed window");
        near(trial.getPreShotSpread(), 120, "pre-shot spread measures four markers");

        trial.reset();
        check(trial.getShots().isEmpty() && !trial.isReady(), "reset clears trial and readiness");
        trial.sample(5, 1560, true);
        trial.sample(5.2, 1560, true);
        trial.mark(5.2, 1560);
        trial.sample(5.3, 1300, true);
        trial.sample(5.4, 0, false);
        near(trial.getShots().get(0).minimum, 1300, "stop/coast-down is not shot speed loss");
        check(trial.getShots().get(0).endReason.contains("interrupted"), "interruption labeled");
        check(!trial.isReady(), "stop clears readiness");
        trial.sample(5.6, 1560, true);
        check(!trial.isReady(), "restart requires a fresh dwell");
        trial.sample(5.8, Double.NaN, true);
        check(!trial.isReady(), "invalid encoder reading cannot become ready");

        expectInvalid(0, 40, 0.15, 2);
        expectInvalid(1560, 1560, 0.15, 2);
        expectInvalid(1560, 40, Double.NaN, 2);
        expectInvalid(1560, 40, 0.2, 0.1);
        System.out.println("PASS: " + checks + " shooter trial checks");
    }

    private static void expectInvalid(double target, double tolerance, double dwell, double window) {
        try {
            new ShotSpeedTrial(target, tolerance, dwell, window);
            throw new AssertionError("invalid settings accepted");
        } catch (IllegalArgumentException expected) {
            checks++;
        }
    }

    private static void near(double actual, double expected, String message) {
        check(Math.abs(actual - expected) < 1e-9, message);
    }

    private static void check(boolean value, String message) {
        if (!value) {
            throw new AssertionError(message);
        }
        checks++;
    }
}
