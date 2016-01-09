package team291;

import battlecode.common.*;

import java.util.ArrayDeque;


public class SCOUT {

    public static RobotController rc;
    public static RobotInfo[] nearbyRobots;
    public static MapLocation myLocation;
    public static Signal[] signals;
    public static ArrayDeque<Signal> scoutSignals;
    public static int archonCount;
    public static int momsId; // id of archon who spawned me

    // scout state
    public static ScoutState state = ScoutState.NONE;
    public static MapLocation rallyPoint;
    public static MapLocation home; // where i came from
    public static MapLocation goal; // location of aoi i found
    public static Utils.MessageType broadcastLandMark;

    public static enum ScoutState {
        NONE,
        SEARCHING_FOR_ALLYS,
        REPORTING_RALLY_LOCATION,
        SEARCHING_FOR_AOI,
        GATHERING_SUPPORT
    }


    private static void doTurn() throws GameActionException {
        nearbyRobots = rc.senseNearbyRobots(RobotPlayer.rt.sensorRadiusSquared);
        myLocation = rc.getLocation();
        signals = rc.emptySignalQueue();
        scoutSignals = Utils.getScoutSignals(signals);

        switch (state) {
            case NONE:
                determineInitialState();
                break;
            case SEARCHING_FOR_ALLYS:
                searchForAllyScouts();
                break;
            case REPORTING_RALLY_LOCATION:
                reportRallyLocation();
            case SEARCHING_FOR_AOI:
                searchForAOIs();
                break;
            case GATHERING_SUPPORT:
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

        // get the archon count if it exists. it only will for initial scouts built
        for (Signal s: scoutSignals) {
            msg = s.getMessage();
            dist = myLocation.distanceSquaredTo(s.getLocation());

            if (msg[0] == Utils.MessageType.ARCHON_COUNT.ordinal() && dist < closest) {
                closest = dist;
                closestSignal = s;
            }
        }

        if (closestSignal != null) {
            // there was an archon count message was found, hence i'm one of our initial scouts
            archonCount = closestSignal.getMessage()[1];
            momsId = closestSignal.getID();
            state = ScoutState.SEARCHING_FOR_ALLYS;
            Utils.dirToLeastDamage(nearbyRobots, myLocation, Direction.NORTH_EAST);
            return;
        }

        // -1 means no archon count message was found, hence i'm not one of our initial scouts
        state = ScoutState.SEARCHING_FOR_AOI;
        searchForAOIs();
    }

    // if i finds all of the initial scouts
    private static void searchForAllyScouts() throws GameActionException {
        int lookingForAllyScoutOrdinal = Utils.MessageType.LOOKING_FOR_ALLY_SCOUT.ordinal();
        rc.broadcastMessageSignal(lookingForAllyScoutOrdinal, serializeMapLocation(home), RobotPlayer.maxSignalRange);
        rc.broadcastMessageSignal(lookingForAllyScoutOrdinal, momsId, RobotPlayer.maxSignalRange);

        if (scoutSignals.size() >= archonCount * 2) { // should be == in theory but there may be a case where it takes along time for one to join the group
            int id = momsId;
            rallyPoint = home;
            int[] idMsg;
            int[] locationMsg;
            Signal[] scoutSigArr = (Signal[]) scoutSignals.toArray(); // cast to array because arraydeques suck

            // lower id wins to be our rally point for now...
            for (int i = scoutSigArr.length - 1; i > -1; i--) {
                idMsg = scoutSigArr[i].getMessage();
                if (idMsg[0] == lookingForAllyScoutOrdinal && idMsg[1] < id) {
                    id = idMsg[1];
                    locationMsg = scoutSigArr[i-1].getMessage();
                    rallyPoint = deserializeMapLocation(locationMsg[1]);
                }
                i--; // there will be an idmsg and location msg coupled so subtract 1 again
            }


            Utils.dirToLeastDamage(nearbyRobots, myLocation, myLocation.directionTo(rallyPoint));
            state = ScoutState.REPORTING_RALLY_LOCATION;
            return;
        }

        Utils.dirToLeastDamage(nearbyRobots, myLocation, Direction.NORTH_EAST);
    }

    private static void reportRallyLocation() throws GameActionException {
        for (Signal s: scoutSignals) {
            // if its a rally point confirmed msg from mom
            if (s.getID() == momsId && s.getMessage()[0] == Utils.MessageType.RALLY_POINT_CONFIRMED.ordinal()) {
                // mom got the rally location, go search for new stuff! new home is rally point
                state = ScoutState.SEARCHING_FOR_AOI;
                home = rallyPoint;
                searchForAOIs();
                return;
            }
        }
        rc.broadcastMessageSignal(Utils.MessageType.RALLY_LOCATION_REPORT.ordinal(), serializeMapLocation(rallyPoint), RobotPlayer.maxSignalRange);
        Utils.dirToLeastDamage(nearbyRobots, myLocation, myLocation.directionTo(home));
    }


    private static void searchForAOIs() throws GameActionException {
        MapLocation[] sensableLocations = Utils.getSensableLocations(myLocation);
        for (MapLocation m: sensableLocations) {
            if (rc.senseParts(m) > 0) {
                broadcastLandMark = Utils.MessageType.PART_LOCATION;
                goal = m;
                state = ScoutState.GATHERING_SUPPORT;
                reportAOI();
                return;
            }
        }

        for (RobotInfo r: nearbyRobots) {
            if (r.team == Team.NEUTRAL) {
                broadcastLandMark = Utils.MessageType.NEUTRAL_ROBOT_LOCATION;
                goal = r.location;
                state = ScoutState.GATHERING_SUPPORT;
                reportAOI();
                return;
            }
        }
    }

    public static void reportAOI() throws GameActionException {
        for (Signal s: scoutSignals) {
            if (s.getMessage()[0] == Utils.MessageType.AOI_CONFIRMED.ordinal()) {

                // reported! go find another
                state = ScoutState.SEARCHING_FOR_AOI;
                searchForAOIs();
                return;
            }
        }

        rc.broadcastMessageSignal(broadcastLandMark.ordinal(), serializeMapLocation(goal), RobotPlayer.maxSignalRange);
        Utils.dirToLeastDamage(nearbyRobots, myLocation, myLocation.directionTo(home));
    }

    public static void execute() {
        rc = RobotPlayer.rc;
        while (true) {
            try {
                if (rc.isCoreReady()) {
                    doTurn();
                }
                Clock.yield();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }
}