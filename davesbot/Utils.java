package davesbot;

import battlecode.common.*;
import examplefuncsplayer.*;

import java.awt.*;

public class Utils {

    public static Signal[] getArchonSignals() {
        RobotController rc = RobotPlayer.rc;
        System.out.println(rc);
        Signal[] signals = rc.emptySignalQueue();

        Signal[] toReturn = new Signal[6];

        // arraylists are bad, since i know max archons mine as well use that
        int i = 0;
        for (Signal signal: signals) {
            if (signal.getTeam() == RobotPlayer.myTeam) {
                toReturn[i] = signal;
                i++;
            }
        }

        return toReturn;
    }

    public static boolean attackGoalIfPossible(Signal[] signals) throws GameActionException {
        RobotController rc = RobotPlayer.rc;
        int[] payload;
        MapLocation sigLoc;
        for (Signal signal: signals) {
            if (signal == null) {
                break;
            }
            payload = signal.getMessage();
            sigLoc = signal.getLocation();

            // if the archon is brodcastin an attack location
            if (!sigLoc.equals(new MapLocation(payload[0], payload[1]))) {
                if (rc.canAttackLocation(sigLoc)) {
                    rc.attackLocation(sigLoc);
                    return true;
                }
            }
        }

        return false;
    }

    public static MapLocation readRallyLocation(MapLocation myLocation, Signal[] signals) {
        RobotController rc = RobotPlayer.rc;
        int minDist = Integer.MAX_VALUE;
        int dist;
        int[] msg;
        MapLocation toReturn = new MapLocation(0,0);

        for (Signal signal: signals) {
            if (signal == null) {
                return toReturn;
            }
            msg = signal.getMessage();
            dist = myLocation.distanceSquaredTo(new MapLocation(msg[0], msg[1]));
            if (dist < minDist) {
                minDist = dist;
                toReturn = new MapLocation(msg[0], msg[1]);
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

    public static int directionToInt(Direction d)  throws GameActionException {
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