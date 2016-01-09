package team291;

import battlecode.common.*;
import battlecode.common.Clock;

import java.time.*;
import java.util.ArrayDeque;


public class SCOUT {

    public static RobotController rc;
    public static RobotInfo[] nearbyRobots;
    public static MapLocation myLocation;
    public static Signal[] signals;
    public static ArrayDeque<Signal> scoutSignals;
    public static int archonCount;
    public static int momsId; // id of archon who spawned me
    private static boolean isCoreReady;

    // scout state
    public static ScoutState state = ScoutState.NONE;
    public static MapLocation rallyPoint;
    public static MapLocation home; // where i came from
    public static MapLocation goal; // location of aoi i found
    public static Utils.MessageType broadcastLandMark;
    public static Direction myRandomDir = Direction.NORTH;

    public static enum ScoutState {
        NONE,
        READY_TO_START,
        SEARCHING_FOR_ALLYS,
        REPORTING_RALLY_LOCATION,
        SEARCHING_FOR_AOI,
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
                state = ScoutState.READY_TO_START;
                break;
            case READY_TO_START:
                determineInitialState();
                break;
            case SEARCHING_FOR_ALLYS:
                searchForAllyScouts();
                break;
            case REPORTING_RALLY_LOCATION:
                reportRallyLocation();
                break;
            case SEARCHING_FOR_AOI:
                searchForAOIs();
                break;
            case REPORTING_AOI:
                reportAOI();
                break;
        }
    }

    // sets state to either:
    // SEARCHING_FOR_ALLYS - if i receive an archon id in my signal queue i know i'm an initial scout
    // SEARCHING_FOR_AOI   - if i dont' receive an archon id in my signal queue i know i'm not an initial scout
    private static void determineInitialState() throws GameActionException {
        home = myLocation;

        Signal closestSignal = null;
        double closest = 999999;
        double dist;
        int[] msg;
        int senderId = -1;

        // get the archon count if it exists. it only will for initial scouts built
        for (Signal s: scoutSignals) {
            msg = s.getMessage();
            dist = myLocation.distanceSquaredTo(s.getLocation());

            if (msg[0] == Utils.MessageType.ARCHON_COUNT.ordinal() && dist < closest) {
                closest = dist;
                closestSignal = s;
                senderId = s.getRobotID();
            }
        }

        if (closestSignal != null) {
            // there was an archon count message was found, hence i'm one of our initial scouts
            archonCount = closestSignal.getMessage()[1];
            momsId = closestSignal.getRobotID();
            state = ScoutState.SEARCHING_FOR_ALLYS;
            rc.broadcastMessageSignal(Utils.MessageType.ARCHON_COUNT_CONFIRMED.ordinal(), senderId, RobotPlayer.maxSignalRange);
            if (isCoreReady) Utils.moveInDirToLeastDamage(nearbyRobots, myLocation, Direction.NORTH_EAST);
            return;
        }

        // -1 means no archon count message was found, hence i'm not one of our initial scouts
        state = ScoutState.SEARCHING_FOR_AOI;
        searchForAOIs();
    }

    // if i finds all of the initial scouts
    private static void searchForAllyScouts() throws GameActionException {
        int lookingForAllyScoutOrdinal = Utils.MessageType.LOOKING_FOR_ALLY_SCOUT.ordinal();
        rc.broadcastMessageSignal(lookingForAllyScoutOrdinal, Utils.serializeMapLocation(home), RobotPlayer.maxSignalRange);

        if (scoutSignals.size() >= archonCount - 1 && scoutsCanSeeEachother()) { // should be == in theory but there may be a case where it takes along time for one to join the group
            int[] idMsg;
            int robotId = RobotPlayer.id;
            rallyPoint = home;

            for (Signal s: scoutSignals) {
                idMsg = s.getMessage();
                if (idMsg[0] == lookingForAllyScoutOrdinal && s.getRobotID() < robotId) {
                    rallyPoint = Utils.deserializeMapLocation(idMsg[1]);
                    robotId = s.getRobotID();
                }
            }

            state = ScoutState.REPORTING_RALLY_LOCATION;
            return;
        }

        // if i see any other scouts follow them
//        for (RobotInfo r: nearbyRobots) {
//            if (myLocation.distanceSquaredTo(r.location) > 40) {
//                if (isCoreReady) Utils.moveInDirToLeastDamage(nearbyRobots, myLocation, myLocation.directionTo(r.location));
//                return;
//            }
//        }

        if (rc.isCoreReady()) Utils.moveInDirToLeastDamage(nearbyRobots, myLocation, Direction.NORTH_EAST);
    }

    private static void reportRallyLocation() throws GameActionException {

        for (RobotInfo r: nearbyRobots) {
            if (r.ID == momsId) {
                rc.broadcastMessageSignal(Utils.MessageType.RALLY_LOCATION_REPORT.ordinal(), Utils.serializeMapLocation(rallyPoint), RobotPlayer.maxSignalRange);
                state = ScoutState.SEARCHING_FOR_AOI;
                return;
            }
        }

        if (rc.isCoreReady()) Utils.moveInDirToLeastDamage(nearbyRobots, myLocation, myLocation.directionTo(home));
    }


    private static void searchForAOIs() throws GameActionException {
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

        // if i hit a wally change my random dir
        if (myLocation.distanceSquaredTo(rallyPoint) > maxAOIDistance || !rc.onTheMap(myLocation.add(myRandomDir))) {
            myRandomDir = RobotPlayer.directions[Math.abs(RobotPlayer.rand.nextInt()) %8];
        }

        if (isCoreReady) Utils.moveInDirToLeastDamage(nearbyRobots, myLocation, myRandomDir);
    }

    public static void reportAOI() throws GameActionException {
        for (RobotInfo r: nearbyRobots) {
            if (r.team != RobotPlayer.myTeam && r.type == RobotType.ARCHON) {
                rc.broadcastMessageSignal(Utils.MessageType.AOI_CONFIRMED.ordinal(), Utils.serializeMapLocation(goal), RobotPlayer.maxSignalRange);
                state = ScoutState.SEARCHING_FOR_AOI;
                return;
            }
        }

        if (isCoreReady) Utils.moveInDirToLeastDamage(nearbyRobots, myLocation, myLocation.directionTo(rallyPoint));
    }

    public static boolean scoutsCanSeeEachother() throws GameActionException {
        for (Signal s: scoutSignals) {
            for (Signal t: scoutSignals) { // can be optimized
                if (s.getLocation().distanceSquaredTo(t.getLocation()) > RobotPlayer.rt.sensorRadiusSquared) {
                    return false;
                }
            }
        }
        return true;
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