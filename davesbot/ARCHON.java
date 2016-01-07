package davesbot;

import battlecode.common.*;

public class ARCHON {
    public static RobotInfo[] nearbyRobots;
    public static MapLocation myLocation;
    public static RobotController rc;

    public static int SOLDIER_SPAWN_COUNT = 0;
    public static int TURRET_SPAWN_COUNT = 0;

    public static void doTurn() throws GameActionException {
        nearbyRobots = rc.senseNearbyRobots(RobotPlayer.rt.sensorRadiusSquared);
        myLocation = rc.getLocation();

        // flee if necessary
        if (Utils.shouldFlee(rc, nearbyRobots, myLocation)) {
            Direction toMove = Utils.flee(rc, nearbyRobots, myLocation);
            if (toMove != Direction.NONE) {
                rc.move(toMove);
                return;
            }

            toMove = Utils.dirToLeastDamage(nearbyRobots, myLocation, Direction.NORTH);
            if (toMove != Direction.NONE) {
                rc.move(Utils.dirToLeastDamage(nearbyRobots, myLocation, Direction.NORTH));
                return;
            }

            // we are screwed! may as well try and spawn
        }

        // spawn or repair if possible
        if (activate()) return;
        if (spawn()) return;
        if (repair()) return;
        if (randomMove()) return;
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
            return true;
        }

        return false;
    }

    public static boolean spawn() throws GameActionException {
        if (SOLDIER_SPAWN_COUNT > 10 && TURRET_SPAWN_COUNT < 4) {
            if (rc.hasBuildRequirements(RobotType.SOLDIER)) {
                if (spawnInDirection(RobotType.TURRET)) {
                    TURRET_SPAWN_COUNT++;
                    return true;
                }
            }
            return false;
        }


        if (rc.hasBuildRequirements(RobotType.SOLDIER)) {
            if (spawnInDirection(RobotType.SOLDIER)) {
                SOLDIER_SPAWN_COUNT++;
                return true;
            }
        }

        return false;
    }

    public static boolean activate() throws GameActionException {
        for (RobotInfo r: nearbyRobots) {
            if (r.team == Team.NEUTRAL) {
                if (r.location.distanceSquaredTo(myLocation) < 2) {
                    rc.activate(r.location);
                    return true;
                }
                Direction d = Utils.dirToLeastDamage(nearbyRobots, myLocation, myLocation.directionTo(r.location));
                if (d != Direction.NONE) {
                    rc.move(d);
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean randomMove() throws GameActionException {
        Direction d = Utils.dirToLeastDamage(nearbyRobots, myLocation, RobotPlayer.directions[RobotPlayer.rand.nextInt()%(RobotPlayer.directions.length-1)]);
        if (d != Direction.NONE) {
            rc.move(d);
            return true;
        }

        return false;
    }

    public static void updateRallyLocation() throws GameActionException {
        double closest = 99999;
        MapLocation closestLoc = null;
        double diff;

        for (RobotInfo r: nearbyRobots) {
            if (r.team != RobotPlayer.myTeam || r.team != Team.NEUTRAL) {
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

    public static void execute() {
        rc =  RobotPlayer.rc;
        while (true) {
            try {

                if (rc.isCoreReady()) {
                    doTurn();
                }

                updateRallyLocation();
                Clock.yield();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
                Clock.yield();
            }
        }
    }
}