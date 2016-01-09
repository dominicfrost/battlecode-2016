package team291;

import battlecode.common.*;

import java.util.ArrayDeque;

public class Utils {
    public static enum MessageType {
        // scout msgs
        ARCHON_COUNT_CONFIRMED,
        LOOKING_FOR_ALLY_SCOUT,
        RALLY_LOCATION_REPORT,
        NEUTRAL_ROBOT_LOCATION,
        PART_LOCATION,

        // archon msgs
        ARCHON_COUNT,
        RALLY_POINT_CONFIRMED,
        AOI_CONFIRMED
    }

    public static boolean attack(MapLocation loc) throws GameActionException {
        RobotController rc = RobotPlayer.rc;
        if (rc.isWeaponReady() && rc.canAttackLocation(loc)) {
            rc.attackLocation(loc);
            return true;
        }

        return false;
    }

    public static ArrayDeque<Signal> getScoutSignals(Signal[] signals) {
        ArrayDeque<Signal> scoutSignals = new ArrayDeque<>();

        for (Signal signal: signals) {
            if (signal.getTeam() == RobotPlayer.myTeam && signal.getMessage() != null) {
                scoutSignals.add(signal);
            }
        }

        return scoutSignals;
    }

    // gets the closest distress signal
    public static Signal getDistressSignal(Signal[] signals, MapLocation myLocation) {
        Signal toReturn = null;
        double closest = 999999;
        double distTo;
        // arraylists are bad, since i know max archons mine as well use that
        for (Signal signal: signals) {
            distTo = signal.getLocation().distanceSquaredTo(myLocation);
            if (signal.getTeam() == RobotPlayer.myTeam && distTo < closest) {
                toReturn = signal;
                closest = distTo;
            }
        }

        return toReturn;
    }

    public static boolean attackGoalIfPossible(Signal[] signals) throws GameActionException {
        int[] payload;
        MapLocation sigLoc;
        for (Signal signal: signals) {
            if (signal == null) {
                break;
            }
            payload = signal.getMessage();
            sigLoc = new MapLocation(payload[0], payload[1]);

            // if the archon is brodcastin an attack location

            if (!signal.getTeam().equals(RobotPlayer.myTeam) && !sigLoc.equals(signal.getLocation())) {
                if (attack(sigLoc)) return true;
            }
        }

        return false;
    }

    public static MapLocation readRallyLocation(MapLocation myLocation, Signal[] signals) {
        int minDist = Integer.MAX_VALUE;
        int dist;
        MapLocation toReturn = null;
        MapLocation loc;
        for (Signal signal: signals) {
            if (signal == null) {
                return toReturn;
            }
            loc = signal.getLocation();
            dist = myLocation.distanceSquaredTo(loc);
            if (dist < minDist) {
                minDist = dist;
                toReturn = loc;
            }
        }

        return toReturn;
    }


    /*
     * Finds a location where no enemies can attack
     *
     *
     * if no such location exists return false
     */
    public static Direction flee(RobotController rc, RobotInfo[] enemyRobots, MapLocation myLocation) throws GameActionException {
        MapLocation desiredLoc;
        double distanceAfterMovingTowards;

        dirLoop:
        for (Direction d: RobotPlayer.directions) {
            desiredLoc = myLocation.add(d);

            //if i can move in the given direction
            if (rc.canMove(d)) {

                //iterate through each enemy bot
                for (RobotInfo robot : enemyRobots) {
                    if (robot.team == RobotPlayer.myTeam) {
                        continue;
                    }

                    distanceAfterMovingTowards = desiredLoc.distanceSquaredTo(robot.location.add(robot.location.directionTo(desiredLoc)));

                    //could he hit me if he moved in
                    if (distanceAfterMovingTowards <= robot.type.attackRadiusSquared) {

                        // could he out chase me if i fled to where he cant hit me now?
                        if (desiredLoc.distanceSquaredTo(robot.location) <= robot.type.attackRadiusSquared) {
                            continue dirLoop;
                        }
                    }

                }

                return d;
            }
        }

        return Direction.NONE;
    }

    /*
     * returns true if someone can hit me at my current location
     *
     * broadcast - should i broadcast the enemies location as a temporary goal
     */
    public static boolean shouldFlee(RobotController rc, RobotInfo[] enemyRobots, MapLocation loc) throws GameActionException {
        for (RobotInfo robot: enemyRobots) {
            if (robot.team == RobotPlayer.myTeam) {
                continue;
            }

            double distanceAfterMovingTowards = loc.distanceSquaredTo(robot.location.add(robot.location.directionTo(loc)));
            int attackRad = robot.type.attackRadiusSquared;

            // if he moved towards me could he hit me???
            if (distanceAfterMovingTowards <= attackRad) return true;
        }

        return false;
    }

