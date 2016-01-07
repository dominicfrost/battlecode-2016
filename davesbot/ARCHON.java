package davesbot;

import battlecode.common.*;

public class ARCHON {
    public static RobotInfo[] nearbyRobots;
    public static MapLocation myLocation;
    public static RobotController rc;

    public static void doTurn() throws GameActionException {
        nearbyRobots = rc.senseNearbyRobots(RobotPlayer.rt.sensorRadiusSquared);
        myLocation = rc.getLocation();

        // tell errone who to kill
        updateRallyLocation();

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

        // spawn if possible
        if (spawn()) return;
    }

    public static boolean spawn() throws GameActionException {
        if (rc.hasBuildRequirements(RobotType.SOLDIER)) {
            return spawnInDirection(RobotType.SOLDIER);
        }

        return false;
    }

    public static void updateRallyLocation() throws GameActionException {
        for (RobotInfo r: nearbyRobots) {
            if (r.team != RobotPlayer.myTeam && myLocation.distanceSquaredTo(r.location) <= r.type.attackRadiusSquared) {
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

    public static void execute(RobotController _rc) {
        rc = _rc;
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