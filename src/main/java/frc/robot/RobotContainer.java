package frc.robot;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.wpilibj.GenericHID;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import frc.robot.commands.Spit;
import frc.robot.commands.Swerve.xDrive;
import frc.robot.commands.feeder.BeltFeed;
import frc.robot.commands.feeder.DeployAndIntake;
import frc.robot.commands.shooter.AimAndShoot;
import frc.robot.commands.shooter.FeedAndShoot;
import frc.robot.commands.shooter.FineAdjust;
import frc.robot.commands.shooter.SetPoint;
import frc.robot.subsystems.Arm;
import frc.robot.subsystems.Intake;
import frc.robot.subsystems.Drivetrain.Drivetrain;

public class RobotContainer {
	private final CommandXboxController xDrive = new CommandXboxController(0);
	private final CommandXboxController xManip = new CommandXboxController(1);
	private final GenericHID simController = new GenericHID(3);

	private final Drivetrain drivetrain = Drivetrain.getInstance();
	private final Arm mArm = Arm.getInstance();
	private final Intake mIntake = Intake.getInstance();

	private final Telemetry logger;
	public Field2d field;

	private void configureDriverBinds() {
		drivetrain.setDefaultCommand(
				new xDrive(() -> xDrive.getLeftX(), () -> xDrive.getLeftY(), () -> xDrive.getRightX(),
						() -> xDrive.getRightTriggerAxis()).ignoringDisable(true));

		xDrive.povDown().onTrue(drivetrain.run(() -> drivetrain.seedFieldRelative()));
		// xDrive.povDown().onTrue(drivetrain.runOnce(() -> drivetrain.tareEverything()));
		// xDrive.povRight().onTrue(drivetrain.runOnce(() -> drivetrain.seedFieldRelative(new Pose2d(5, 5, Rotation2d.fromDegrees(20)))));

		mIntake.setDefaultCommand(new BeltFeed());

		xDrive.rightBumper().whileTrue(new DeployAndIntake());

		/* TODO: Install limelights then test */
		// xDrive.leftBumper().whileTrue(
		// new Align(() -> xDrive.getLeftX(), () -> xDrive.getLeftY(), () ->
		// xDrive.getRightTriggerAxis(), true));
		// xDrive.rightBumper().whileTrue(
		// new Align(() -> xDrive.getLeftX(), () -> xDrive.getLeftY(), () ->
		// xDrive.getRightTriggerAxis(), false));

	}

	private void configureManipBinds() {
		new Trigger(() -> Math.abs(xManip.getRightY()) > Constants.OperatorConstants.kArmDeadzone)
				.whileTrue(new FineAdjust(() -> -xManip.getRightY()));

		xManip.y().whileTrue(new AimAndShoot(0));
		xManip.x().whileTrue(new AimAndShoot(Constants.ArmConstants.SetPoints.kSubwoofer));

		xManip.a().whileTrue(new AimAndShoot());
		xManip.b().whileTrue(new AimAndShoot(Constants.ArmConstants.SetPoints.kAmp));
		// xManip.a().onTrue(new SetPoint(0));
		// xManip.b().onTrue(new SetPoint(Constants.ArmConstants.SetPoints.kAmp));

		
		xManip.pov(90).whileTrue(new Spit());
		xManip.povDown().whileTrue(new DeployAndIntake());

		xManip.leftBumper().whileTrue(new FeedAndShoot(() -> xManip.getHID().getRightBumper()));
	}

	public RobotContainer() {
		logger = new Telemetry();
		field = Robot.mField;
		drivetrain.registerTelemetry((telemetry) -> logger.telemeterize(telemetry));
		configureDriverBinds();
		configureManipBinds();

		if (Robot.isSimulation()) {
			configureSimBinds();
		}
	}

	private void configureSimBinds() {
		drivetrain.removeDefaultCommand();
		mArm.removeDefaultCommand();

		drivetrain.setDefaultCommand(new xDrive(() -> simController.getRawAxis(0), () -> simController.getRawAxis(1),
				() -> simController.getRawAxis(2), () -> 0d));

		new Trigger(() -> simController.getRawButtonPressed(1))
				.whileTrue(new SetPoint(Constants.ArmConstants.SetPoints.kSubwoofer));

		new Trigger(() -> simController.getRawButtonPressed(2))
				.whileTrue(new SetPoint(Constants.ArmConstants.SetPoints.kAmp));

		new Trigger(() -> simController.getRawButtonPressed(3))
				.whileTrue(new AimAndShoot());

		// new Trigger(() -> simController.getRawButtonPressed(4))
		// .whileTrue(new SetPoint(Constants.ArmConstants.SetPoints.kHorizontal));

		// new Trigger(() -> simController.getRawButtonPressed(4)).onTrue(new
		// InstantCommand(() -> mArm.calculateArmSetpoint(), mArm));

		// new Trigger(() -> simController.getRawButtonPressed(2))
		// .onTrue(new InstantCommand(() -> drivetrain.seedFieldRelative()));

		// new Trigger(() -> simController.getRawButtonPressed(3))
		// .onTrue(new
		// SetPoint(Constants.ArmConstants.SetPoints.kSpeakerClosestPoint).withTimeout(1));

		// new Trigger(() -> simController.getRawButtonPressed(3)).onTrue(new
		// SetPoint(0).withTimeout(1));

		// new Trigger(() -> simController.getRawButtonPressed(3))
		// .whileTrue(drivetrain.applyRequest(() ->
		// forwardStraight.withVelocityX(1).withVelocityY(0)));

		// mArm.setDefaultCommand(new FineAdjust(() -> -simController.getRawAxis(2)));

		// new Trigger(() -> simController.getRawButtonPressed(4)).onTrue(new
		// InstantCommand(Intake.toggleDeploy));
	}
}
