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
    public static MapLocation[] sigLocs;
    private static boolean isCoreReady;

    // scout state
    public static ScoutState state = ScoutState.NONE;
    public static MapLocation rallyPoint;
    public static MapLocation goal; // location of aoi i found
    public static Utils.MessageType broadcastLandMark;

    public static Circle circler;

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

        sigLocs = new MapLocation[scoutSignals.size()];
        int i  = 0;
        for (Signal s: scoutSignals) {
            sigLocs[i] = Utils.deserializeMapLocation(s.getMessage()[1]);
            i++;
        }


        switch (state) {
            case NONE:
                rallyPoint = Utils.getRallyLocation();
                state = ScoutState.SEARCHING_FOR_AOI;
                circler = new Circle(rallyPoint, RobotPlayer.rt.sensorRadiusSquared);
                searchForAOIs();
                break;
            case SEARCHING_FOR_AOI:
                if (isCoreReady) flee();
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

    private static boolean sigLocsContains(MapLocation m) {
        for (MapLocation s: sigLocs) {
            if (s.equals(m)) return true;
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
                        if (dstSqr <= RobotType.TURRET.attackRadiusSquared && dstSqr > RobotType.TURRET.sensorRadiusSquared && !sigLocsContains(enemy.location)) {
                            broadcastLandMark = Utils.MessageType.TURRET_TARGET;
                            goal = enemy.location;
                            rc.broadcastMessageSignal(broadcastLandMark.ordinal(), Utils.serializeMapLocation(goal), RobotPlayer.maxSignalRange);
                            broadcastCount++;
                            if (broadcastCount == 10) {
                                circle();
                                return;
                            }
                        }
                    }
                }
            }
        }

        for (RobotInfo r : nearbyEnemies) {
            if (r.type != RobotType.ZOMBIEDEN && !sigLocsContains(r.location)) {
                broadcastLandMark = Utils.MessageType.ENEMY;
                goal = r.location;
                rc.broadcastMessageSignal(broadcastLandMark.ordinal(), Utils.serializeMapLocation(goal), RobotPlayer.maxSignalRange);
                broadcastCount++;
                if (broadcastCount == 10) {
                    circle();
                    return;
                }
            }
        }

        RobotInfo[] neutrals = rc.senseNearbyRobots(RobotPlayer.rt.sensorRadiusSquared, Team.NEUTRAL);
        if (neutrals.length > 0) {
            broadcastLandMark = Utils.MessageType.NEUTRAL_ROBOT_LOCATION;
            for (RobotInfo neutral: neutrals) {
                if (!Utils.isSurrounded(neutral.location)&& !sigLocsContains(neutral.location)) {
                    goal = neutral.location;
                    rc.broadcastMessageSignal(broadcastLandMark.ordinal(), Utils.serializeMapLocation(goal), RobotPlayer.maxSignalRange);
                    broadcastCount++;
                    if (broadcastCount == 10) {
                        circle();
                        return;
                    }
                }
            }
        }

        for (RobotInfo r : nearbyEnemies) {
            if (r.type == RobotType.ZOMBIEDEN && !sigLocsContains(r.location)) {
                broadcastLandMark = Utils.MessageType.DEN;
                goal = r.location;
                rc.broadcastMessageSignal(broadcastLandMark.ordinal(), Utils.serializeMapLocation(goal), RobotPlayer.maxSignalRange);
                broadcastCount++;
                if (broadcastCount == 10) {
                    circle();
                    return;
                }
            }
        }

        MapLocation[] partLocations = rc.sensePartLocations(RobotPlayer.rt.sensorRadiusSquared);
        for (MapLocation m : partLocations) {
            if (rc.senseRubble(m) < 100 && !Utils.isSurrounded(m) && !rc.isLocationOccupied(m) && !sigLocsContains(m)) {
                broadcastLandMark = Utils.MessageType.PART_LOCATION;
                goal = m;
                rc.broadcastMessageSignal(broadcastLandMark.ordinal(), Utils.serializeMapLocation(goal), RobotPlayer.maxSignalRange);
                broadcastCount++;
                if (broadcastCount == 10) {
                    circle();
                    return;
                }
            }
        }

        circle();
    }

    public static boolean circle() throws GameActionException {
        if (!rc.isCoreReady()) return false;
        circler.setCircleRadius(Math.max(Utils.distanceSquaredToPerimeter() + 9, RobotPlayer.rt.sensorRadiusSquared));
        return circler.circle(nearbyEnemies, myLocation);
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
