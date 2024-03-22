package frc.robot;

import com.pathplanner.lib.util.GeometryUtil;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.wpilibj.GenericHID;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.GenericHID.RumbleType;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.ConditionalCommand;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.RepeatCommand;
import edu.wpi.first.wpilibj2.command.RunCommand;
import edu.wpi.first.wpilibj2.command.WaitCommand;
import edu.wpi.first.wpilibj2.command.WaitUntilCommand;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import frc.robot.Constants.Lights.LEDState;
import frc.robot.commands.ShootCommandFactory;
import frc.robot.commands.Spit;
import frc.robot.commands.ArmCommands.FineAdjust;
import frc.robot.commands.ArmCommands.SetPoint;
import frc.robot.commands.ClimberCommands.AlrightTranslate;
import frc.robot.commands.ClimberCommands.Extend;
import frc.robot.commands.ClimberCommands.HomeClimber;
import frc.robot.commands.FeederCommands.BeltDrive;
import frc.robot.commands.FeederCommands.PassToShooter;
import frc.robot.commands.FeederCommands.QuickFeed;
import frc.robot.commands.IntakeCommands.DeployAndIntake;
import frc.robot.commands.IntakeCommands.IntakeMultiple;
import frc.robot.commands.ShooterCommands.AimAndShoot;
import frc.robot.commands.ShooterCommands.CancelShooter;
import frc.robot.commands.ShooterCommands.Feed;
import frc.robot.commands.ShooterCommands.FeedAndShoot;
import frc.robot.commands.ShooterCommands.PoopOut;
import frc.robot.commands.ShooterCommands.Prepare;
import frc.robot.commands.ShooterCommands.Shoot;
import frc.robot.commands.Swerve.Align;
import frc.robot.commands.Swerve.PathToPoint;
import frc.robot.commands.Swerve.xDrive;
import frc.robot.subsystems.Arm;
import frc.robot.subsystems.Climber;
import frc.robot.subsystems.Intake;
import frc.robot.subsystems.Lighting;
import frc.robot.subsystems.Music;
import frc.robot.subsystems.Shooter;
import frc.robot.subsystems.Drivetrain.Drivetrain;

@SuppressWarnings("unused")
public class RobotContainer {
	public static final CommandXboxController xDrive = new CommandXboxController(0);
	public static final CommandXboxController xManip = new CommandXboxController(1);
	private static final GenericHID simController = new GenericHID(3);

	private final Drivetrain drivetrain = Drivetrain.getInstance();
	private final Arm mArm = Arm.getInstance();
	private final Shooter mShooter = Shooter.getInstance();
	private final Intake mIntake = Intake.getInstance();
	private final Climber mClimber = Climber.getInstance();
	private final Music mMusic = Music.getInstance();
	private final Lighting mLighting = Lighting.getInstance();

	private final Telemetry logger;
	public Field2d field;

	public void configureDefaultBinds() {
		removeDefaultCommands();

		mClimber.setDefaultCommand(mClimber.run(mClimber.stopClimber).withName("Climber Default (no moving)"));
		mLighting.setDefaultCommand(
			mLighting.runOnce(() -> {
				if (mLighting.getTimeExpired() && !mLighting.getAutoMode()) {
					mLighting.autoSetLights(true);
				}
			})
		);

		mShooter.setDefaultCommand(new CancelShooter());

		if (Robot.isSimulation()) {
			drivetrain
					.setDefaultCommand(new xDrive(() -> simController.getRawAxis(0), () -> simController.getRawAxis(1),
							() -> simController.getRawAxis(2), () -> 0d));
		} else {
			drivetrain.setDefaultCommand(
					new xDrive(xDrive::getLeftX, xDrive::getLeftY, xDrive::getRightX,
							xDrive::getRightTriggerAxis).ignoringDisable(true));
		}
	}

	public void removeDefaultCommands() {
		Intake.getInstance().removeDefaultCommand();
		Shooter.getInstance().removeDefaultCommand();
		Arm.getInstance().removeDefaultCommand();
		Drivetrain.getInstance().removeDefaultCommand();
		Climber.getInstance().removeDefaultCommand();
		Lighting.getInstance().removeDefaultCommand();
	}

