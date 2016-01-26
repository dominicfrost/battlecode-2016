package team291;

import battlecode.common.*;

import java.util.ArrayDeque;

public class ARCHON {
    public static RobotInfo[] nearbyAllies;
    public static RobotInfo[] nearbyEnemies;
    public static MapLocation myLocation;
    public static ArrayDeque<Signal> signals;
    public static RobotController rc;
    private static boolean isCoreReady;

    private static ArchonState state = ArchonState.NONE;
    private static MapLocation rallyPoint;
    private static MapLocation aoi;
    private static int aoiType;

    private static int spawnFate = -1;
    private static boolean hasSpawnedInitialGuard = false;

    private static ArrayDeque<MapLocation> seen = new ArrayDeque<>();

    private static double totalRounds;

    public static enum ArchonState {
        NONE,
        MOVING_TO_RALLY,
        CHILLIN_AT_RALLY,
        FLEEING,
        REPORTING_TO_AOI,
        RETURING_TO_RALLY,
        HIDING_FROM_THE_ZOMBIE_SPAWN_LIKE_A_BITCH
    }

    public static void doTurn() throws GameActionException {
        isCoreReady = rc.isCoreReady();
        myLocation = rc.getLocation();
        nearbyAllies = rc.senseNearbyRobots(RobotPlayer.rt.sensorRadiusSquared, RobotPlayer.myTeam);
        nearbyEnemies = rc.senseHostileRobots(myLocation, RobotPlayer.rt.sensorRadiusSquared);
        signals = Utils.getScoutSignals(rc.emptySignalQueue());

        rc.setIndicatorString(0, state.name());
        if (aoi != null) rc.setIndicatorString(1, aoi.toString());
        repair();
        switch (state) {
            case NONE:
                getRallyLocation();
                break;
            case MOVING_TO_RALLY:
                if (isCoreReady && shouldFlee()) break;
                returnToRally();
                break;
            case CHILLIN_AT_RALLY:
                if (isCoreReady) {
                    if (shouldFlee()) break;
                    if (spawn()) break;
                    if (moveToParts()) break;
                    if (activate()) break;
                }
                chill();
                break;
            case FLEEING:
                flee();
                break;
            case REPORTING_TO_AOI:
                if (isCoreReady) {
                    if (shouldFlee()) break;
                    if (activate()) break;
                    if (moveToParts()) break;
                }

                reportToAOI();
                break;
            case RETURING_TO_RALLY:
                if (isCoreReady) {
                    if (shouldFlee()) break;
                    if (activate()) break;
                    if (moveToParts()) break;
                }
                returnToRally();
                break;
            case HIDING_FROM_THE_ZOMBIE_SPAWN_LIKE_A_BITCH:
                if (isCoreReady) {
                    if (shouldFlee()) break;
                    Utils.moveInDirToLeastDamage(nearbyEnemies, myLocation, myLocation.directionTo(rallyPoint));
                }
                break;
        }
    }

    public static boolean shouldFlee() throws GameActionException {
        if (Utils.shouldFlee(nearbyEnemies, myLocation)) {
            seen.clear();
            state = ArchonState.FLEEING;
            flee();
            return true;
        }

        return false;
    }

    public static void flee() throws GameActionException {
        if (!Utils.shouldFlee(nearbyEnemies, myLocation)) {
            state = ArchonState.RETURING_TO_RALLY;
            returnToRally();
            return;
        }
        if (isCoreReady) {
            if (Utils.moveInDirToLeastDamage(nearbyEnemies, myLocation, myLocation.directionTo(rallyPoint), seen)) {
                seen.add(myLocation);
                if (seen.size() > 5) seen.pop();
                return;
            }
            if (Utils.moveInDirToLeastDamage(nearbyEnemies, myLocation, myLocation.directionTo(rallyPoint), null)) {
                seen.add(myLocation);
                if (seen.size() > 5) seen.pop();
                return;
            }
            for (Direction d : RobotPlayer.directions) {
                if (rc.senseRobotAtLocation(myLocation.add(d)) != null && rc.onTheMap(myLocation.add(d))) {
                    Utils.moveThrough(myLocation, d);
                    return;
                }
            }
        }
    }

