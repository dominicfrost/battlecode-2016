package davesbot;

import battlecode.common.*;

public class SOLDIER {
    public static RobotController rc;
    public static RobotInfo[] nearbyRobots;
    public static MapLocation myLocation;
    public static MapLocation rallyLocation;
    public static Signal[] signals;

    public static void doTurn() throws GameActionException {
        nearbyRobots = rc.senseNearbyRobots(RobotPlayer.rt.sensorRadiusSquared);
        myLocation = rc.getLocation();
        signals = Utils.getArchonSignals();
        rallyLocation = Utils.readRallyLocation(myLocation, signals);

        if (Utils.attackGoalIfPossible(signals)) return;

        for (RobotInfo r: nearbyRobots) {
            if (rc.canAttackLocation(r.location)) {
                rc.attackLocation(r.location);
                return;
            }
        }


        MapLocation goal = Utils.readRallyLocation(myLocation, signals);
        Direction toMove = Utils.dirToLeastDamage(nearbyRobots, myLocation, myLocation.directionTo(goal));
        if (toMove != Direction.NONE) {
            rc.move(toMove);
        }
    }

    public static void execute(RobotController _rc) {
        rc = _rc;
        while (true) {
            try {
                if (rc.isCoreReady()) {
                    doTurn();
                }
                Clock.yield();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }
}