	private void configureDriverBinds() {
		new Trigger(() -> Timer.getMatchTime() > 25 && Timer.getMatchTime() < 30)
				.onTrue(new InstantCommand(() -> xDrive.getHID().setRumble(RumbleType.kBothRumble, 1))
						.finallyDo(() -> xDrive.getHID().setRumble(RumbleType.kBothRumble, 0)));

		xDrive.a().toggleOnTrue(new Align(xDrive::getLeftX, xDrive::getLeftY,
				xDrive::getRightTriggerAxis, false));
		xDrive.b().toggleOnTrue(new DeployAndIntake(true).deadlineWith(new Align(xDrive::getLeftX, xDrive::getLeftY,
				xDrive::getRightTriggerAxis, true)).andThen(Lighting.getStrobeCommand(LEDState.kRed)));
				
		// xDrive.b().toggleOnTrue(new Align(xDrive::getLeftX, xDrive::getLeftY,
		// 		xDrive::getRightTriggerAxis, true));

		xDrive.x().toggleOnTrue(ShootCommandFactory.getAimAndShootCommand());
		xDrive.y().toggleOnTrue(new PathToPoint(Constants.AutoConstants.WayPoints.Blue.StartingNotes.center)
		);
				// .alongWith(ShootCommandFactory.getAmpCommandWithWaitUntil()));

		xDrive.povDown().onTrue(drivetrain.runOnce(drivetrain::seedFieldRelative).withName("Seed Field Relative"));
		// Square up to the speaker and press this to reset odometry to the speaker
		xDrive.povRight().onTrue(drivetrain
				.runOnce(() -> drivetrain
						.seedFieldRelative(!Robot.isRed() ? Constants.AutoConstants.WayPoints.Blue.CenterStartPosition
								: GeometryUtil
										.flipFieldPose(Constants.AutoConstants.WayPoints.Blue.CenterStartPosition)))
				.withName("Zero Swerve 2 Speaker"));

		xDrive.povLeft().onTrue(new IntakeMultiple().alongWith(new Feed()));

		xDrive.leftStick().toggleOnTrue(new DeployAndIntake(false).andThen(Lighting.getStrobeCommand(LEDState.kRed)));
		xDrive.rightStick().toggleOnTrue(new DeployAndIntake(true).andThen(Lighting.getStrobeCommand(LEDState.kRed)));
	}

	private void configureManipBinds() {
		new Trigger(() -> Math.abs(xManip.getRightY()) > Constants.OperatorConstants.kManipDeadzone)
				.whileTrue(new FineAdjust(() -> -xManip.getRightY()));

		new Trigger(() -> Math.abs(xManip.getLeftY()) > Constants.OperatorConstants.kManipDeadzone)
				.whileTrue(new BeltDrive(() -> -xManip.getLeftY()));

		new Trigger(() -> Timer.getMatchTime() > 25).and(() -> Timer.getMatchTime() < 30)
				.whileTrue(new RunCommand(() -> {
					xManip.getHID().setRumble(RumbleType.kBothRumble, 1);
					xDrive.getHID().setRumble(RumbleType.kBothRumble, 1);
				})
						.finallyDo(() -> {
							xManip.getHID().setRumble(RumbleType.kBothRumble, 0);
							xDrive.getHID().setRumble(RumbleType.kBothRumble, 0);
						}));

		xManip.y().onTrue(HomeClimber.getHomingCommand());
		xManip.x().whileTrue(new QuickFeed());
		xManip.a().toggleOnTrue(ShootCommandFactory.getAimAndShootCommandWithWaitUntil());
		xManip.b().toggleOnTrue(ShootCommandFactory.getAmpCommandWithWaitUntil());

		xManip.rightStick().toggleOnTrue(new Extend());
		xManip.leftStick()
				.toggleOnTrue(new AlrightTranslate(() -> -Constants.ClimberConstants.LeftMotor.kRetractPower,
						() -> -Constants.ClimberConstants.RightMotor.kRetractPower));

		xManip.povUp().toggleOnTrue(ShootCommandFactory.getPrepareAndShootCommand());
		xManip.povRight().toggleOnTrue(ShootCommandFactory.getContinuousShootCommand());
		xManip.povDown().whileTrue(new Spit());
		xManip.povLeft()
				.toggleOnTrue(new PassToShooter().andThen(
						new SetPoint(Constants.ArmConstants.SetPoints.kCenterToWingPass).alongWith(new Prepare()),
						new Shoot()));

		// make this the command for shooting cross map
		// xManip.povLeft().toggleOnTrue(new DeployAndIntake(true));

		xManip.rightBumper().whileTrue(ShootCommandFactory.getPrepareAndShootCommandWithWaitUntil());
		// left bumper is the universal shoot button

		// absolute worst case scenario
		xManip.start().and(() -> xManip.back().getAsBoolean())
				.onTrue(mArm.runOnce(mArm::toggleArmMotorLimits));
	}

	public RobotContainer() {
		logger = new Telemetry();
		field = Robot.teleopField;
		drivetrain.registerTelemetry((telemetry) -> logger.telemeterize(telemetry));
		configureDriverBinds();
		configureManipBinds();
		configureDefaultBinds();

		if (Robot.isSimulation()) {
			configureSimBinds();
		}
	}

	private void configureSimBinds() {
		new Trigger(() -> simController.getRawButtonPressed(1))
				.toggleOnTrue(new Align(() -> simController.getRawAxis(0), () -> simController.getRawAxis(1), null,
						false));

		new Trigger(() -> simController.getRawButtonPressed(2))
				.whileTrue(new SetPoint());

		new Trigger(() -> simController.getRawButtonPressed(3))
				.toggleOnTrue(new PathToPoint(Constants.AutoConstants.WayPoints.Blue.kAmp));

		new Trigger(() -> simController.getRawButtonPressed(3))
				.toggleOnTrue(new PathToPoint(Constants.AutoConstants.WayPoints.Blue.kSpeakerRight));

		// new Trigger(() -> simController.getRawButtonPressed(3))
		// .whileTrue(new AimAndShoot());

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
