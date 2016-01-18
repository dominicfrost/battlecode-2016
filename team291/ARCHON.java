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

    private static int spawnFate = -1;

    private static ArrayDeque<MapLocation> seen = new ArrayDeque<>();

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

        switch (state) {
            case NONE:
                getRallyLocation();
                break;
            case MOVING_TO_RALLY:
                if (isCoreReady && shouldFlee()) return;
                returnToRally();
                break;
            case CHILLIN_AT_RALLY:
                if (isCoreReady) {
                    if (shouldFlee()) return;
                    if (spawn()) return;
                    if (moveToParts()) return;
                    if (repair()) return;
                    if (activate()) return;
                }
                chill();
                break;
            case FLEEING:
                flee();
                break;
            case REPORTING_TO_AOI:
                if (isCoreReady) {
                    if (shouldFlee()) {
                        state = ArchonState.RETURING_TO_RALLY;
                        return;
                    }
                    if (activate()) return;
                    if (repair()) return;
                }

                reportToAOI();
                break;
            case RETURING_TO_RALLY:
                if (isCoreReady) {
                    if (shouldFlee()) return;
                    if (activate()) return;
                    if (repair()) return;
                    if (moveToParts()) return;
                }

                returnToRally();
                break;
            case HIDING_FROM_THE_ZOMBIE_SPAWN_LIKE_A_BITCH:
                if (isCoreReady) {
                    if (shouldFlee()) return;
                    if (repair()) return;
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
            }
        }
    }

    public static boolean repair() throws GameActionException {
        double lowestHealth = 1001;
        MapLocation lowestHealthLocation = null;

        for (RobotInfo r: nearbyAllies) {
            if (myLocation.distanceSquaredTo(r.location) <= RobotPlayer.rt.attackRadiusSquared && r.health < lowestHealth && r.type != RobotType.ARCHON && r.health != r.type.maxHealth) {
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
            if (spawnFate < 50) {
                if (spawnTurret()) {
                    spawnFate = Math.abs(RobotPlayer.rand.nextInt() % 100);
                    return true;
                }
                return false;
            }

            if (spawnFate < 95) {
                if (spawnGuard()) {
                    spawnFate = Math.abs(RobotPlayer.rand.nextInt() % 100);
                    return true;
                }
                return false;
            }

            if (spawnFate < 100) {
                if (trySpawn(Direction.NORTH, RobotType.SCOUT)) {
                    spawnFate = Math.abs(RobotPlayer.rand.nextInt() % 100);
                    return true;
                }
                return false;

            }
        }

        return false;
    }

    public static boolean activate() throws GameActionException {
        RobotInfo[] neutrals = rc.senseNearbyRobots(RobotPlayer.rt.sensorRadiusSquared, Team.NEUTRAL);
        for (RobotInfo r: neutrals) {
            if (r.location.distanceSquaredTo(myLocation) < 2) {
                rc.activate(r.location);
                //System.out.println("activate");
                return true;
            }
            if (Utils.moveInDirToLeastDamage(nearbyEnemies, myLocation, myLocation.directionTo(r.location))) return true;
        }
        return false;
    }

    public static boolean moveToParts() throws GameActionException {
        MapLocation adj;
        for (Direction d: RobotPlayer.directions) {
            adj = myLocation.add(d);
            if (rc.canMove(d) && rc.senseParts(adj) != 0 && rc.senseRubble(adj) < 100) {
                if (Utils.moveInDirToLeastDamage(nearbyEnemies, myLocation, d)) return true;
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
        if (myLocation.distanceSquaredTo(rallyPoint) < 9) {
            state = ArchonState.CHILLIN_AT_RALLY;
            chill();
            return;
        }

        if (isCoreReady) {
            Direction d = Bug.startBuggin(rallyPoint, myLocation, 0);
            if (d != Direction.NONE && d != Direction.OMNI) {
                rc.move(d);
            }
        }
    }

    public static void chill() throws GameActionException {
        if (!rc.hasBuildRequirements(RobotType.TURRET)) {
            int[] msg;

            ogLoop:
            for (Signal s : signals) {
                msg = s.getMessage();
                if (msg[0] == Utils.MessageType.PART_LOCATION.ordinal() || msg[0] == Utils.MessageType.NEUTRAL_ROBOT_LOCATION.ordinal()) {
                    int signalId = s.getID();
                    for (Signal s2: signals) {
                        if (s2.getMessage()[0] == Utils.MessageType.AOI_CONFIRMED.ordinal() && s2.getMessage()[1] == signalId) {
                            continue ogLoop;
                        }
                    }

                    aoi = Utils.deserializeMapLocation(msg[1]);
                    state = ArchonState.REPORTING_TO_AOI;
                    rc.broadcastMessageSignal(Utils.MessageType.AOI_CONFIRMED.ordinal(), signalId, RobotPlayer.maxSignalRange);
                    return;
                }
            }

            if (isCoreReady && myLocation.distanceSquaredTo(rallyPoint) > 9) {
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

        // if there is a bot at the location and im next to it go back
        if (rc.canSenseLocation(aoi) && rc.senseRobotAtLocation(aoi) != null && myLocation.distanceSquaredTo(aoi) < 2) {
            state = ArchonState.RETURING_TO_RALLY;
            returnToRally();
            return;
        }

        if (isCoreReady) {
            Direction d = Bug.startBuggin(aoi, myLocation, 0);
            if (d != Direction.NONE && d != Direction.OMNI) {
                rc.move(d);
            }
        }
    }


    public static boolean randomMove() throws GameActionException {
        return isCoreReady && Utils.moveInDirToLeastDamage(nearbyEnemies, myLocation, RobotPlayer.directions[Math.abs(RobotPlayer.rand.nextInt()) % RobotPlayer.directions.length]);
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

        return randomMove();
    }

    public static boolean spawnGuard() throws GameActionException{
        if (!trySpawn(Direction.NORTH, RobotType.GUARD)) {
            return false;
        }
        rc.broadcastMessageSignal(Utils.MessageType.RALLY_LOCATION.ordinal(), Utils.serializeMapLocation(rallyPoint), RobotPlayer.rt.sensorRadiusSquared);
        return true;
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
        spawnFate = Math.abs(RobotPlayer.rand.nextInt() % 100);
        while (true) {
            try {
                doTurn();
                Clock.yield();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
                Clock.yield();
            }
        }
    }
}