    // wrapper for dirToLeastDamage that trys to make the move returns true if made the move
    public static boolean moveInDirToLeastDamage(RobotInfo[] nearbyRobots, MapLocation myLocation, Direction d) throws GameActionException {
        RobotController rc = RobotPlayer.rc;
        d = Utils.dirToLeastDamage(nearbyRobots, myLocation, d);
        if (d != Direction.NONE) {
            rc.move(d);
            return true;
        }

        return false;
    }

    public static Direction dirToLeastDamage(RobotInfo[] enemyRobots, MapLocation myLocation, Direction d) throws GameActionException {
        RobotController rc = RobotPlayer.rc;
        MapLocation desiredLoc;
        double distanceAfterMovingTowards;

        int minDamage = Integer.MAX_VALUE;
        Direction dirToMinDamage = Direction.NONE;
        int damageOnLoc;

        int offsetIndex = 0;
        int[] offsets = {0,1,-1,2,-2,3,-3,4};
        int dirint = directionToInt(d);
        while (offsetIndex < 8) {
            d = RobotPlayer.directions[(dirint+offsets[offsetIndex]+8)%8];
            offsetIndex++;

            desiredLoc = myLocation.add(d);
            if (rc.canMove(d)) {

                damageOnLoc = 0;

                //iterate through each enemy bot
                for (RobotInfo robot : enemyRobots) {
                    if (robot.team == RobotPlayer.myTeam) {
                        continue;
                    }

                    distanceAfterMovingTowards = desiredLoc.distanceSquaredTo(robot.location.add(robot.location.directionTo(desiredLoc)));

                    //could he hit me if he moved in
                    if (distanceAfterMovingTowards <= robot.type.attackRadiusSquared) {

                        // could he out chase me if i fled to where he cant hit me now?
                        damageOnLoc += robot.type.attackPower;
                    }

                }

                if (damageOnLoc == 0) {
                    return d;
                }

                if (damageOnLoc < minDamage) {
                    minDamage = damageOnLoc;
                    dirToMinDamage = d;
                }
            }
        }

        return dirToMinDamage;
    }

    // This method will attempt to move in Direction d (or as close to it as possible)
    public static boolean tryMove(Direction d) throws GameActionException {
        RobotController rc = RobotPlayer.rc;

        int offsetIndex = 0;
        int[] offsets = {0,1,-1,2,-2};
        int dirint = directionToInt(d);
        while (offsetIndex < 5 && (!rc.canMove(RobotPlayer.directions[(dirint+offsets[offsetIndex]+8)%8]))) {
            offsetIndex++;
        }
        if (offsetIndex < 5) {
            rc.move(RobotPlayer.directions[(dirint+offsets[offsetIndex]+8)%8]);
            return true;
        }

        return false;
    }

    public static MapLocation[] getSensableLocations(MapLocation myLocation) {
        RobotController rc = RobotPlayer.rc;

        int longest = (int) Math.sqrt(RobotPlayer.rt.sensorRadiusSquared);
        MapLocation[] toReturn = new MapLocation[longest * longest];
        int nextIndex = 0;
        MapLocation nextLoc;

        for (int i = 0; i < longest; i++) {
            for (int j = 0; j < longest; j++) {
                nextLoc = new MapLocation(myLocation.x + i, myLocation.y + j);
                if (rc.canSenseLocation(nextLoc)) {
                    toReturn[nextIndex] = nextLoc;
                    nextIndex++;
                }
            }
        }

        return toReturn;
    }

    public static boolean moveThrough(MapLocation myLocation, MapLocation goal) throws GameActionException {
        RobotController rc = RobotPlayer.rc;
        Direction toNext = myLocation.directionTo(goal);
        if (rc.senseRubble(myLocation.add(toNext)) > 50) {
            rc.clearRubble(toNext);
            return true;
        } else {
            return tryMove(toNext);
        }
    }


    public static int getLeft(int field) {
        return field >> 16; // sign bit is significant
    }

    public static int getRight(int field) {
        return (short) (field & 0xFFFF); //gets cast back to signed int
    }

    public static int serializeMapLocation(MapLocation m) {
        return (m.x << 16) | (m.y & 0xFFFF);
    }

    public static MapLocation deserializeMapLocation(int i) {
        return new MapLocation(getLeft(i), getRight(i));
    }

    public static int directionToInt(Direction d) throws GameActionException {
        switch(d) {
            case NORTH:
                return 0;
            case NORTH_EAST:
                return 1;
            case EAST:
                return 2;
            case SOUTH_EAST:
                return 3;
            case SOUTH:
                return 4;
            case SOUTH_WEST:
                return 5;
            case WEST:
                return 6;
            case NORTH_WEST:
                return 7;
            default:
                return 0;
        }
    }

}