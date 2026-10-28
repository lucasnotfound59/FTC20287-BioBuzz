package org.firstinspires.ftc.teamcode.drive;

import com.qualcomm.hardware.digitalchickenlabs.OctoQuad;
import org.firstinspires.ftc.teamcode.framework.command.Subsystem;

/** Owns OctoQuad MK2 heading reads; invalid packets are never used for driving. */
public final class OctoQuadHeadingSubsystem implements Subsystem {
    private final OctoQuad octoquad;
    private final OctoQuad.LocalizerDataBlock data=new OctoQuad.LocalizerDataBlock();
    private boolean valid;
    private double startHeadingRadians=Double.NaN;
    private int badCrcPackets;

    public OctoQuadHeadingSubsystem(OctoQuad octoquad) {
        if (octoquad == null) {
            throw new IllegalArgumentException("octoquad cannot be null");
        }
        this.octoquad=octoquad;
        if (octoquad.getChipId() != OctoQuad.OCTOQUAD_CHIP_ID) {
            throw new IllegalStateException("Cannot communicate with OctoQuad; check I2C wiring.");
        }
        if (octoquad.getFirmwareVersion().maj != OctoQuad.SUPPORTED_FW_VERSION_MAJ) {
            throw new IllegalStateException("OctoQuad firmware major must match SDK support: "
                    +OctoQuad.SUPPORTED_FW_VERSION_MAJ);
        }
        // Preserve stored pod geometry and heading scalar; only recalibrate/reset the localizer.
        octoquad.resetLocalizerAndCalibrateIMU();
    }

    @Override
    public void periodic() {
        valid=false;
        octoquad.readLocalizerData(data);
        if (!data.crcOk) {
            badCrcPackets++;
        }
        valid=data.isDataValid() && Float.isFinite(data.heading_rad);
        if (data.crcOk && data.localizerStatus == OctoQuad.LocalizerStatus.FAULT_NO_IMU) {
            throw new IllegalStateException("OctoQuad reports no IMU; headless drive requires MK2.");
        }
    }

    /** Returns rotation from the fixed START direction; reading never changes the reference. */
    public double getHeadingRadians() {
        if (!valid || !Double.isFinite(startHeadingRadians)) {
            return Double.NaN;
        }
        double heading=data.heading_rad-startHeadingRadians;
        return Math.atan2(Math.sin(heading), Math.cos(heading));
    }

    /** Captures the current sample exactly once, when the driver presses START. */
    public void captureStartHeading() {
        if (Double.isFinite(startHeadingRadians)) {
            throw new IllegalStateException("START heading is already locked for this OpMode.");
        }
        if (!valid) {
            throw new IllegalStateException(
                    "OctoQuad not ready at START; wait for RUNNING, then re-INIT.");
        }
        startHeadingRadians=data.heading_rad;
    }

    public boolean isValid() {
        return valid;
    }

    public String getStatus() {
        if (!data.crcOk) {
            return "INVALID CRC: drive stopped";
        }
        if (!Float.isFinite(data.heading_rad)) {
            return "INVALID HEADING: drive stopped";
        }
        return String.valueOf(data.localizerStatus);
    }

    public int getBadCrcPackets() {
        return badCrcPackets;
    }

    @Override
    public void stop() {
        valid=false;
    }
}
