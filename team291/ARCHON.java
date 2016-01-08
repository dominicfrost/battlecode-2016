package team291;

import battlecode.common.*;

public class ARCHON {
    public static RobotInfo[] nearbyRobots;
    public static MapLocation myLocation;
    public static Signal[] signals;
    public static RobotController rc;

    public static int SOLDIER_SPAWN_COUNT = 0;
    public static int TURRET_SPAWN_COUNT = 0;

    public static void doTurn() throws GameActionException {
        nearbyRobots = rc.senseNearbyRobots(RobotPlayer.rt.sensorRadiusSquared);
        myLocation = rc.getLocation();
        signals = Utils.getArchonSignals();


        if (flee()) return;
        if (activate()) return;
        if (spawn()) return;
        if (repair()) return;
        if (waitForDenDestruction()) return;
        if (moveToParts()) return;
        if (moveToGroup()) return;
        if (randomMove()) return;
    }

    public static boolean flee() throws GameActionException {
        if (Utils.shouldFlee(rc, nearbyRobots, myLocation)) {
            Direction toMove = Utils.flee(rc, nearbyRobots, myLocation);
            if (toMove != Direction.NONE) {
                rc.move(toMove);
                System.out.println("flee");
                return true;
            }

            toMove = Utils.dirToLeastDamage(nearbyRobots, myLocation, Direction.NORTH);
            if (toMove != Direction.NONE) {
                rc.move(toMove);
                System.out.println("flee");
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
            System.out.println("repair");
            return true;
        }

        return false;
    }

    public static boolean spawn() throws GameActionException {
        if (SOLDIER_SPAWN_COUNT > 10 && TURRET_SPAWN_COUNT < 4) {
            if (rc.hasBuildRequirements(RobotType.SOLDIER)) {
                if (spawnInDirection(RobotType.TURRET)) {
                    TURRET_SPAWN_COUNT++;
                    System.out.println("spawn turret");
                    return true;
                }
            }
            return false;
        }


        if (rc.hasBuildRequirements(RobotType.SOLDIER)) {
            if (spawnInDirection(RobotType.SOLDIER)) {
                SOLDIER_SPAWN_COUNT++;
                System.out.println("spawn soldier");
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
                    System.out.println("activate");
                    return true;
                }
                Direction d = Utils.dirToLeastDamage(nearbyRobots, myLocation, myLocation.directionTo(r.location));
                if (d != Direction.NONE) {
                    rc.move(d);
                    System.out.println("activate");
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean moveToParts() throws GameActionException {
        MapLocation[] sightRange = Utils.getSensableLocations(myLocation);
        for (MapLocation m: sightRange) {
            if (rc.senseParts(m) != 0) {
                if (Utils.moveThrough(myLocation, m)) return true;
            }
        }

        return false;
    }

    public static boolean waitForDenDestruction() throws GameActionException {
        for (RobotInfo r: nearbyRobots) {
            if (r.type == RobotType.ZOMBIEDEN) {
                System.out.println("waitForDenDestruction");
                return true;
            }
        }

        return false;
    }

    public static boolean moveToGroup() throws GameActionException {
        Signal signal = signals[0]; // everyone goes to the first archons location
        if (signal == null) {
            return false;
        }

        Direction d = Bug.startBuggin(signal.getLocation(), myLocation, 15);
        if (d != Direction.NONE || d != Direction.OMNI) {
            rc.move(d);
            System.out.println("buggin!");
            return true;
        }

        return false;
    }

    public static boolean randomMove() throws GameActionException {
        Direction d = Utils.dirToLeastDamage(nearbyRobots, myLocation, RobotPlayer.directions[Math.abs(RobotPlayer.rand.nextInt())%RobotPlayer.directions.length]);
        if (d != Direction.NONE) {
            rc.move(d);
            System.out.println("randomMove");
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

    public static void execute() {
        rc =  RobotPlayer.rc;
        while (true) {
            try {

                if (rc.isCoreReady()) {
                    doTurn();
                } else {
                    rc.emptySignalQueue();
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