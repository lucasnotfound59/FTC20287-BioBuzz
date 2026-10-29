package org.firstinspires.ftc.teamcode.tests.shooter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Encoder observations around manual markers, not a ball detector. Times are monotonic seconds. */
public final class ShotSpeedTrial {
    public static final int SHOTS = 4;

    public static final class Shot {
        public final int number;
        public final double markedAt;
        public final double before;
        public final boolean readyAtMark;
        public double minimum;
        public double maximum;
        /** From manual marker to the first confirmed in-band dwell AFTER an observed disturbance. */
        public double recoverySeconds = Double.NaN;
        public String endReason = "observing";
        private boolean departed;
        private double inBandSince = Double.NaN;

        private Shot(int number, double now, double speed, boolean ready) {
            this.number = number;
            markedAt = now;
            before = speed;
            minimum = speed;
            maximum = speed;
            readyAtMark = ready;
        }

        public boolean sawDisturbance() {
            return departed;
        }
    }

    private final double target;
    private final double tolerance;
    private final double dwell;
    private final double window;
    private final List<Shot> shots = new ArrayList<>();
    private Shot active;
    private double readySince = Double.NaN;
    private boolean ready;

    public ShotSpeedTrial(double target, double tolerance, double dwell, double window) {
        if (!Double.isFinite(target) || target <= 0 || !Double.isFinite(tolerance)
                || tolerance <= 0 || tolerance >= target || !Double.isFinite(dwell)
                || dwell <= 0 || !Double.isFinite(window) || window < dwell) {
            throw new IllegalArgumentException("Invalid trial settings");
        }
        this.target = target;
        this.tolerance = tolerance;
        this.dwell = dwell;
        this.window = window;
    }

    public void sample(double now, double speed, boolean running) {
        if (!running || !Double.isFinite(speed)) {
            readySince = Double.NaN;
            ready = false;
            close("stopped / interrupted");
            return;
        }
        boolean inBand = Math.abs(speed - target) <= tolerance;
        if (!inBand) {
            readySince = Double.NaN;
        } else if (Double.isNaN(readySince)) {
            readySince = now;
        }
        ready = inBand && now - readySince >= dwell;

        // Never include a late sample (or motor coast-down) in a finished window.
        if (active != null && now - active.markedAt > window) {
            close("window elapsed");
        }
        if (active == null) {
            return;
        }
        active.minimum = Math.min(active.minimum, speed);
        active.maximum = Math.max(active.maximum, speed);
        if (!inBand) {
            active.departed = true;
            active.inBandSince = Double.NaN;
        } else if (active.departed) {
            if (Double.isNaN(active.inBandSince)) {
                active.inBandSince = now;
            }
            if (Double.isNaN(active.recoverySeconds) && now - active.inBandSince >= dwell) {
                active.recoverySeconds = now - active.markedAt;
            }
        }
        if (now - active.markedAt >= window) {
            close("window elapsed");
        }
    }

    /** Call immediately BEFORE feeding a ball; a new marker ends the previous observation. */
    public boolean mark(double now, double speed) {
        if (shots.size() >= SHOTS || !Double.isFinite(speed)) {
            return false;
        }
        close("next marker");
        active = new Shot(shots.size() + 1, now, speed, ready && Math.abs(speed - target) <= tolerance);
        shots.add(active);
        // A marker that is already out of band must not be called a ready shot.
        active.departed = Math.abs(speed - target) > tolerance;
        return true;
    }

    public void reset() {
        shots.clear();
        active = null;
        readySince = Double.NaN;
        ready = false;
    }

    private void close(String reason) {
        if (active != null) {
            active.endReason = reason;
            active = null;
        }
    }

    public boolean isReady() {
        return ready;
    }

    public List<Shot> getShots() {
        return Collections.unmodifiableList(shots);
    }

    public boolean isComplete() {
        return shots.size() == SHOTS && active == null;
    }

    public double getPreShotSpread() {
        if (shots.isEmpty()) {
            return Double.NaN;
        }
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (Shot shot : shots) {
            min = Math.min(min, shot.before);
            max = Math.max(max, shot.before);
        }
        return max - min;
    }
}
