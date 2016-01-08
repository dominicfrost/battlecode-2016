package team291;

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
        if (!goal.equals(EMPTY_MAP_LOC)) {
            Direction toMove = Utils.dirToLeastDamage(nearbyRobots, myLocation, myLocation.directionTo(goal));
            if (toMove != Direction.NONE) {
                // if i can sense the robot and its my archon move away if too close, else move to goal
                if (rc.canSenseLocation(goal) && rc.senseRobotAtLocation(goal).team == RobotPlayer.myTeam) {
                    if (myLocation.distanceSquaredTo(goal) >= 9) {
                        rc.move(toMove);
                        return;
                    } else {
                        rc.move(toMove.opposite());
                        return;
                    }
                } else {
                    rc.move(toMove);
                    return;
                }
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