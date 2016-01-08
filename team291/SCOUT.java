package team291;

import battlecode.common.*;

public class SCOUT {

    public static RobotController rc;
    public static RobotInfo[] nearbyRobots;
    public static MapLocation myLocation;
    public static ScoutState state = ScoutState.SEARCHING;
    public static MapLocation home;
    public static MapLocation goal;

    public static LandMark broadcastLandMark = LandMark.NONE;

    public static enum ScoutState {
        SEARCHING,
        GATHERING_SUPPORT,
        NONE
    }

    public static enum LandMark {
        ENEMY_FORCES,
        NEUTRAL_BOTS,
        PARTS,
        NONE
    }



    public static void doTurn() throws GameActionException {
        nearbyRobots = rc.senseNearbyRobots(RobotPlayer.rt.sensorRadiusSquared);
        myLocation = rc.getLocation();

        switch (state) {
            case NONE:
                home = myLocation;
                state = ScoutState.SEARCHING;
                search();
                break;
            case SEARCHING:
                search();
                break;
            case GATHERING_SUPPORT:
                gatherSupport();
                break;
        }
    }

    public static void search() throws GameActionException {
        MapLocation[] sensableLocations = Utils.getSensableLocations(myLocation);
        for (MapLocation m: sensableLocations) {
            if (rc.senseParts(m) > 0) {
                broadcastLandMark = LandMark.PARTS;
                goal = m;
                state = ScoutState.GATHERING_SUPPORT;
                gatherSupport();
                return;
            }
        }

        for (RobotInfo r: nearbyRobots) {
            if (r.team == RobotPlayer.enemyTeam) {
                broadcastLandMark = LandMark.NEUTRAL_BOTS;
                goal = r.location;
                state = ScoutState.GATHERING_SUPPORT;
                gatherSupport();
                return;
            }
            if (r.team == Team.NEUTRAL) {
                broadcastLandMark = LandMark.ENEMY_FORCES;
                goal = r.location;
                state = ScoutState.GATHERING_SUPPORT;
                gatherSupport();
                return;
            }
            //update home if i sense teammates??
        }
    }

    public static void gatherSupport() throws GameActionException {
        Utils.dirToLeastDamage(nearbyRobots, myLocation, myLocation.directionTo(home));
//        rc.broadcastMessageSignal(broadcastLandMark.ordinal(), 0, RobotPlayer.maxSignalRange);
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