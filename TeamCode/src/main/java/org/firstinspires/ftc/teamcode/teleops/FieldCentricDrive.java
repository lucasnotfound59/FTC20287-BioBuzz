package org.firstinspires.ftc.teamcode.teleops;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.commands.DriveCommand;
import org.firstinspires.ftc.teamcode.subsystems.Drive;
import org.firstinspires.ftc.teamcode.subsystems.GamepadConfig;
import org.firstinspires.ftc.teamcode.subsystems.Localizer;

/** Field-centric mecanum drive with its forward direction locked after START. */
@TeleOp(name = "Field-Centric")
public final class FieldCentricDrive extends BaseDriveTeleOp {
    private Localizer localizer;
    private boolean startHeadingCaptured;

    @Override
    protected DriveCommand createDriveCommand(Drive drive, GamepadConfig controls) {
        localizer = new Localizer(hardwareMap);
        startHeadingCaptured = false;
        return new DriveCommand(
                drive,
                controls::getForward,
                controls::getStrafe,
                controls::getTurn,
                localizer,
                GamepadConfig.DRIVE_DEADBAND,
                GamepadConfig.TURN_LIMIT);
    }

    @Override
    protected void updateDriveInitialization() {
        localizer.periodic();
        telemetry.addData("OctoQuad", localizer.getStatus());
        telemetry.addLine("Keep still; wait for RUNNING before START.");
        telemetry.addLine("START locks this facing as forward for the entire run.");
    }

    @Override
    protected void beforeDriveRun() {
        // 0.3.4 has no preRun hook: capture before the first active scheduler run.
        if (!startHeadingCaptured) {
            localizer.periodic();
            localizer.captureStartHeading();
            startHeadingCaptured = Double.isFinite(localizer.getHeadingRadians());
        }
    }

    @Override
    protected void addDriveTelemetry() {
        telemetry.addData("OctoQuad", localizer.getStatus());
        telemetry.addData("Heading valid", localizer.isValid());
        telemetry.addData("Heading from START (deg)", Math.toDegrees(localizer.getHeadingRadians()));
        telemetry.addData("Bad CRC packets", localizer.getBadCrcPackets());
        if (!Double.isFinite(localizer.getHeadingRadians())) {
            telemetry.addData("Drive", "STOPPED: heading unavailable");
        } else {
            telemetry.addData("Drive", "START reference");
        }
    }
}
