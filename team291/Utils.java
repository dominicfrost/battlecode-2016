package team291;

import battlecode.common.*;

import java.util.ArrayDeque;

public class Utils {
    public static enum MessageType {
        // scout msgs
        NEUTRAL_ROBOT_LOCATION,
        PART_LOCATION,
        DEN,
        ENEMY,
        TURRET_TARGET,
    }

    public static Direction getRandomDirection() {
        return RobotPlayer.directions[RobotPlayer.rand.nextInt(8)];
    }

    // Returns an int that will be the perimeter of the distanceSquared to the rally Point
    public static int distanceSquaredToPerimeter() throws GameActionException {
        int numRobots = RobotPlayer.rc.getRobotCount();
        int weight = 2;

        return Math.round((float) (numRobots * weight / Math.PI));

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
            if (signal.getTeam() == RobotPlayer.myTeam && signal.getMessage() == null && distTo < closest) {
                toReturn = signal;
                closest = distTo;
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
                    if (robot.team == RobotPlayer.myTeam || robot.team == Team.NEUTRAL || robot.type == RobotType.ARCHON || robot.type == RobotType.SCOUT) {
                        continue;
                    }

                    if (robot.type == RobotType.TURRET) {
                        distanceAfterMovingTowards = desiredLoc.distanceSquaredTo(robot.location);
                    } else {
                        distanceAfterMovingTowards = desiredLoc.distanceSquaredTo(robot.location.add(robot.location.directionTo(desiredLoc)));
                    }

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
    public static boolean shouldFlee(RobotInfo[] enemyRobots, MapLocation loc) throws GameActionException {
        double distanceAfterMovingTowards;
        for (RobotInfo robot: enemyRobots) {
            if (robot.type == RobotType.ARCHON || robot.type == RobotType.SCOUT) {
                continue;
            }

            if (robot.type == RobotType.TURRET) {
                distanceAfterMovingTowards = loc.distanceSquaredTo(robot.location);
            } else {
                distanceAfterMovingTowards = loc.distanceSquaredTo(robot.location.add(robot.location.directionTo(loc)));
            }

            // if he moved towards me could he hit me???
            if (distanceAfterMovingTowards <= robot.type.attackRadiusSquared) return true;
        }

        return false;
    }

    // wrapper for dirToLeastDamage that trys to make the move returns true if made the move
    public static boolean moveInDirToLeastDamage(RobotInfo[] nearbyEnemies, MapLocation myLocation, Direction d) throws GameActionException {
        return moveInDirToLeastDamage(nearbyEnemies, myLocation, d, null);
    }

    public static boolean moveInDirToLeastDamage(RobotInfo[] nearbyEnemies, MapLocation myLocation, Direction d, ArrayDeque<MapLocation> seen) throws GameActionException {
        RobotController rc = RobotPlayer.rc;
        d = Utils.dirToLeastDamage(nearbyEnemies, myLocation, d, seen);

        if (d != Direction.NONE && rc.isCoreReady() && rc.canMove(d)) {
            rc.move(d);
            return true;
        }

        return false;
    }

    public static Direction dirToLeastDamage(RobotInfo[] enemyRobots, MapLocation myLocation, Direction d, ArrayDeque<MapLocation> seen) throws GameActionException {
        RobotController rc = RobotPlayer.rc;
        MapLocation desiredLoc;
        double distanceAfterMovingTowards;

        int minDamage = Integer.MAX_VALUE;
        Direction dirToMinDamage = Direction.NONE;
        int damageOnLoc;
        boolean repeateLocation;

        int offsetIndex = 0;
        int[] offsets = {0,1,-1,2,-2,3,-3,4};
        int dirint = directionToInt(d);
        while (offsetIndex < 8) {
            d = RobotPlayer.directions[(dirint+offsets[offsetIndex]+8)%8];
            offsetIndex++;

            desiredLoc = myLocation.add(d);
            repeateLocation = false;

            if (seen != null) {
                for (MapLocation m : seen) {
                    repeateLocation = m.equals(desiredLoc);
                    if (repeateLocation) {
                        break;
                    }
                }

                if (repeateLocation) {
                    continue;
                }
            }

            if (rc.canMove(d)) {

                damageOnLoc = 0;

                //iterate through each enemy bot
                for (RobotInfo robot : enemyRobots) {
                    if (robot.type == RobotType.TURRET) {
                        distanceAfterMovingTowards = desiredLoc.distanceSquaredTo(robot.location);
                    } else {
                        distanceAfterMovingTowards = desiredLoc.distanceSquaredTo(robot.location.add(robot.location.directionTo(desiredLoc)));
                    }

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

        offsetIndex = 0;
        dirint = directionToInt(d);
        while (offsetIndex < 8) {
            d = RobotPlayer.directions[(dirint+offsets[offsetIndex]+8)%8];
            offsetIndex++;

            desiredLoc = myLocation.add(d);
            repeateLocation = false;

            if (seen != null) {
                for (MapLocation m : seen) {
                    repeateLocation = m.equals(desiredLoc);
                    if (repeateLocation) {
                        break;
                    }
                }

                if (repeateLocation) {
                    continue;
                }
            }

            if (rc.canMove(d)) {

                damageOnLoc = 0;

                //iterate through each enemy bot
                for (RobotInfo robot : enemyRobots) {
                    //could he hit me if he moved in
                    if (desiredLoc.distanceSquaredTo(robot.location) <= robot.type.attackRadiusSquared) {
                        // could he out chase me if i fled to where he cant hit me now?
                        damageOnLoc += robot.type.attackPower;
                    }
                }

                if (damageOnLoc == 0) {
                    return d;
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
        MapLocation[] toReturn = new MapLocation[longest * longest * 2];
        int nextIndex = 0;
        MapLocation nextLoc;

        for (int i = longest - 1; i > -1; i--) {
            for (int j = longest - 1; j > -1; j--) {
                nextLoc = new MapLocation(myLocation.x + i, myLocation.y + j);
                if (rc.canSenseLocation(nextLoc)) {
                    toReturn[nextIndex] = nextLoc;
                    nextIndex++;
                }
            }
        }

        for (int i = longest - 1; i > 0; i--) {
            for (int j = longest - 1; j > 0; j--) {
                nextLoc = new MapLocation(myLocation.x - i, myLocation.y - j);
                if (rc.canSenseLocation(nextLoc)) {
                    toReturn[nextIndex] = nextLoc;
                    nextIndex++;
                }
            }
        }

        return toReturn;
    }


    // This method will attempt to move in Direction d (or as close to it as possible), while staying within the perimeter.
    public static boolean tryMoveWithinPerimeter(MapLocation rallyPoint, Direction d) throws GameActionException {
        RobotController rc = RobotPlayer.rc;

        int offsetIndex = 0;
        int[] offsets = {0,1,-1,2,-2};
        int dirint = directionToInt(d);
        while (offsetIndex < 5 && (!canMoveWithinPerimeter(RobotPlayer.directions[(dirint+offsets[offsetIndex]+8)%8], rallyPoint))) {
            offsetIndex++;
        }
        if (offsetIndex < 5) {
            rc.move(RobotPlayer.directions[(dirint+offsets[offsetIndex]+8)%8]);
            return true;
        }

        return false;
    }

    public static boolean canMoveWithinPerimeter(Direction d, MapLocation rallyPoint) throws GameActionException {
        RobotController rc = RobotPlayer.rc;
        return rc.canMove(d) && rallyPoint.distanceSquaredTo(rc.getLocation().add(d)) < distanceSquaredToPerimeter();
    }

    public static boolean moveThrough(MapLocation myLocation, Direction d) throws GameActionException {
        RobotController rc = RobotPlayer.rc;
        if (rc.senseRubble(myLocation.add(d)) > 50) {
            rc.clearRubble(d);
            return true;
        } else {
            return tryMove(d);
        }
    }

    public static MapLocation enemyAvgLoc(RobotInfo[] enemyRobots, MapLocation myLocation) {
        int x = 0;
        int y = 0;
        int count = 0;
        MapLocation movetwrds;
        for(RobotInfo robot: enemyRobots) {
            if (robot.type == RobotType.ARCHON || robot.type == RobotType.SCOUT) {
                continue;
            }

            movetwrds = robot.location.add(robot.location.directionTo(myLocation));
            if (movetwrds.distanceSquaredTo(myLocation) <= robot.type.attackRadiusSquared) {
                x += movetwrds.x;
                y += movetwrds.y;
                count++;
            }
        }

        return new MapLocation(x/count, y/count);
    }

    public static MapLocation getRallyLocation() {
        RobotController rc = RobotPlayer.rc;

        int sum;
        int lowest = 9999999;
        MapLocation[] archonLocs = rc.getInitialArchonLocations(RobotPlayer.myTeam);
        MapLocation rallyPoint = archonLocs[0];

        for (MapLocation m1 : archonLocs) {
            sum = 0;
            for (MapLocation m2 : archonLocs) {
                if (!m1.equals(m2)) {
                    sum += m1.distanceSquaredTo(m2); // add distance to other signal
                }
            }

            if (sum < lowest) {
                rallyPoint = m1;
                lowest = sum;
            }
        }

        return rallyPoint;
    }

    public static boolean shouldPutTurretOn(MapLocation myLocation, MapLocation rallyPoint, MapLocation potentialLoc) throws GameActionException {
      RobotController rc = RobotPlayer.rc;
      return ((potentialLoc.x + potentialLoc.y) % 2) == 0 && !rc.isLocationOccupied(potentialLoc) && rc.onTheMap(potentialLoc) && rc.senseRubble(potentialLoc) < 100 && potentialLoc.distanceSquaredTo(rallyPoint) < myLocation.distanceSquaredTo(rallyPoint);
    }

    public static MapLocation findBetterLocation(MapLocation initialLoc, MapLocation rallyPoint) throws GameActionException {
        MapLocation potentialLoc;
        for (Direction d: RobotPlayer.directions) {
            potentialLoc = initialLoc.add(d);
            if (shouldPutTurretOn(initialLoc, rallyPoint, potentialLoc)) {
              return potentialLoc;
            }
        }

        return null;
    }

    public static boolean isSurrounded(MapLocation m) {
        RobotController rc = RobotPlayer.rc;
        for (Direction d: RobotPlayer.directions) {
            if (rc.senseRubble(m.add(d)) < 50) {
                return false;
            }
        }

        return  true;
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
