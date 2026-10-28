package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.hardware.digitalchickenlabs.OctoQuad;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.seattlesolvers.solverslib.command.SubsystemBase;

/** Reads OctoQuad MK2 heading and holds a fixed reference captured at START. */
public final class OctoQuadSubsystem extends SubsystemBase {
    private final OctoQuad octoQuad;
    private final OctoQuad.LocalizerDataBlock data = new OctoQuad.LocalizerDataBlock();
    private boolean valid;
    private double startHeadingRadians = Double.NaN;
    private int badCrcPackets;

    public OctoQuadSubsystem(final HardwareMap hMap) {
        if (hMap == null) {
            throw new IllegalArgumentException("hardwareMap cannot be null");
        }
        octoQuad = hMap.get(OctoQuad.class, "octoquad");
        if (octoQuad.getChipId() != OctoQuad.OCTOQUAD_CHIP_ID) {
            throw new IllegalStateException("Cannot communicate with OctoQuad; check I2C wiring.");
        }
        OctoQuad.FirmwareVersion version = octoQuad.getFirmwareVersion();
        if (version.maj != OctoQuad.SUPPORTED_FW_VERSION_MAJ) {
            throw new IllegalStateException("OctoQuad firmware major must match SDK support: "
                    + OctoQuad.SUPPORTED_FW_VERSION_MAJ);
        }
        // Recalibrate local IMU state without changing saved pod geometry or heading scale.
        octoQuad.resetLocalizerAndCalibrateIMU();
    }

    @Override
    public void periodic() {
        valid = false;
        octoQuad.readLocalizerData(data);
        if (!data.crcOk) {
            badCrcPackets++;
        }
        valid = data.isDataValid() && Float.isFinite(data.heading_rad);
        if (data.crcOk && data.localizerStatus == OctoQuad.LocalizerStatus.FAULT_NO_IMU) {
            throw new IllegalStateException("OctoQuad reports no IMU; field-centric drive requires MK2.");
        }
    }

    /** Returns current heading relative to the direction captured at START, or NaN if invalid. */
    public double getHeadingRadians() {
        if (!valid || !Double.isFinite(startHeadingRadians)) {
            return Double.NaN;
        }
        double relativeHeading = data.heading_rad - startHeadingRadians;
        return Math.atan2(Math.sin(relativeHeading), Math.cos(relativeHeading));
    }

    /** Locks the latest valid IMU sample as the field-centric zero direction. */
    public void captureStartHeading() {
        if (Double.isFinite(startHeadingRadians)) {
            throw new IllegalStateException("START heading is already locked for this OpMode.");
        }
        if (!valid) {
            throw new IllegalStateException("OctoQuad heading is invalid at START; re-INIT and wait.");
        }
        startHeadingRadians = data.heading_rad;
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
