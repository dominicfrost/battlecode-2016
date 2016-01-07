package davesbot;

import battlecode.common.*;

public class SOLDIER {
    public static RobotController rc;
    public static RobotInfo[] nearbyRobots;
    public static MapLocation myLocation;
    public static MapLocation rallyLocation;
    public static Signal[] signals;
    public static MapLocation EMPTY_MAP_LOC = new MapLocation(0,0);

    public static void doTurn() throws GameActionException {
        nearbyRobots = rc.senseNearbyRobots(RobotPlayer.rt.sensorRadiusSquared);
        myLocation = rc.getLocation();
        signals = Utils.getArchonSignals();
        rallyLocation = Utils.readRallyLocation(myLocation, signals);

        if (Utils.attackGoalIfPossible(signals)) return;

        for (RobotInfo r: nearbyRobots) {
            if (r.team != RobotPlayer.myTeam && r.team != Team.NEUTRAL && Utils.attack(r.location)) return;
        }


        MapLocation goal = Utils.readRallyLocation(myLocation, signals);
        if (!goal.equals(EMPTY_MAP_LOC) && myLocation.distanceSquaredTo(goal) > 2) {
            Direction toMove = Utils.dirToLeastDamage(nearbyRobots, myLocation, myLocation.directionTo(goal));
            if (toMove != Direction.NONE) {
                rc.move(toMove);
                return;
            }
        }
    }

    public static void execute() {
        rc = RobotPlayer.rc;
        while (true) {
            try {
                if (rc.isCoreReady()) {
                    doTurn();
                } else {
                    rc.emptySignalQueue();
                }
                Clock.yield();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }
}