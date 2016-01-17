package team291;

import battlecode.common.*;
import battlecode.common.Clock;

import java.util.ArrayDeque;


public class SCOUT {

    public static RobotController rc;
    public static RobotInfo[] nearbyAllies;
    public static RobotInfo[] nearbyEnemies;

    public static MapLocation myLocation;
    public static Signal[] signals;
    public static ArrayDeque<Signal> scoutSignals;
    private static boolean isCoreReady;

    // scout state
    public static ScoutState state = ScoutState.NONE;
    public static MapLocation rallyPoint;
    public static MapLocation goal; // location of aoi i found
    public static Utils.MessageType broadcastLandMark;
    public static Direction myRandomDir = Direction.NORTH;

    public static int circlingDir;

    public static enum ScoutState {
        NONE,
        SEARCHING_FOR_AOI,
        RESETTING_SEARCH_DIRECTION,
        REPORTING_AOI
    }

    private static int maxAOIDistance = 200;


    private static void doTurn() throws GameActionException {
        isCoreReady = rc.isCoreReady();
        myLocation = rc.getLocation();

        nearbyAllies = rc.senseNearbyRobots(RobotPlayer.rt.sensorRadiusSquared, RobotPlayer.myTeam);
        nearbyEnemies = rc.senseHostileRobots(myLocation, RobotPlayer.rt.sensorRadiusSquared);

        signals = rc.emptySignalQueue();
        scoutSignals = Utils.getScoutSignals(signals);

        switch (state) {
            case NONE:
                circlingDir = getRandomCirclingDir();
                rallyPoint = Utils.getRallyLocation();
                state = ScoutState.SEARCHING_FOR_AOI;
                searchForAOIs();
                break;
            case SEARCHING_FOR_AOI:
                if (isCoreReady && flee()) return;
                searchForAOIs();
                break;
            case REPORTING_AOI:
                if (isCoreReady && flee()) return;
                reportAOI();
                break;
        }
    }

    private static boolean flee() throws GameActionException {
        if (Utils.shouldFlee(nearbyEnemies, myLocation)) {
            Direction dirToAllies = myLocation.directionTo(rallyPoint);
            if (Utils.moveInDirToLeastDamage(nearbyEnemies, myLocation, dirToAllies)) return true;
        }

        return false;
    }



    private static void searchForAOIs() throws GameActionException {

        //TODO : decide fi we should not broadcast when enemy robots close to aoi
        // find turret attack locations, this may be too inefficient
        double dstSqr;
        for (RobotInfo r: nearbyAllies) {
            if (r.type == RobotType.TURRET) {
                for (RobotInfo enemy: nearbyEnemies) {
                    if (enemy.coreDelay >= 1) {
                        dstSqr = r.location.distanceSquaredTo(enemy.location);
                        if (dstSqr <= RobotType.TURRET.attackRadiusSquared && dstSqr > RobotType.TURRET.sensorRadiusSquared) {
                            broadcastLandMark = Utils.MessageType.TURRET_TARGET;
                            goal = enemy.location;
                            state = ScoutState.REPORTING_AOI;
                            reportAOI();
                            return;
                        }
                    }
                }
            }
        }


//        for (RobotInfo r: enemyRobots) {
//            if (r.type == RobotType.ZOMBIEDEN) {
//                broadcastLandMark = Utils.MessageType.DEN;
//                goal = r.location;
//                state = ScoutState.REPORTING_AOI;
//                reportAOI();
//                return;
//            }
//        }

        MapLocation[] partLocations = rc.sensePartLocations(RobotPlayer.rt.sensorRadiusSquared);
        for (MapLocation m: partLocations) {
            if (rc.senseRubble(m) < 100) {
                broadcastLandMark = Utils.MessageType.PART_LOCATION;
                goal = m;
                state = ScoutState.REPORTING_AOI;
                reportAOI();
                return;
            }
        }

        RobotInfo[] neutrals = rc.senseNearbyRobots(RobotPlayer.rt.sensorRadiusSquared, Team.NEUTRAL);
        if (neutrals.length > 0) {
            broadcastLandMark = Utils.MessageType.NEUTRAL_ROBOT_LOCATION;
            goal = neutrals[0].location;
            state = ScoutState.REPORTING_AOI;
            reportAOI();
            return;
        }

        if (isCoreReady) circle();
    }

    public static void reportAOI() throws GameActionException {
        if (isCoreReady && myLocation.equals(goal)) {
            Utils.moveInDirToLeastDamage(nearbyEnemies, myLocation, myLocation.directionTo(rallyPoint));
            return;
        }

        for (RobotInfo r: nearbyAllies) {
            if (r.type == RobotType.ARCHON) {
                rc.broadcastMessageSignal(broadcastLandMark.ordinal(), Utils.serializeMapLocation(goal), RobotPlayer.maxSignalRange);
                state = ScoutState.SEARCHING_FOR_AOI;
                return;
            }
        }
        if (isCoreReady) Utils.moveInDirToLeastDamage(nearbyEnemies, myLocation, myLocation.directionTo(rallyPoint));
    }

    public static boolean circle() throws GameActionException {
        MapLocation next = myLocation.add(RobotPlayer.directions[circlingDir]);
        if (next.distanceSquaredTo(rallyPoint) <= RobotPlayer.rt.sensorRadiusSquared && rc.onTheMap(next)) {
            if (Utils.moveInDirToLeastDamage(nearbyEnemies, myLocation, RobotPlayer.directions[circlingDir])) return true;
        }

        next = myLocation.add(RobotPlayer.directions[(circlingDir + 1) % 8]);
        if (next.distanceSquaredTo(rallyPoint) <= RobotPlayer.rt.sensorRadiusSquared && rc.onTheMap(next)) {
            if (Utils.moveInDirToLeastDamage(nearbyEnemies, myLocation, RobotPlayer.directions[(circlingDir + 1) % 8])) return true;
        }

        next = myLocation.add(RobotPlayer.directions[(circlingDir + 2) % 8]);
        if (next.distanceSquaredTo(rallyPoint) <= RobotPlayer.rt.sensorRadiusSquared && rc.onTheMap(next)) {
            circlingDir = (circlingDir + 2) % 8;
            if (Utils.moveInDirToLeastDamage(nearbyEnemies, myLocation, RobotPlayer.directions[circlingDir])) return true;
        }

        return Utils.moveInDirToLeastDamage(nearbyEnemies, myLocation, myLocation.directionTo(rallyPoint));
    }

    public static int getRandomCirclingDir() {
        int d = Math.abs(RobotPlayer.rand.nextInt()) % 8;
        if (d % 2 == 1) {
            d--;
        }

        return d;
    }

    public static void execute() {
        rc = RobotPlayer.rc;
        while (true) {
            try {
                doTurn();
                Clock.yield();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }
}