package davesbot;

import battlecode.common.*;

import java.util.Random;

public class RobotPlayer {
    public static Direction[] directions;
    public static RobotType[] robotTypes;
    public static Random rand;
    public static Team myTeam;
    public static Team enemyTeam;
    public static RobotType rt;
    public static int maxSignalRange;
    public static RobotController rc;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController _rc) {
        // You can instantiate variables here.
        Direction[] _directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
                Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
        RobotType[] _robotTypes = {RobotType.SCOUT, RobotType.SOLDIER, RobotType.ARCHON, RobotType.TTM,
                RobotType.GUARD, RobotType.VIPER, RobotType.TURRET};

        rc = _rc;
        directions = _directions;
        robotTypes = _robotTypes;

        rand = new Random(rc.getID());
        myTeam = rc.getTeam();
        enemyTeam = myTeam.opponent();
        rt = rc.getType();
        maxSignalRange = rt.sensorRadiusSquared * 2 - 1;


        switch (rt) {
            case SCOUT:
                SCOUT.execute(rc);
                break;
            case VIPER:
                VIPER.execute(rc);
                break;
            case ARCHON:
                ARCHON.execute(rc);
                break;
            case SOLDIER:
                SOLDIER.execute(rc);
                break;
            case TURRET:
                TURRET.execute(rc);
                break;
            case TTM:
                TTM.execute(rc);
                break;
        }
    }
}
