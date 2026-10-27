package org.firstinspires.ftc.teamcode.drive;

import com.qualcomm.hardware.digitalchickenlabs.OctoQuad;
import org.firstinspires.ftc.teamcode.framework.command.Subsystem;

/** Owns OctoQuad MK2 heading reads; invalid packets are never used for driving. */
public final class OctoQuadHeadingSubsystem implements Subsystem {
    private final OctoQuad octoquad;
    private final OctoQuad.LocalizerDataBlock data=new OctoQuad.LocalizerDataBlock();
    private boolean valid;
    private double zeroRadians=Double.NaN;
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

    /** The first usable drive-cycle sample defines field forward; Y can redefine it later. */
    public double getHeadingRadians() {
        if (!valid) {
            return Double.NaN;
        }
        if (!Double.isFinite(zeroRadians)) {
            zeroRadians=data.heading_rad;
        }
        double heading=data.heading_rad-zeroRadians;
        return Math.atan2(Math.sin(heading), Math.cos(heading));
    }

    /** Changes only the software reference; does not restart calibration while driving. */
    public void zeroHeading() {
        if (valid) {
            zeroRadians=data.heading_rad;
        }
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
