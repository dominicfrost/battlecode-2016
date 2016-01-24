package team291;

import battlecode.common.*;

import java.util.ArrayDeque;

public class TURRET {
    public static RobotController rc;
    public static RobotInfo[] nearbyEnemies;
    public static MapLocation myLocation;
    public static ArrayDeque<Signal> signals;
    public static MapLocation rallyPoint;

    public static boolean doTurn() throws GameActionException {
        myLocation = rc.getLocation();
        nearbyEnemies = rc.senseHostileRobots(myLocation, RobotPlayer.rt.sensorRadiusSquared);

        signals = Utils.getScoutSignals(rc.emptySignalQueue());

        if (attackTurretLocations()) return false;
        if (attackAnythingClose()) return false;
        if (findBetterLocation()) return true;
        return false;
    }

    public static boolean attackTurretLocations() throws GameActionException {
        int[] msg;
        MapLocation enemyLoc;
        for (Signal s: signals) {
            msg = s.getMessage();
            if (msg[0] == Utils.MessageType.TURRET_TARGET.ordinal()) {
                enemyLoc = Utils.deserializeMapLocation(msg[1]);
                if (rc.canAttackLocation(enemyLoc)) {
                    rc.attackLocation(enemyLoc);
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean attackAnythingClose() throws GameActionException {
        for (RobotInfo r: nearbyEnemies) {
            if (Utils.attack(r.location)) return true;
        }

        return false;
    }

    public static boolean findBetterLocation() throws GameActionException {
        MapLocation toMove = Utils.findBetterLocation(myLocation, rallyPoint);
        if (toMove != null) {
          rc.pack();
          return true;
        }

        return false;
    }

    public static void execute() {
        rc = RobotPlayer.rc;
        rallyPoint = Utils.getRallyLocation();
        while (true) {
            try {
                if (rc.isCoreReady()) {
                    if (doTurn()) break;
                } else {
                    rc.emptySignalQueue();
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
            Clock.yield();
        }
        Clock.yield();
        TTM.execute();
    }
}
