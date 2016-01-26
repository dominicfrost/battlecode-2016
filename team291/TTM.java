package team291;

import battlecode.common.*;

public class TTM {

    public static RobotController rc;
    public static MapLocation goal, rallyPoint, myLocation;


    public static boolean doTurn() throws GameActionException {
        myLocation = rc.getLocation();
        goal = Utils.findBetterLocation(myLocation, rallyPoint);
        if (goal != null) {
            Direction dirToGoal = myLocation.directionTo(goal);
            if (rc.canMove(dirToGoal)) {
                if (!rc.isCoreReady()) return false;
                rc.move(dirToGoal);
                goal = Utils.findBetterLocation(myLocation, rallyPoint);
                if (goal != null) return false;
            }
        }
        rc.unpack();
        return true;
    }

    public static void execute() {
        rc = RobotPlayer.rc;
        rallyPoint = Utils.getRallyLocation();
        while (true) {
            try {
                if (doTurn()) break;
                rc.emptySignalQueue();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
            Clock.yield();
        }
        Clock.yield();
        TURRET.execute();
    }
}
