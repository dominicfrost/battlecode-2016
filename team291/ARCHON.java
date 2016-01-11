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

    private static ArchonState state = ArchonState.NONE;
    private static MapLocation rallyPoint;
    private static MapLocation aoi;

    public static enum ArchonState {
        NONE,
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
                getRallyLocation();
                break;
            case MOVING_TO_RALLY:
                if (isCoreReady) {
                    if (flee()) return;
                    if (activate()) return;
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
            if (RobotPlayer.id == 1691) System.out.println(toMove);

            if (toMove != Direction.NONE) {
                rc.move(toMove);

                //System.out.println("flee");
                return true;
            }

            Direction dirToAllies = myLocation.directionTo(rallyPoint);
            if (Utils.moveInDirToLeastDamage(nearbyRobots, myLocation, dirToAllies)) return true;
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
            if (isCoreReady && rc.senseParts(m) != 0 && rc.senseRubble(m) < 50) {
                Direction d = Bug.startBuggin(rallyPoint, myLocation, 0);
                if (d != Direction.NONE && d != Direction.OMNI) {
                    rc.move(d);
                    return true;
                }
            }
        }

        return false;
    }

    public static void getRallyLocation() throws GameActionException {
        Clock.yield();
        rc.broadcastMessageSignal(0, 0, 10000);
        Clock.yield();

        signals = Utils.getScoutSignals(rc.emptySignalQueue());
        int sum;
        int lowest = 0;
        int lowestRobotId = RobotPlayer.id;
        rallyPoint = myLocation;

        for (Signal s: signals) {
            lowest += myLocation.distanceSquaredTo(s.getLocation()); // lowest is initialy my location to all others
        }

        for (Signal s1: signals) {
            sum = myLocation.distanceSquaredTo(s1.getLocation()); //start sum at distance to mylocation
            for (Signal s2: signals) {
                if (!s2.equals(s1)) {
                    sum += s1.getLocation().distanceSquaredTo(s2.getLocation()); // add distance to other signal
                }
            }
            if (sum < lowest) {
                rallyPoint = s1.getLocation();
                lowest = sum;
                lowestRobotId = s1.getRobotID();
            }
            // lower robot id breaks the tie
            if (sum == lowest && s1.getRobotID() < lowestRobotId) {
                rallyPoint = s1.getLocation();
                lowest = sum;
                lowestRobotId = s1.getRobotID();
            }
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
            for (Signal s : signals) {
                msg = s.getMessage();
                if (msg[0] == Utils.MessageType.PART_LOCATION.ordinal() || msg[0] == Utils.MessageType.NEUTRAL_ROBOT_LOCATION.ordinal()) {
                    aoi = Utils.deserializeMapLocation(msg[1]);
                    state = ArchonState.REPORTING_TO_AOI;
                    return;
                }
            }

            if (isCoreReady && myLocation.distanceSquaredTo(rallyPoint) > 9) {
                Utils.moveInDirToLeastDamage(nearbyRobots, myLocation, myLocation.directionTo(rallyPoint));
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
                Clock.yield();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
                Clock.yield();
            }
        }
    }
}