    public static boolean repair() throws GameActionException {
        double highestHealth = 1;
        MapLocation highestHealthLocation = null;

        for (RobotInfo r : nearbyAllies) {
            if (myLocation.distanceSquaredTo(r.location) <= RobotPlayer.rt.attackRadiusSquared && r.health > highestHealth && r.type != RobotType.ARCHON && r.health != r.type.maxHealth) {
                highestHealthLocation = r.location;
                highestHealth = r.health;
            }
        }

        if (highestHealthLocation != null) {
            rc.repair(highestHealthLocation);
            return true;
        }

        return false;
    }

    public static boolean spawn() throws GameActionException {
        if (rc.hasBuildRequirements(RobotType.TURRET)) {
            if (!hasSpawnedInitialGuard) {
                if (spawnGuard()) {
                    hasSpawnedInitialGuard = true;
                    return true;
                }
                return false;
            }

            if (spawnFate < 5) {
                if (trySpawn(Direction.NORTH, RobotType.SCOUT)) {
                    spawnFate = Math.abs(RobotPlayer.rand.nextInt() % 100);
                    return true;
                }
                return false;

            }

            if (spawnFate < spawnThreshold_1to2_10to1_fast()) {
                 if (spawnTurret()) {
                     spawnFate = Math.abs(RobotPlayer.rand.nextInt() % 100);
                     return true;
                 }
                 return false;
            } else {
                 if (spawnSoldier()) {
                     spawnFate = Math.abs(RobotPlayer.rand.nextInt() % 100);
                     return true;
                 }
                 return false;
            }


        }

        return false;
    }
    
    // changes the ratio of turrets spawned as the game progresses
    // at the beginning of the game, we spawn 1:2, and at the end 10:1
    public static int spawnThreshold_1to2_10to1_fast() {
        float totalPercentages = 95;
        double roundPercent = Math.min(rc.getRoundNum() / (totalRounds / 2), 1);
        // 33% -> 90%
        return (int) (totalPercentages * (.33 + (.57 * roundPercent)));
    }

    public static boolean activate() throws GameActionException {
        RobotInfo[] neutrals = rc.senseNearbyRobots(2, Team.NEUTRAL);
        if (neutrals.length > 0) {
            rc.activate(neutrals[0].location);
            return true;
        }
        return false;
    }

    public static boolean moveToParts() throws GameActionException {
        MapLocation adj;
        for (Direction d : RobotPlayer.directions) {
            adj = myLocation.add(d);
            if (rc.canMove(d) && rc.senseParts(adj) != 0 && rc.senseRubble(adj) < 100) {
                rc.move(d);
                return true;
            }
        }

        return false;
    }

    public static void getRallyLocation() throws GameActionException {
        rallyPoint = Utils.getRallyLocation();
        if (myLocation.equals(rallyPoint)) {
            trySpawn(Direction.NORTH, RobotType.SCOUT);
        }
        state = ArchonState.MOVING_TO_RALLY;
    }


    public static void returnToRally() throws GameActionException {
        if (myLocation.distanceSquaredTo(rallyPoint) < 2) {
            state = ArchonState.CHILLIN_AT_RALLY;
            chill();
            return;
        }

        if (isCoreReady) {
            Direction d = Bug.startBuggin(rallyPoint, myLocation, 0);
            if (d != Direction.NONE && d != Direction.OMNI) {
                rc.move(d);
            } else if (d == Direction.OMNI) {
                Utils.moveInDirToLeastDamage(nearbyEnemies, myLocation, myLocation.directionTo(rallyPoint));
            }
        }
    }

