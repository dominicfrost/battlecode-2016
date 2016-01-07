package davesbot;

import battlecode.common.*;

public class TURRET {
    public static RobotController rc;
    public static RobotInfo[] nearbyRobots;
    public static MapLocation myLocation;
    public static Signal[] signals;

    public static void doTurn() throws GameActionException {
        nearbyRobots = rc.senseNearbyRobots(RobotPlayer.rt.sensorRadiusSquared);
        myLocation = rc.getLocation();
        signals = Utils.getArchonSignals();

        if (Utils.attackGoalIfPossible(signals)) return;

        for (RobotInfo r: nearbyRobots) {
            if (r.team != RobotPlayer.myTeam && r.team != Team.NEUTRAL && Utils.attack(r.location)) return;
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