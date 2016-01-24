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

    public static boolean circlingCW = true;
    public static int circlingDir;

    public static enum ScoutState {
        NONE,
        SEARCHING_FOR_AOI
    }

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
        int broadcastCount = 0;
        //TODO : decide fi we should not broadcast when enemy robots close to aoi
        // find turret attack locations, this may be too inefficient
        double dstSqr;
        for (RobotInfo r : nearbyAllies) {
            if (r.type == RobotType.TURRET) {
                for (RobotInfo enemy : nearbyEnemies) {
                    if (enemy.type == RobotType.ZOMBIEDEN || enemy.coreDelay >= 1) {
                        dstSqr = r.location.distanceSquaredTo(enemy.location);
                        if (dstSqr <= RobotType.TURRET.attackRadiusSquared && dstSqr > RobotType.TURRET.sensorRadiusSquared) {
                            broadcastLandMark = Utils.MessageType.TURRET_TARGET;
                            goal = enemy.location;
                            rc.broadcastMessageSignal(broadcastLandMark.ordinal(), Utils.serializeMapLocation(goal), RobotPlayer.maxSignalRange);
                            broadcastCount++;
                            if (broadcastCount == 20) {
                                circle();
                                return;
                            }
                        }
                    }
                }
            }
        }


        for (RobotInfo r : nearbyEnemies) {
            if (r.type == RobotType.ZOMBIEDEN) {
                broadcastLandMark = Utils.MessageType.DEN;
                goal = r.location;
                rc.broadcastMessageSignal(broadcastLandMark.ordinal(), Utils.serializeMapLocation(goal), RobotPlayer.maxSignalRange);
                broadcastCount++;
                if (broadcastCount == 20) {
                    circle();
                    return;
                }
            }
        }

        MapLocation[] partLocations = rc.sensePartLocations(RobotPlayer.rt.sensorRadiusSquared);
        for (MapLocation m : partLocations) {
            if (rc.senseRubble(m) < 100) {
                broadcastLandMark = Utils.MessageType.PART_LOCATION;
                goal = m;
                rc.broadcastMessageSignal(broadcastLandMark.ordinal(), Utils.serializeMapLocation(goal), RobotPlayer.maxSignalRange);
                broadcastCount++;
                if (broadcastCount == 20) {
                    circle();
                    return;
                }
            }
        }

        RobotInfo[] neutrals = rc.senseNearbyRobots(RobotPlayer.rt.sensorRadiusSquared, Team.NEUTRAL);
        if (neutrals.length > 0) {
            broadcastLandMark = Utils.MessageType.NEUTRAL_ROBOT_LOCATION;
            goal = neutrals[0].location;
            rc.broadcastMessageSignal(broadcastLandMark.ordinal(), Utils.serializeMapLocation(goal), RobotPlayer.maxSignalRange);
            broadcastCount++;
            if (broadcastCount == 20) {
                circle();
                return;
            }
        }

        circle();
    }

    public static boolean circle() throws GameActionException {
        // hackz
        int reverseDir = circlingCW ? (circlingDir - 1 + 8) % 8 : (circlingDir + 1) % 8;
        MapLocation next = myLocation.add(RobotPlayer.directions[reverseDir]);
        RobotInfo botAtNext = rc.senseRobotAtLocation(next);
        if (next.distanceSquaredTo(rallyPoint) <= RobotPlayer.rt.sensorRadiusSquared) {
            if (!rc.onTheMap(next) || (botAtNext != null && botAtNext.type == RobotType.SCOUT)) {
                circlingDir = RobotPlayer.directions[circlingDir].opposite().ordinal();
                next = myLocation.add(RobotPlayer.directions[circlingDir]);
                circlingCW = !circlingCW;
            } else {
                if (rc.isCoreReady() && Utils.moveInDirToLeastDamage(nearbyEnemies, myLocation, RobotPlayer.directions[reverseDir]))
                    return true;
                return false;
            }
        }

        next = myLocation.add(RobotPlayer.directions[circlingDir]);
        botAtNext = rc.senseRobotAtLocation(next);
        if (!rc.onTheMap(next) || (botAtNext != null && botAtNext.type == RobotType.SCOUT)) {
            circlingDir = RobotPlayer.directions[circlingDir].opposite().ordinal();
            next = myLocation.add(RobotPlayer.directions[circlingDir]);
            circlingCW = !circlingCW;
        }

        if (next.distanceSquaredTo(rallyPoint) <= RobotPlayer.rt.sensorRadiusSquared && rc.onTheMap(next)) {
            if (rc.isCoreReady() && Utils.moveInDirToLeastDamage(nearbyEnemies, myLocation, RobotPlayer.directions[circlingDir]))
                return true;
            return false;
        }
        int nextDir = circlingCW ? (circlingDir + 1) % 8 : (circlingDir - 1 + 8) % 8;
        next = myLocation.add(RobotPlayer.directions[nextDir]);
        botAtNext = rc.senseRobotAtLocation(next);
        if (!rc.onTheMap(next) || (botAtNext != null && botAtNext.type == RobotType.SCOUT)) {
            circlingDir = RobotPlayer.directions[circlingDir].opposite().ordinal();
            circlingCW = !circlingCW;
            return false;
        }

        if (next.distanceSquaredTo(rallyPoint) <= RobotPlayer.rt.sensorRadiusSquared && rc.onTheMap(next)) {
            if (rc.isCoreReady() && Utils.moveInDirToLeastDamage(nearbyEnemies, myLocation, RobotPlayer.directions[nextDir]))
                return true;
            return false;
        }

        nextDir = circlingCW ? (circlingDir + 2) % 8 : (circlingDir - 2 + 8) % 8;
        next = myLocation.add(RobotPlayer.directions[nextDir]);
        botAtNext = rc.senseRobotAtLocation(next);
        if (!rc.onTheMap(next) || (botAtNext != null && botAtNext.type == RobotType.SCOUT)) {
            circlingDir = RobotPlayer.directions[circlingDir].opposite().ordinal();
            circlingCW = !circlingCW;
            return false;
        }

        if (next.distanceSquaredTo(rallyPoint) <= RobotPlayer.rt.sensorRadiusSquared && rc.onTheMap(next)) {
            circlingDir = nextDir;
            if (rc.isCoreReady() && Utils.moveInDirToLeastDamage(nearbyEnemies, myLocation, RobotPlayer.directions[circlingDir]))
                return true;
            return false;
        }

        return rc.isCoreReady() && Utils.moveInDirToLeastDamage(nearbyEnemies, myLocation, myLocation.directionTo(rallyPoint));
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
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
            Clock.yield();
        }
    }
}
