package team291;

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

        if (Utils.attackGoalIfPossible(signals)) return;
        if (attackAnythingClose()) return;
        if (moveToRally()) return;
        if (mineAllTheThings()) return;
    }

    public static boolean attackAnythingClose() throws GameActionException {
        for (RobotInfo r: nearbyRobots) {
            if (r.team != RobotPlayer.myTeam && r.team != Team.NEUTRAL && Utils.attack(r.location)) return true;
        }

        return false;
    }

    public static boolean moveToRally() throws GameActionException {
        MapLocation goal = Utils.readRallyLocation(myLocation, signals);
        if (goal != null) {
            Direction toMove = Utils.dirToLeastDamage(nearbyRobots, myLocation, myLocation.directionTo(goal));
            if (toMove != Direction.NONE) {
                // if i can sense the robot and its my archon move away if too close, else move to goal
                if (rc.canSenseLocation(goal) && rc.senseRobotAtLocation(goal).team == RobotPlayer.myTeam) {
                    if (myLocation.distanceSquaredTo(goal) >= 9) {
                        rc.move(toMove);
                        return true;
                    } else {
                        if (Utils.tryMove(toMove.opposite())) return true;
                    }
                } else {
                    rc.move(toMove);
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean mineAllTheThings() throws GameActionException {
        Direction dirToRubs = Direction.NONE;
        double mostRubble = rc.senseRubble(myLocation);

        double rubbleAtLoc;
        for (Direction d: RobotPlayer.directions) {
            rubbleAtLoc = rc.senseRubble(myLocation.add(d));
            if (rubbleAtLoc > mostRubble) {
                mostRubble = rubbleAtLoc;
                dirToRubs = d;
            }
        }

        if (mostRubble != 0) {
            rc.clearRubble(dirToRubs);
            return true;
        }

        return false;
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