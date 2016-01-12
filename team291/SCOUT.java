package team291;

import battlecode.common.*;
import battlecode.common.Clock;

import java.util.ArrayDeque;


public class SCOUT {

    public static RobotController rc;
    public static RobotInfo[] nearbyRobots;
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

    public static enum ScoutState {
        NONE,
        SEARCHING_FOR_AOI,
        RESETTING_SEARCH_DIRECTION,
        REPORTING_AOI
    }

    private static int maxAOIDistance = 200;


    private static void doTurn() throws GameActionException {
        isCoreReady = rc.isCoreReady();
        nearbyRobots = rc.senseNearbyRobots(RobotPlayer.rt.sensorRadiusSquared);
        myLocation = rc.getLocation();
        signals = rc.emptySignalQueue();
        scoutSignals = Utils.getScoutSignals(signals);

        switch (state) {
            case NONE:
                rallyPoint = myLocation;
                state = ScoutState.SEARCHING_FOR_AOI;
                searchForAOIs();
                break;
            case SEARCHING_FOR_AOI:
                if (isCoreReady && flee()) {
                    state = ScoutState.RESETTING_SEARCH_DIRECTION;
                    return;
                }
                searchForAOIs();
                break;
            case RESETTING_SEARCH_DIRECTION:
                if (isCoreReady && flee()) return;
                resetSearchDir();
                break;
            case REPORTING_AOI:
                if (isCoreReady && flee()) return;
                reportAOI();
                break;
        }
    }

    private static boolean flee() throws GameActionException {
        if (Utils.shouldFlee(rc, nearbyRobots, myLocation)) {
            Direction dirToAllies = myLocation.directionTo(rallyPoint);
            if (Utils.moveInDirToLeastDamage(nearbyRobots, myLocation, dirToAllies)) return true;
        }

        return false;
    }



    private static void searchForAOIs() throws GameActionException {

        //TODO : decide fi we should not broadcast when enemy robots close to aoi

        for (RobotInfo r: nearbyRobots) {
            if (r.type == RobotType.ZOMBIEDEN) {
                broadcastLandMark = Utils.MessageType.DEN;
                goal = r.location;
                state = ScoutState.REPORTING_AOI;
                reportAOI();
                return;
            }
        }

        MapLocation[] sensableLocations = Utils.getSensableLocations(myLocation);
        for (MapLocation m: sensableLocations) {
            if (m == null) {
                break;
            }

            // TODO: decide if we should only do this if there is little rubble
            if (rc.senseParts(m) > 0 && rc.senseRubble(m) < 50) {
                broadcastLandMark = Utils.MessageType.PART_LOCATION;
                goal = m;
                state = ScoutState.REPORTING_AOI;
                reportAOI();
                return;
            }
        }

        for (RobotInfo r: nearbyRobots) {
            if (r.team == Team.NEUTRAL) {
                broadcastLandMark = Utils.MessageType.NEUTRAL_ROBOT_LOCATION;
                goal = r.location;
                state = ScoutState.REPORTING_AOI;
                reportAOI();
                return;
            }
        }


        // if i hit a wall change my random dir
        if (myLocation.distanceSquaredTo(rallyPoint) > maxAOIDistance || !rc.canMove(myRandomDir)) {
            state = ScoutState.RESETTING_SEARCH_DIRECTION;
            if (isCoreReady) Utils.moveInDirToLeastDamage(nearbyRobots, myLocation, myLocation.directionTo(rallyPoint));
            return;
        }

        if (isCoreReady) Utils.moveInDirToLeastDamage(nearbyRobots, myLocation, myRandomDir);
    }

    public static void resetSearchDir() throws GameActionException {
        if (myLocation.distanceSquaredTo(rallyPoint) < 15) {
            myRandomDir = RobotPlayer.directions[Math.abs(RobotPlayer.rand.nextInt()) % 8];
            state = ScoutState.SEARCHING_FOR_AOI;
            return;
        }

        if (isCoreReady) Utils.moveInDirToLeastDamage(nearbyRobots, myLocation, myLocation.directionTo(rallyPoint));
    }

    public static void reportAOI() throws GameActionException {
        if (isCoreReady && myLocation.equals(goal)) {
            Utils.moveInDirToLeastDamage(nearbyRobots, myLocation, myLocation.directionTo(rallyPoint));
            return;
        }

        for (RobotInfo r: nearbyRobots) {
            if (r.team == RobotPlayer.myTeam) {
                if (r.type == RobotType.ARCHON) {
                    rc.broadcastMessageSignal(broadcastLandMark.ordinal(), Utils.serializeMapLocation(goal), RobotPlayer.maxSignalRange);
                    state = ScoutState.SEARCHING_FOR_AOI;
                    return;
                }
            }
        }
        if (isCoreReady) Utils.moveInDirToLeastDamage(nearbyRobots, myLocation, myLocation.directionTo(rallyPoint));
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