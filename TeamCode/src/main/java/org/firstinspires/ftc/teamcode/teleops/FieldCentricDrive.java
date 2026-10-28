package org.firstinspires.ftc.teamcode.teleops;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.seattlesolvers.solverslib.command.CommandOpMode;

import org.firstinspires.ftc.teamcode.commands.DriveCommand;
import org.firstinspires.ftc.teamcode.subsystems.Drive;
import org.firstinspires.ftc.teamcode.subsystems.OctoQuadSubsystem;

/** Field-centric mecanum drive with its forward direction locked when START is pressed. */
@TeleOp(name = "Field Centric Drive", group = "BIOBUZZ")
public final class FieldCentricDrive extends CommandOpMode {
    private static final double DRIVE_DEADBAND = 0.05;

    private OctoQuadSubsystem heading;

    @Override
    public void initialize() {
        Drive drive = new Drive(hardwareMap);
        heading = new OctoQuadSubsystem(hardwareMap);
        DriveCommand driveCommand = new DriveCommand(
                drive,
                () -> -gamepad1.left_stick_y,
                () -> gamepad1.left_stick_x,
                () -> gamepad1.right_stick_x,
                heading,
                DRIVE_DEADBAND);
        drive.setDefaultCommand(driveCommand);
    }

    @Override
    public void initialize_loop() {
        heading.periodic();
        telemetry.addData("OctoQuad", heading.getStatus());
        telemetry.addLine("Keep robot still during IMU calibration.");
        telemetry.addLine("Point robot forward before START to choose field-centric forward.");
        telemetry.update();
        sleep(50);
    }

    public void preRun() {
        heading.periodic();
        heading.captureStartHeading();
    }

    @Override
    public void run() {
        super.run();
        telemetry.addData("OctoQuad", heading.getStatus());
        telemetry.addData("Heading valid", heading.isValid());
        telemetry.addData("Heading from START (deg)",
                Math.toDegrees(heading.getHeadingRadians()));
        telemetry.addData("Bad CRC packets", heading.getBadCrcPackets());
        telemetry.update();
    }
}
