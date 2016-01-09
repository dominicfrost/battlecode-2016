package team291;

import battlecode.common.*;

import java.awt.*;
import java.util.ArrayDeque;

public class ARCHON {
    public static RobotInfo[] nearbyRobots;
    public static MapLocation myLocation;
    public static ArrayDeque<Signal> signals;
    public static RobotController rc;
    public static int separation = 15;

    private static int archonCount;
    private static ArchonState state = ArchonState.NONE;
    private static MapLocation rallyPoint;

    public static enum ArchonState {
        NONE,
        REPORTING_ARCHON_COUNT,
        STAYING_NEAR_INITIAL_LOCATION,
        MOVING_TO_RALLY,
        CHILLIN_AT_RALLY,
        REPORTING_TO_AOI,
        RETURING_TO_RALLY,
        HIDING_FROM_THE_ZOMBIE_SPAWN_LIKE_A_BITCH
    }

    public static void doTurn() throws GameActionException {
        nearbyRobots = rc.senseNearbyRobots(RobotPlayer.rt.sensorRadiusSquared);
        myLocation = rc.getLocation();
        signals = Utils.getScoutSignals(rc.emptySignalQueue());

        switch (state) {
            case NONE:
                spawnInitialScout();
                break;
            case REPORTING_ARCHON_COUNT:
                reportArchonCount();
                break;
            case STAYING_NEAR_INITIAL_LOCATION:
                if (flee()) return;
                if (activate()) return;
                if (moveToParts()) return; // may have to remove this if they stray too far from home

                moveTowardRally();
                break;
            case MOVING_TO_RALLY:
                if (flee()) return;
                if (activate()) return;
                if (moveToParts()) return;

                moveTowardRally();
                break;
            case CHILLIN_AT_RALLY:
                if (flee()) return;
                if (repair()) return;
                if (spawn()) return;
                if (activate()) return;
                
                break;
            case REPORTING_TO_AOI:
                if (flee()) return;
                if (activate()) return;
                if (repair()) return;

                reportAOI();
                break;
            case RETURING_TO_RALLY:
                if (flee()) return;
                if (activate()) return;
                if (repair()) return;

                moveTowardRally();
                break;
            case HIDING_FROM_THE_ZOMBIE_SPAWN_LIKE_A_BITCH:
                if (flee()) return;
                if (repair()) return;

                moveTowardRally();
                break;
        }

        if (flee()) return;
        if (activate()) return;
        if (spawn()) return;
        if (repair()) return;
//        if (waitForDenDestruction()) return; // Only needed with soldier strat
        if (moveToParts()) return;
        if (moveToGroup()) return;
        if (randomMove()) return;
        //System.out.println("NO MOVE MADE!");
    }

    public static boolean flee() throws GameActionException {
        if (Utils.shouldFlee(rc, nearbyRobots, myLocation)) {
            Direction toMove = Utils.flee(rc, nearbyRobots, myLocation);
            if (toMove != Direction.NONE) {
                rc.move(toMove);
                //System.out.println("flee");
                return true;
            }

            Direction dirToAllies = getDirectionToAllies();
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

//        if (SOLDIER_SPAWN_COUNT > 10 && TURRET_SPAWN_COUNT < 4) {
//            if (rc.hasBuildRequirements(RobotType.SOLDIER)) {
//                if (spawnInDirection(RobotType.TURRET)) {
//                    TURRET_SPAWN_COUNT++;
//                    //System.out.println("spawn turret");
//                    return true;
//                }
//            }
//            return false;
//        }
//
//
//        if (rc.hasBuildRequirements(RobotType.SOLDIER)) {
//            if (spawnInDirection(RobotType.SOLDIER)) {
//                SOLDIER_SPAWN_COUNT++;
//                //System.out.println("spawn soldier");
//                return true;
//            }
//        }

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

    public static boolean waitForDenDestruction() throws GameActionException {
        for (RobotInfo r: nearbyRobots) {
            if (r.type == RobotType.ZOMBIEDEN) {
                //System.out.println("waitForDenDestruction");
                return true;
            }
        }

        return false;
    }

    public static boolean moveToGroup() throws GameActionException {
        Signal signal = signals.pop(); // everyone goes to the first archons location
        if (signal == null) {
            return false;
        }

        if (myLocation.distanceSquaredTo(signal.getLocation()) < separation) {
            return false;
        }

        Direction d = Utils.dirToLeastDamage(nearbyRobots, myLocation, myLocation.directionTo(signal.getLocation()));
        if (d != Direction.NONE) {
            rc.move(d);
            //System.out.println("moveToGroup!");
            return true;
        }

        return false;
    }

    public static boolean randomMove() throws GameActionException {
        Direction d = Utils.dirToLeastDamage(nearbyRobots, myLocation, RobotPlayer.directions[Math.abs(RobotPlayer.rand.nextInt())%RobotPlayer.directions.length]);
        if (d != Direction.NONE) {
            rc.move(d);
            //System.out.println("randomMove");
            return true;
        }

        return false;
    }

    public static void updateRallyLocation() throws GameActionException {
        double closest = 99999;
        MapLocation closestLoc = null;
        double diff;

        for (RobotInfo r: nearbyRobots) {
            if (r.team != RobotPlayer.myTeam && r.team != Team.NEUTRAL) {
                diff = myLocation.distanceSquaredTo(r.location);
                if  (diff < closest) {
                    closestLoc = r.location;
                    closest = diff;
                }
            }
        }

        if (closestLoc != null) {
            rc.broadcastMessageSignal(closestLoc.x, closestLoc.y, RobotPlayer.maxSignalRange);
            return;
        }

        for (RobotInfo r: nearbyRobots) {
            if (r.type == RobotType.ZOMBIEDEN) {
                rc.broadcastMessageSignal(r.location.x, r.location.y, RobotPlayer.maxSignalRange);
                return;
            }
        }

        rc.broadcastMessageSignal(myLocation.x, myLocation.y, RobotPlayer.maxSignalRange);
    }

    public static boolean spawnInDirection(RobotType type) throws GameActionException {
        for (Direction d: RobotPlayer.directions) {
            if (rc.canBuild(d, type)) {
                rc.build(d, type);
                return true;
            }
        }

        return false;
    }

    public static boolean spawnTurret() throws GameActionException {
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

    public static Direction getDirectionToAllies() throws GameActionException {
        int x = 0;
        int y = 0;
        int count = 0;
        for (RobotInfo r: nearbyRobots) {
            if (r.team == RobotPlayer.myTeam && r.type == RobotType.TURRET) { //MAKE SURE TO CHANGE THIS IF WE ABANDON TURRET STRAT
                x += r.location.x;
                y += r.location.y;
                count++;
            }
        }

        MapLocation avg = new MapLocation(x/count, y/count);
        return myLocation.directionTo(avg);
    }

    public static void execute() {
        rc =  RobotPlayer.rc;
        while (true) {
            try {

                if (rc.isCoreReady()) {
                    doTurn();
                } else {
                    rc.emptySignalQueue();
                }

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