package team291;

import battlecode.common.*;
import javafx.scene.shape.Arc;

import java.awt.*;
import java.util.ArrayDeque;

public class ARCHON {
    public static RobotInfo[] nearbyRobots;
    public static MapLocation myLocation;
    public static ArrayDeque<Signal> signals;
    public static RobotController rc;
//    public static int separation = 15;
    private static boolean isCoreReady;

    private static int archonCount = -1;
    private static ArchonState state = ArchonState.NONE;
    private static MapLocation rallyPoint;
    private static MapLocation aoi;

    public static enum ArchonState {
        NONE,
        READY_TO_GIVE_ARCHON_COUNT,
        REPORTING_ARCHON_COUNT,
        STAYING_NEAR_INITIAL_LOCATION,
        MOVING_TO_RALLY,
        CHILLIN_AT_RALLY,
        REPORTING_TO_AOI,
        RETURING_TO_RALLY,
        HIDING_FROM_THE_ZOMBIE_SPAWN_LIKE_A_BITCH
    }

    public static void doTurn() throws GameActionException {
        isCoreReady = rc.isCoreReady();
        nearbyRobots = rc.senseNearbyRobots(RobotPlayer.rt.sensorRadiusSquared);
        myLocation = rc.getLocation();
        signals = Utils.getScoutSignals(rc.emptySignalQueue());

        switch (state) {
            case NONE:
                archonCount = rc.getRobotCount();
                state = ArchonState.READY_TO_GIVE_ARCHON_COUNT;
                reportArchonCount();
                break;
            case READY_TO_GIVE_ARCHON_COUNT:
                spawnInitialScout();
                break;
            case REPORTING_ARCHON_COUNT:
                reportArchonCount();
                break;
            case STAYING_NEAR_INITIAL_LOCATION:
                if (isCoreReady) {
                    if (flee()) return;
                    if (activate()) return;
                    if (moveToParts()) return; // may have to remove this if they stray too far from home
                }

                waitForRallyLocation();
                break;
            case MOVING_TO_RALLY:
                if (isCoreReady) {
                    if (flee()) return;
                    if (activate()) return;
                    if (moveToParts()) return;
                }

                returnToRally();
                break;
            case CHILLIN_AT_RALLY:
                if (isCoreReady) {
                    if (flee()) return;
                    if (repair()) return;
                    if (spawn()) return;
                    if (activate()) return;
                    if (moveToParts()) return;
                }

                chill();
                break;
            case REPORTING_TO_AOI:
                if (isCoreReady) {
                    if (flee()) return;
                    if (activate()) return;
                    if (repair()) return;
                    if (moveToParts()) return;
                }

                reportToAOI();
                break;
            case RETURING_TO_RALLY:
                if (isCoreReady) {
                    if (flee()) return;
                    if (activate()) return;
                    if (repair()) return;
                    if (moveToParts()) return;
                }

                returnToRally();
                break;
            case HIDING_FROM_THE_ZOMBIE_SPAWN_LIKE_A_BITCH:
                if (isCoreReady) {
                    if (flee()) return;
                    if (repair()) return;
                    Utils.moveInDirToLeastDamage(nearbyRobots, myLocation, myLocation.directionTo(rallyPoint));
                }
                break;
        }
    }

    public static boolean flee() throws GameActionException {
        if (Utils.shouldFlee(rc, nearbyRobots, myLocation)) {
            Direction toMove = Utils.flee(rc, nearbyRobots, myLocation);
            if (toMove != Direction.NONE) {
                rc.move(toMove);
                //System.out.println("flee");
                return true;
            }

            Direction dirToAllies = myLocation.directionTo(rallyPoint);
            toMove = Utils.dirToLeastDamage(nearbyRobots, myLocation, dirToAllies);
            if (toMove != Direction.NONE) {
                rc.move(toMove);
                //System.out.println("flee");
                return true;
            }
        }

        return false;
    }

    public static boolean repair() throws GameActionException {
        double lowestHealth = 1001;
        MapLocation lowestHealthLocation = null;

        for (RobotInfo r: nearbyRobots) {
            if (r.team == RobotPlayer.myTeam && myLocation.distanceSquaredTo(r.location) <= RobotPlayer.rt.attackRadiusSquared && r.health < lowestHealth && r.type != RobotType.ARCHON && r.health != r.type.maxHealth) {
                lowestHealthLocation = r.location;
                lowestHealth = r.health;
            }
        }

        if (lowestHealthLocation != null) {
            rc.repair(lowestHealthLocation);
            //System.out.println("repair");
            return true;
        }

        return false;
    }

