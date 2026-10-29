package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.hardware.digitalchickenlabs.OctoQuad;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.seattlesolvers.solverslib.command.SubsystemBase;

import org.jetbrains.annotations.NotNull;

/** Reads OctoQuad MK2 heading and holds a fixed reference captured at START. */
public final class Localizer extends SubsystemBase {
    private final OctoQuad octoQuad;
    private final OctoQuad.LocalizerDataBlock data = new OctoQuad.LocalizerDataBlock();
    private boolean valid;
    private double startHeadingRadians = Double.NaN; // needs to be measured
    private int badCrcPackets;

    public Localizer(@NotNull final HardwareMap hMap) {
        this.octoQuad = hMap.get(OctoQuad.class, "octoquad");
        // Recalibrate local IMU state without changing saved pod geometry or heading scale.
        this.octoQuad.resetLocalizerAndCalibrateIMU();
    }

    @Override
    public void periodic() {
        valid = false;
        octoQuad.readLocalizerData(data);
        if (!data.crcOk) {
            badCrcPackets++;
        }
        valid = data.isDataValid() && Float.isFinite(data.heading_rad);
    }

    /** Returns current heading relative to the direction captured at START, or NaN if invalid. */
    public double getHeadingRadians() {
        if (!valid || !Double.isFinite(startHeadingRadians)) {
            return Double.NaN;
        }
        double relativeHeading = data.heading_rad - startHeadingRadians;
        return Math.atan2(Math.sin(relativeHeading), Math.cos(relativeHeading));
    }

    /** Locks the first valid sample requested after START as the field-centric zero direction. */
    public void captureStartHeading() {
        if (valid && !Double.isFinite(startHeadingRadians)) {
            startHeadingRadians = data.heading_rad;
        }
    }

    public boolean isValid() {
        return valid;
    }

    public String getStatus() {
        if (!data.crcOk) {
            return "INVALID CRC";
        }
        if (!Float.isFinite(data.heading_rad)) {
            return "INVALID HEADING";
        }
        return String.valueOf(data.localizerStatus);
    }

    public int getBadCrcPackets() {
        return badCrcPackets;
    }
}
