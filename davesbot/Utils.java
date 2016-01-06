package davesbot;

import battlecode.common.*;
import examplefuncsplayer.*;

import java.awt.*;

public class Utils {

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

    public static Direction dirToLeastDamage(RobotController rc, RobotInfo[] enemyRobots, MapLocation myLocation) throws GameActionException {
        MapLocation desiredLoc;
        double distanceAfterMovingTowards;

        int minDamage = Integer.MAX_VALUE;
        Direction dirToMinDamage = Direction.NONE;
        int damageOnLoc;

        for (Direction d: RobotPlayer.directions) {
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

                if (damageOnLoc < minDamage) {
                    minDamage = damageOnLoc;
                    dirToMinDamage = d;
                }
            }
        }

        return dirToMinDamage;
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


}