    public static boolean spawn() throws GameActionException {
        if (rc.hasBuildRequirements(RobotType.TURRET)) {
            return spawnTurret();
        }

        return false;
    }

    public static boolean activate() throws GameActionException {
        for (RobotInfo r: nearbyRobots) {
            if (r.team == Team.NEUTRAL) {
                if (r.location.distanceSquaredTo(myLocation) < 2) {
                    rc.activate(r.location);
                    //System.out.println("activate");
                    return true;
                }
                Direction d = Utils.dirToLeastDamage(nearbyRobots, myLocation, myLocation.directionTo(r.location));
                if (d != Direction.NONE) {
                    rc.move(d);
                    //System.out.println("activate");
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean moveToParts() throws GameActionException {
        MapLocation[] sightRange = Utils.getSensableLocations(myLocation);
        for (MapLocation m: sightRange) {
            if (m == null) {
                return false;
            }
            if (rc.senseParts(m) != 0) {
                if (Utils.moveThrough(myLocation, m)) {
                    //System.out.println("moveToParts");
                    return true;
                }
            }
        }

        return false;
    }

    public static void spawnInitialScout() throws GameActionException {
        // rally location while waiting for real rally location is my spot where i spawned the scout
        rallyPoint = myLocation;
        if (trySpawn(Direction.NORTH, RobotType.SCOUT)) {
            state = ArchonState.REPORTING_ARCHON_COUNT;
            reportArchonCount();
            return;
        }

        randomMove();
    }

    public static void reportArchonCount() throws GameActionException {
        int[] msg;

        // When we get a response from the scout that it received our message switch state to STAYING_NEAR_INITIAL_LOCATION
        // TODO: decide if we want to delete this so we don't get stuck in this state when many archons spawn close
        for (Signal s: signals) {
            msg = s.getMessage();
            if (msg[0] == Utils.MessageType.ARCHON_COUNT_CONFIRMED.ordinal() && msg[1] == RobotPlayer.id) {
                state = ArchonState.STAYING_NEAR_INITIAL_LOCATION;
                return;
            }
        }

        rc.broadcastMessageSignal(Utils.MessageType.ARCHON_COUNT.ordinal(), archonCount, RobotPlayer.maxSignalRange);
    }

    public static void waitForRallyLocation() throws GameActionException {
        int[] msg;
        for (Signal s: signals) {
            msg = s.getMessage();
            if (msg[0] == Utils.MessageType.RALLY_LOCATION_REPORT.ordinal()) {
                rallyPoint = Utils.deserializeMapLocation(msg[1]);
                System.out.println("GOT MY GOAL " + rallyPoint);
                state = ArchonState.MOVING_TO_RALLY;
                if (isCoreReady) Utils.moveInDirToLeastDamage(nearbyRobots, myLocation, myLocation.directionTo(rallyPoint));
                return;
            }
        }

        if (myLocation == rallyPoint) return;
        if (isCoreReady) Utils.moveInDirToLeastDamage(nearbyRobots, myLocation, myLocation.directionTo(rallyPoint));
    }

    public static void returnToRally() throws GameActionException {
        if (myLocation.distanceSquaredTo(rallyPoint) < 9) {
            state = ArchonState.CHILLIN_AT_RALLY;
            chill();
            return;
        }

        if (isCoreReady) Utils.moveInDirToLeastDamage(nearbyRobots, myLocation, myLocation.directionTo(rallyPoint));
    }

    public static void chill() throws GameActionException {
        int[] msg;
        for (Signal s: signals) {
            msg = s.getMessage();
            if (msg[0] == Utils.MessageType.AOI_CONFIRMED.ordinal()) {
                aoi = Utils.deserializeMapLocation(msg[1]);
                state = ArchonState.REPORTING_TO_AOI;
                reportToAOI();
                return;
            }
        }
    }

    public static void reportToAOI() throws GameActionException {
        if (myLocation.distanceSquaredTo(aoi) < 2) {
            state = ArchonState.RETURING_TO_RALLY;
            returnToRally();
            return;
        }

        if (isCoreReady) Utils.moveInDirToLeastDamage(nearbyRobots, myLocation, myLocation.directionTo(aoi));
    }


    public static boolean randomMove() throws GameActionException {
        if (isCoreReady) {
            Direction d = Utils.dirToLeastDamage(nearbyRobots, myLocation, RobotPlayer.directions[Math.abs(RobotPlayer.rand.nextInt()) % RobotPlayer.directions.length]);
            if (d != Direction.NONE) {
                rc.move(d);
                return true;
            }
        }

        return false;
    }

    public static boolean spawnTurret() throws GameActionException {
        // don't need to check core because this should only be called from spawn!
        MapLocation potentialSpawnPoint;
        for (Direction d: RobotPlayer.directions) {
            potentialSpawnPoint = myLocation.add(d);
            if (((potentialSpawnPoint.x + potentialSpawnPoint.y) % 2) == 0 && rc.canBuild(d, RobotType.TURRET)) {
                rc.build(d, RobotType.TURRET);
                return true;
            }
        }

        if (randomMove()) return true;

        return false;
    }

    // This method will attempt to spawn in the given direction (or as close to it as possible)
    public static boolean trySpawn(Direction d, RobotType type) throws GameActionException {
        if (isCoreReady) {
            int offsetIndex = 0;
            int[] offsets = {0, 1, -1, 2, -2, 3, -3, 4};
            int dirint = Utils.directionToInt(d);
            while (offsetIndex < 8 && !rc.canBuild(RobotPlayer.directions[(dirint + offsets[offsetIndex] + 8) % 8], type)) {
                offsetIndex++;
            }
            if (offsetIndex < 8) {
                rc.build(RobotPlayer.directions[(dirint + offsets[offsetIndex] + 8) % 8], type);
                return true;
            }
        }

        return false;
    }


    public static void execute() {
        rc =  RobotPlayer.rc;
        while (true) {
            try {

                doTurn();

//                updateRallyLocation();
                Clock.yield();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
                Clock.yield();
            }
        }
    }
}

//
//    public static void updateRallyLocation() throws GameActionException {
//        double closest = 99999;
//        MapLocation closestLoc = null;
//        double diff;
//
//        for (RobotInfo r: nearbyRobots) {
//            if (r.team != RobotPlayer.myTeam && r.team != Team.NEUTRAL) {
//                diff = myLocation.distanceSquaredTo(r.location);
//                if  (diff < closest) {
//                    closestLoc = r.location;
//                    closest = diff;
//                }
//            }
//        }
//
//        if (closestLoc != null) {
//            rc.broadcastMessageSignal(closestLoc.x, closestLoc.y, RobotPlayer.maxSignalRange);
//            return;
//        }
//
//        for (RobotInfo r: nearbyRobots) {
//            if (r.type == RobotType.ZOMBIEDEN) {
//                rc.broadcastMessageSignal(r.location.x, r.location.y, RobotPlayer.maxSignalRange);
//                return;
//            }
//        }
//
//        rc.broadcastMessageSignal(myLocation.x, myLocation.y, RobotPlayer.maxSignalRange);
//    }
//
//    public static boolean spawnInDirection(RobotType type) throws GameActionException {
//        for (Direction d: RobotPlayer.directions) {
//            if (rc.canBuild(d, type)) {
//                rc.build(d, type);
//                return true;
//            }
//        }
//
//        return false;
//    }



//    public static Direction getDirectionToAllies() throws GameActionException {
//        int x = 0;
//        int y = 0;
//        int count = 0;
//        for (RobotInfo r: nearbyRobots) {
//            if (r.team == RobotPlayer.myTeam && r.type == RobotType.TURRET) { //MAKE SURE TO CHANGE THIS IF WE ABANDON TURRET STRAT
//                x += r.location.x;
//                y += r.location.y;
//                count++;
//            }
//        }
//
//        MapLocation avg = new MapLocation(x/count, y/count);
//        return myLocation.directionTo(avg);
//    }

//    public static boolean waitForDenDestruction() throws GameActionException {
//        for (RobotInfo r: nearbyRobots) {
//            if (r.type == RobotType.ZOMBIEDEN) {
//                //System.out.println("waitForDenDestruction");
//                return true;
//            }
//        }
//
//        return false;
//    }
//
//    public static boolean moveToGroup() throws GameActionException {
//        Signal signal = signals.pop(); // everyone goes to the first archons location
//        if (signal == null) {
//            return false;
//        }
//
//        if (myLocation.distanceSquaredTo(signal.getLocation()) < separation) {
//            return false;
//        }
//
//        Direction d = Utils.dirToLeastDamage(nearbyRobots, myLocation, myLocation.directionTo(signal.getLocation()));
//        if (d != Direction.NONE) {
//            rc.move(d);
//            //System.out.println("moveToGroup!");
//            return true;
//        }
//
//        return false;
//    }
//