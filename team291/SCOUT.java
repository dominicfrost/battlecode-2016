//package team291;
//
//import battlecode.common.*;
//
//import java.awt.*;
//
//public class SCOUT {
//
//    public static RobotController rc;
//    public static RobotInfo[] nearbyRobots;
//    public static MapLocation myLocation;
//
//    // scout state
//    public static ScoutState state = ScoutState.SEARCHING;
//    public static MapLocation home;
//    public static MapLocation goal;
//    public static LandMark broadcastLandMark = LandMark.NONE;
//    public static Direction searchForAllysDir = Direction.NORTH;
//
//    public static enum ScoutState {
//        SEARCHING,
//        GATHERING_SUPPORT,
//        RETURNING_TO_AOI,
//        SEARCHING_FOR_ALLYS,
//        NONE
//    }
//
//    public static enum LandMark {
//        ENEMY_FORCES,
//        NEUTRAL_BOTS,
//        PARTS,
//        DEN,
//        ALLIED_ARCHON,
//        NONE
//    }
//
//
//
//    public static void doTurn() throws GameActionException {
//        nearbyRobots = rc.senseNearbyRobots(RobotPlayer.rt.sensorRadiusSquared);
//        myLocation = rc.getLocation();
//
//        switch (state) {
//            case NONE:
//                home = myLocation;
//                state = ScoutState.SEARCHING;
//                search();
//                break;
//            case SEARCHING:
//                search();
//                break;
//            case GATHERING_SUPPORT:
//                gatherSupport();
//                break;
//            case RETURNING_TO_AOI:
//                leadToAOI();
//                break;
//            case SEARCHING_FOR_ALLYS:
//                searchForAllys();
//                break;
//        }
//    }
//
//    public static void search() throws GameActionException {
//        MapLocation[] sensableLocations = Utils.getSensableLocations(myLocation);
//        for (MapLocation m: sensableLocations) {
//            if (rc.senseParts(m) > 0) {
//                broadcastLandMark = LandMark.PARTS;
//                goal = m;
//                state = ScoutState.GATHERING_SUPPORT;
//                gatherSupport();
//                return;
//            }
//        }
//
//        for (RobotInfo r: nearbyRobots) {
//            if (r.team == RobotPlayer.myTeam && r.type) {
//                broadcastLandMark = LandMark.ALLIED_ARCHON;
//                goal = r.location;
//                state = ScoutState.GATHERING_SUPPORT;
//                gatherSupport();
//                return;
//            }
//            if (r.team == RobotPlayer.enemyTeam) {
//                broadcastLandMark = LandMark.NEUTRAL_BOTS;
//                goal = r.location;
//                state = ScoutState.GATHERING_SUPPORT;
//                gatherSupport();
//                return;
//            }
//            if (r.team == Team.NEUTRAL) {
//                broadcastLandMark = LandMark.ENEMY_FORCES;
//                goal = r.location;
//                state = ScoutState.GATHERING_SUPPORT;
//                gatherSupport();
//                return;
//            }
//            //update home if i sense teammates??
//        }
//    }
//
//    public static void gatherSupport() throws GameActionException {
//        for (RobotInfo r: nearbyRobots) {
//            if (r.team == RobotPlayer.myTeam && r.type == RobotType.ARCHON && myLocation.distanceSquaredTo(r.location) < r.type.senseRadiusSquared - 2) {
//                rc.broadcastMessageSignal(broadcastLandMark.ordinal(), 0, RobotPlayer.maxSignalRange);
//                state = ScoutState.RETURNING_TO_AOI;
//                return;
//            }
//        }
//
//        Utils.dirToLeastDamage(nearbyRobots, myLocation, myLocation.directionTo(home));
//    }
//
//    public static void leadToAOI() throws GameActionException {
//        // what triggers a state change?
//        switch(broadcastLandMark) {
//            case NEUTRAL_BOTS:
//                if (waitForActivation()) return;
//                break;
//            case PARTS:
//                if (waitForPartsToBeTaken()) return;
//                break;
//            case ALLIED_ARCHON:
//                if (peace()) return;
//                break;
//        }
//
//
//        // only move back to goal if ally can sense me
//        for (RobotInfo r: nearbyRobots) {
//            if (r.team == RobotPlayer.myTeam && r.type == RobotType.ARCHON && myLocation.distanceSquaredTo(r.location) < r.type.senseRadiusSquared - 2) {
//                rc.broadcastMessageSignal(broadcastLandMark.ordinal(), 0, RobotPlayer.maxSignalRange);
//                Utils.dirToLeastDamage(nearbyRobots, myLocation, myLocation.directionTo(goal));
//                return;
//            }
//        }
//
//        // if you lost the dude you leading gather support
//        state = ScoutState.GATHERING_SUPPORT;
//        gatherSupport();
//    }
//
//    public static void searchForAllys() throws GameActionException {
//        for (RobotInfo r: nearbyRobots) {
//            if (r.team == RobotPlayer.myTeam && r.type == RobotType.ARCHON) {
//                if (myLocation.distanceSquaredTo(r.location) < r.type.senseRadiusSquared - 2) {
//                    rc.broadcastMessageSignal(broadcastLandMark.ordinal(), 0, RobotPlayer.maxSignalRange);
//                    state = ScoutState.RETURNING_TO_AOI;
//                    return;
//                } else {
//                    Utils.dirToLeastDamage(nearbyRobots, myLocation, myLocation.directionTo(r.location));
//                    return;
//                }
//            }
//        }
//
//        if (!rc.onTheMap(myLocation.add(searchForAllysDir))) {
//            searchForAllysDir = RobotPlayer.directions[(dirint+offsets[offsetIndex]+8)%8];
//        }
//
//        Utils.dirToLeastDamage(nearbyRobots, myLocation, myLocation.directionTo(goal));
//    }
//
//    public static void execute() {
//        rc = RobotPlayer.rc;
//        while (true) {
//            try {
//                if (rc.isCoreReady()) {
//                    doTurn();
//                }
//                Clock.yield();
//            } catch (Exception e) {
//                System.out.println(e.getMessage());
//                e.printStackTrace();
//            }
//        }
//    }
//}