    public static void chill() throws GameActionException {
        if (!rc.hasBuildRequirements(RobotType.TURRET)) {
            int[] msg;

            for (Signal s : signals) {
                msg = s.getMessage();
                if (msg[0] == Utils.MessageType.PART_LOCATION.ordinal() || msg[0] == Utils.MessageType.NEUTRAL_ROBOT_LOCATION.ordinal()) {
                    aoiType = msg[0];
                    aoi = Utils.deserializeMapLocation(msg[1]);
                    state = ArchonState.REPORTING_TO_AOI;
                    return;
                }
            }

            if (isCoreReady && myLocation.distanceSquaredTo(rallyPoint) > 2) {
                Utils.moveInDirToLeastDamage(nearbyEnemies, myLocation, myLocation.directionTo(rallyPoint));
            }
        }
    }

    public static void reportToAOI() throws GameActionException {
        // if im at the goal go back
        if (myLocation.equals(aoi)) {
            state = ArchonState.RETURING_TO_RALLY;
            returnToRally();
            return;
        }

        if (aoiType == Utils.MessageType.NEUTRAL_ROBOT_LOCATION.ordinal()) {
            if (rc.canSenseLocation(aoi) && rc.senseRobotAtLocation(aoi) == null) {
                state = ArchonState.RETURING_TO_RALLY;
                returnToRally();
                return;
            }
        }

        if (aoiType == Utils.MessageType.PART_LOCATION.ordinal()) {
            if (rc.canSenseLocation(aoi) && rc.senseParts(aoi) <= 0) {
                state = ArchonState.RETURING_TO_RALLY;
                returnToRally();
                return;
            }
        }
        if (isCoreReady) {
            Direction d = Bug.startBuggin(aoi, myLocation, 0);
            if (d != Direction.NONE && d != Direction.OMNI) {
                Utils.moveInDirToLeastDamage(nearbyEnemies, myLocation, d);
            } else if (d == Direction.OMNI) {
                Utils.moveInDirToLeastDamage(nearbyEnemies, myLocation, myLocation.directionTo(aoi));
            } else if (d == Direction.NONE) {
                state = ArchonState.RETURING_TO_RALLY;
                returnToRally();
            }
        }
    }


    public static boolean randomMove() throws GameActionException {
        return isCoreReady && Utils.moveInDirToLeastDamage(nearbyEnemies, myLocation, RobotPlayer.directions[Math.abs(RobotPlayer.rand.nextInt()) % RobotPlayer.directions.length]);
    }

    public static boolean spawnTurret() throws GameActionException {
         // don't need to check core because this should only be called from spawn!
        MapLocation potentialSpawnPoint;
        double minDistToRally = 9999999;
        double distToRally;
        Direction toSpawn = Direction.NONE;

        for (Direction d : RobotPlayer.directions) {
            potentialSpawnPoint = myLocation.add(d);
            distToRally = potentialSpawnPoint.distanceSquaredTo(rallyPoint);
            if (((potentialSpawnPoint.x + potentialSpawnPoint.y) % 2) == 0 && rc.canBuild(d, RobotType.TURRET) && minDistToRally > distToRally) {
                minDistToRally = distToRally;
                toSpawn = d;
            }
        }

        if (toSpawn != Direction.NONE) {
            MapLocation better = Utils.findBetterLocation(myLocation.add(toSpawn), rallyPoint);
            if (better != null) {
                if (Utils.moveInDirToLeastDamage(nearbyEnemies, myLocation, myLocation.directionTo(better))) {
                    return true;
                }
                rc.build(toSpawn, RobotType.TURRET);
            }
            rc.build(toSpawn, RobotType.TURRET);
            return true;
        }

        return randomMove();
    }

    public static boolean spawnGuard() throws GameActionException {
        return trySpawn(Direction.NORTH, RobotType.GUARD);
    }

    public static boolean spawnSoldier() throws GameActionException {
        return trySpawn(Direction.NORTH, RobotType.SOLDIER);
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
        rc = RobotPlayer.rc;
        spawnFate = Math.abs(RobotPlayer.rand.nextInt() % 100);
        totalRounds = rc.getRoundLimit();
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
