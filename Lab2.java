import java.util.Random;
import lejos.nxt.Button;
import lejos.nxt.LCD;
import lejos.nxt.Sound;
import lejos.util.Delay;
import lejos.nxt.LightSensor;
import lejos.nxt.TouchSensor;
import lejos.nxt.UltrasonicSensor;
import lejos.nxt.SoundSensor;
import lejos.nxt.SensorPort;
import lejos.robotics.RegulatedMotor;
import lejos.robotics.navigation.DifferentialPilot;
import lejos.robotics.navigation.RotateMoveController;
import lejos.robotics.subsumption.Arbitrator;
import lejos.robotics.subsumption.Behavior;
import lejos.util.PilotProps;


public class Lab2 {

  public static void main (String[] aArg) throws Exception {

    PilotProps pp = new PilotProps();
    pp.loadPersistentValues();

    final Random random = new Random();

    final int soundThreshold = 30;

    // Motors
    final RegulatedMotor leftMotor = PilotProps.getMotor(pp.getProperty(PilotProps.KEY_LEFTMOTOR, "C"));
    final RegulatedMotor rightMotor = PilotProps.getMotor(pp.getProperty(PilotProps.KEY_RIGHTMOTOR, "A"));
    // boolean reverse = Boolean.parseBoolean(pp.getProperty(PilotProps.KEY_REVERSE,"false"));
    // Wheel calibration values
    float wheelDiameter = Float.parseFloat(pp.getProperty(PilotProps.KEY_WHEELDIAMETER, "2"));
    float trackWidth = Float.parseFloat(pp.getProperty(PilotProps.KEY_TRACKWIDTH, "6.25"));

    // Pilot
    final DifferentialPilot pilot = new DifferentialPilot(wheelDiameter, trackWidth, leftMotor, rightMotor);

    // Light sensor
    final LightSensor light = new LightSensor(SensorPort.S1, false);

    // Ultrasonic sensor
    // final UltrasonicSensor ultrasonicSensor = new UltrasonicSensor(SensorPort.S1);

    // Sound sensor
    final SoundSensor sound = new SoundSensor(SensorPort.S4);

    // Touch sensors
    final TouchSensor[] touch = new TouchSensor[2];
    touch[0] = new TouchSensor(SensorPort.S2);
    touch[1] = new TouchSensor(SensorPort.S3);

    pilot.setRotateSpeed(180);

    // LEAST IMPORTANT BEHAVIOR
    // EXPLORE ------------------------------------------------------------------------------------
    Behavior Explore = new Behavior() {
      private boolean suppressed = false;
      private int[] lightIntensities = new int[3];

      public boolean takeControl() {
        return true;
      }

      public void action() {
        LCD.drawString("EXPLORING", 0, 0);
        // LCD.drawString("light: " + light.readValue(), 0, 1);

        lightIntensities[0] = light.readValue();

        pilot.rotate(45);
        while(pilot.isMoving() && !suppressed) {
          Thread.yield();
        }

        lightIntensities[1] = light.readValue();

        pilot.rotate(-90);
        while(pilot.isMoving() && !suppressed) {
          Thread.yield();
        }

        lightIntensities[2] = light.readValue();

        if (lightIntensities[0] <= lightIntensities[1] && lightIntensities[0] <= lightIntensities[2]) {
          LCD.drawString("CENTER", 0, 1);
          pilot.rotate(45);

        } else {
          if (lightIntensities[1] <= lightIntensities[0] && lightIntensities[1] <= lightIntensities[2]) {
            LCD.drawString("LEFT", 0, 1);
            pilot.rotate(90);
          } else {
            LCD.drawString("RIGHT", 0, 1);
          }
        }

        while(pilot.isMoving() && !suppressed) {
          Thread.yield();
        }

        pilot.travel(5, true);
        while(pilot.isMoving() && !suppressed) {
          Thread.yield();
        }
        Delay.msDelay(1000);
        LCD.clear();
      }

      public void suppress() {
        suppressed = true;
      }
    };

    // SECOND MOST IMPORTANT BEHAVIOR
    // RUN FROM SOUND -----------------------------------------------------------------------------
    Behavior RunFromSound = new Behavior(){
      private boolean suppressed = false;

      // This behavior should be triggered if the sound value exceds the soundThreshold
      public boolean takeControl() {
        return sound.readValue() > soundThreshold;
      }

      // If this behavior stops, stop moving
      public void suppress() {
        suppressed = true;
      }

      public void action() {

          LCD.drawString("SCARED", 0, 0);
          LCD.drawString("Im scared of", 0, 1);
          LCD.drawString("sounds. ", 0, 2);
          LCD.drawString("S: " + sound.readValue(), 0, 3);

          // Beep since its a little ~bitch~ mousey mousey
          Sound.beepSequence();

          // Go back
          pilot.travel(-5 - random.nextInt(5));
          while(pilot.isMoving() && !suppressed) {
            Thread.yield();
          }

          // Get a random direction
          int angle = 60 + random.nextInt(60);
          int side = (Math.random() > 0.5 ? -1 : 1);

          // Rotate
          pilot.rotate(-side * angle);

          while(pilot.isMoving() && !suppressed) {
            Thread.yield();
          }
      
        LCD.clear();
      }
    };

    // MOST IMPORTANT BEHAVIOR
    // HIT WALL -----------------------------------------------------------------------------------
    Behavior HitWall = new Behavior() {
      private boolean suppressed = false;
      private int degrees;

      public boolean takeControl() {
        return touch[0].isPressed() || touch[1].isPressed();
      }

      public void action() {
        LCD.drawString("I HIT A WALL!", 0, 0);
        degrees = random.nextInt(90) + 135;

        suppressed = false;
        pilot.travel(-7.5);
        pilot.rotate(degrees);
        pilot.travel(10, true);

        while (pilot.isMoving() && !suppressed) {
          Thread.yield();
        }
        LCD.clear();
      }

      public void suppress() {
        suppressed = true;
      }
    };

    // DEPRECATED
    // DRIVE --------------------------------------------------------------------------------------
    Behavior Drive = new Behavior() {
      public boolean takeControl() {
        return sound.readValue() <= soundThreshold;
      }

      public void suppress() {
        pilot.stop();
      }

      public void action() {
        LCD.drawString("Status: DRIVE", 0, 0);
        LCD.drawString("" + sound.readValue(), 0, 1);

        pilot.forward();
        while(sound.readValue() <= soundThreshold)
          Thread.yield(); //action complete when not on line
      }
    };

    // DEPRECATED
    // MOVE BACK ----------------------------------------------------------------------------------
    Behavior MoveBack = new Behavior(){
      private boolean suppress = false;

      public boolean takeControl() {
        return sound.readValue() > soundThreshold;
      }

      public void suppress() {
        suppress = true;
      }

      public void action() {
        LCD.drawString("Status: BACK", 0, 0);
        LCD.drawString("" + sound.readValue(), 0, 1);

        while (!suppress) {
          pilot.backward();
          while (!suppress && pilot.isMoving())
            Thread.yield();
        }
        pilot.stop();
        suppress = false;
      }
    };

    // DEPRECATED
    // RUN AWAY FROM SOUND ------------------------------------------------------------------------
    Behavior RunAway = new Behavior(){
      private boolean suppress = false;

      public boolean takeControl() {
        return sound.readValue() > soundThreshold;
      }

      public void suppress() {
        suppress = true;
      }

      public void action() {
        LCD.drawString("Status: SCARED", 0, 0);
        LCD.drawString("Im scared of", 0, 1);
        LCD.drawString("sounds. ", 0, 2);

        while (!suppress) {
          pilot.backward();
          while (!suppress && pilot.isMoving())
            Thread.yield();
        }
        pilot.stop();
        suppress = false;
      }
    };

    Behavior[] behaviors = {Explore, RunFromSound, HitWall};
    Button.waitForAnyPress();
    (new Arbitrator(behaviors)).start();
  }
}
