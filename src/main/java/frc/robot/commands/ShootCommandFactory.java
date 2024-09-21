package frc.robot.commands;

import java.util.function.BooleanSupplier;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.WaitUntilCommand;
import frc.robot.Constants;
import frc.robot.Constants.Lights.LEDState;
import frc.robot.commands.ArmCommands.ReturnHome;
import frc.robot.commands.ArmCommands.SetPoint;
import frc.robot.commands.FeederCommands.BeltDrive;
import frc.robot.commands.FeederCommands.PassToShooter;
import frc.robot.commands.ShooterCommands.CancelShooter;
import frc.robot.commands.ShooterCommands.FullSend;
import frc.robot.commands.ShooterCommands.PoopOut;
import frc.robot.commands.ShooterCommands.Prepare;
import frc.robot.commands.ShooterCommands.Shoot;
import frc.robot.subsystems.Arm;
import frc.robot.subsystems.Intake;
import frc.robot.subsystems.Lighting;
import frc.robot.subsystems.Shooter;

public class ShootCommandFactory {

        /**
         *Shoots when the arm reaches it's setpoint
         *@see also used for shooting in teleop
        */
        public static Command getAimAndShootCommand() {
                return new PassToShooter().unless(() -> Intake.getInstance().getShooterSensor())
                                .andThen(new SetPoint()
                                                .raceWith(new WaitUntilCommand(() -> Arm.getInstance().atSetpoint())
                                                                .deadlineWith(new Prepare()).andThen(new Shoot(false))))
                                .withName("Auto Aim and Shoot");
        }

        /**
         * Shoots when the arm reaches it's setpoint and after all the timeouts
         *@see also used for shooting in auto
         */
        public static Command getAimAndShootCommandWithTimeouts() {
                return new PassToShooter().withTimeout(Constants.OperatorConstants.feedTimeout)
                                .unless(() -> Intake.getInstance().getShooterSensor())
                                .andThen(new SetPoint().raceWith(
                                                new WaitUntilCommand(() -> Arm.getInstance()
                                                                .atSetpoint())
                                                                .withTimeout(Constants.OperatorConstants.setpointTimeout)
                                                                .deadlineWith(new Prepare())
                                                                .andThen(new Shoot(false)
                                                                                .withTimeout(Constants.OperatorConstants.shootTimeout),
                                                                                new Shoot(true).onlyWhile(
                                                                                                () -> Intake.getInstance()
                                                                                                                .getShooterSensor())))
                                                .onlyIf(
                                                                () -> Intake.getInstance().getShooterSensor()))
                                .withName("Auto Aim and Shoot with Timeouts");
        }

        // works perfectly
        public static Command getAimAndShootCommandWithWaitUntil(BooleanSupplier waitUntil) {
                return new PassToShooter().unless(() -> Intake.getInstance().getShooterSensor())
                                .andThen(new SetPoint()
                                                .raceWith(new Prepare().until(waitUntil).andThen(new Shoot(false))),
                                                new CancelShooter().alongWith(new ReturnHome()))
                                .withName("Aim and Shoot");
        }

        // works perfectly
        public static Command getAmpCommand() {
                return new PassToShooter().unless(() -> Intake.getInstance().getShooterSensor())
                                .andThen(new SetPoint(Constants.ArmConstants.SetPoints.kAmp), new PoopOut())
                                .finallyDo(ReturnHome.ReturnHome)
                                .withName("Auto Amp Shot");
        }

        // works perfectly
        public static Command getAmpCommandWithWaitUntil(BooleanSupplier waitUntil) {
                return new PassToShooter().unless(() -> Intake.getInstance().getShooterSensor()).andThen(
                                new WaitUntilCommand(waitUntil)
                                                .deadlineWith(new SetPoint(Constants.ArmConstants.SetPoints.kAmp)),
                                new PoopOut()).finallyDo(ReturnHome.ReturnHome)
                                .withName("Amp Shot");
        }

        // works perfectly
        public static Command getPrepareAndShootCommand() {
                return new PassToShooter().unless(() -> Intake.getInstance().getShooterSensor()).andThen(
                                new Prepare().raceWith(new WaitUntilCommand(() -> Shooter.getInstance().readyToShoot()))
                                                .andThen(new Shoot(false)))
                                .withName("Auto Prepare and Shoot");
        }

        // works perfectly
        public static Command getPrepareAndShootCommandWithTimeouts() {
                return new PassToShooter().withTimeout(Constants.OperatorConstants.feedTimeout)
                                .unless(() -> Intake.getInstance().getShooterSensor())
                                .andThen(

                                                new Prepare().until(() -> Shooter.getInstance().readyToShoot())
                                                                .withTimeout(Constants.OperatorConstants.chargeUpTimeout)
                                                                .andThen(new Shoot(false).withTimeout(
                                                                                Constants.OperatorConstants.shootTimeout),
                                                                                new Shoot(true).onlyWhile(
                                                                                                () -> Intake.getInstance()
                                                                                                                .getShooterSensor())))
                                .withName("Auto Prepare and Shoot with Timeouts");
        }

        // works perfectly
        public static Command getPrepareAndShootCommandWithWaitUntil(BooleanSupplier waitUntil) {
                return new Prepare().until(waitUntil)
                                .andThen(new Shoot(true).onlyWhile(waitUntil))
                                .finallyDo(CancelShooter.CancelShooter)
                                .withName("Prepare and Shoot");
        }

        // works perfectly
        public static Command getRapidFireCommand() {
                return new Shoot(false)
                                .andThen(new BeltDrive(() -> Constants.IntakeConstants.beltIntakeSpeed)
                                                .alongWith(new FullSend()))
                                .finallyDo(CancelShooter.CancelShooter)
                                .withName("Auto Rapid Fire");
        }

        // works perfectly
        public static Command getRapidFireCommandWithWaitUntil(BooleanSupplier waitUntil) {
                return new WaitUntilCommand(waitUntil).deadlineWith(new Prepare())
                                // Prepare().until(waitUntil)
                                .andThen(new BeltDrive(() -> Constants.IntakeConstants.beltIntakeSpeed)
                                                .alongWith(new FullSend()).onlyWhile(waitUntil))
                                .finallyDo(CancelShooter.CancelShooter)
                                .withName("Rapid Fire");
        }

        
        public static Command getCenterToWingCommand(BooleanSupplier waitUntil) {
                return new PassToShooter().unless(() -> Intake.getInstance().getShooterSensor())
                                .andThen(new WaitUntilCommand(waitUntil).deadlineWith(
                                                new SetPoint(Constants.ArmConstants.SetPoints.kCenterToWingPass),
                                                new Prepare(0.60)).finallyDo(() ->Lighting.getStrobeCommand(() ->LEDState.kPurple)),
                                                new Shoot(true).onlyWhile(waitUntil))
                                .finallyDo(ReturnHome.ReturnHome)
                                .alongWith(Lighting.getStrobeCommand(() -> LEDState.kPurple))
                                .handleInterrupt(CancelShooter.CancelShooter)
                                .withName("Pass to Center");
        }
}