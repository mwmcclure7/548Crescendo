package frc.robot.commands.Swerve;

import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.Constants;
import frc.robot.Vision.LimelightHelpers;
import frc.robot.subsystems.Drivetrain.Drivetrain;
import frc.robot.subsystems.Drivetrain.SwerveRequest;

public class DriveToNote extends Command {
    Drivetrain drivetrain;
    SwerveRequest.RobotCentric driveRequest;
    PIDController pidRadianController;

    public DriveToNote() {
        drivetrain = Drivetrain.getInstance();
        driveRequest = new SwerveRequest.RobotCentric();

        // TODO: tune
        pidRadianController = new PIDController(12, 12, 1);

        this.addRequirements(drivetrain);
        this.setName("Drive To Note");
    }

    @Override
    public void initialize() {
        driveRequest.Deadband = Constants.OperatorConstants.deadband;
        driveRequest.RotationalDeadband = Constants.OperatorConstants.rotationalDeadband;
    }

    @Override
    public void execute() {
        driveRequest.RotationalRate = pidRadianController
                .calculate(Units.degreesToRadians(LimelightHelpers.getTX(Constants.Vision.llPython)));

        driveRequest.VelocityX = Constants.SwerveConstants.kMaxSpeedMetersPerSecond * 1;

        drivetrain.setControl(driveRequest
                .withRotationalRate(Units
                        .degreesToRadians(pidRadianController.calculate(LimelightHelpers.getTX(Constants.Vision.llPython)))));
    }
}