import battlecode.common.*;

import java.util.Random;

public class RobotPlayer {

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) {
        // You can instantiate variables here.
        Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
                Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
        RobotType[] robotTypes = {RobotType.SCOUT, RobotType.SOLDIER, RobotType.ARCHON, RobotType.TTM,
                RobotType.GUARD, RobotType.VIPER, RobotType.TURRET};

        Random rand = new Random(rc.getID());
        Team myTeam = rc.getTeam();
        Team enemyTeam = myTeam.opponent();

        RobotType rt = rc.getType();

        switch (rt) {
            case RobotType.SCOUT:
                SCOUT.execute(rc);
                break;
            case RobotType.VIPER:
                SCOUT.execute(rc);
                break;
            case RobotType.ARCHON:
                SCOUT.execute(rc);
                break;
            case RobotType.SOLDIER:
                SCOUT.execute(rc);
                break;
            case RobotType.TURRENT:
                SCOUT.execute(rc);
                break;
            case RobotType.TTM:
                SCOUT.execute(rc);
                break;
        }
    }
}
