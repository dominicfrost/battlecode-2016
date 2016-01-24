package team291;

import java.util.ArrayList;
import battlecode.common.*;

public class Circle {
    public static RobotController rc;
    public static boolean circlingCW;
    public static int circlingDir;
    public static RobotInfo[] nearbyEnemies;
    public static MapLocation rallyPoint;
    public static MapLocation myLocation;
    public static int circleRadius;

    public Circle(MapLocation _rallyPoint, int _circleRadius) {
        rallyPoint = _rallyPoint;
        circleRadius = _circleRadius;
        circlingDir = this.getRandomCirclingDir();
        circlingCW = true;
        rc = RobotPlayer.rc;
    }

    public void setCircleRadius(int newRadius) {
        circleRadius = newRadius;
    }

    private int getRandomCirclingDir() {
        int d = Math.abs(RobotPlayer.rand.nextInt()) % 8;
        if (d % 2 == 1) {
            d--;
        }

        return d;
    }

    private boolean reverse(MapLocation next) throws GameActionException {
        // if the next spot is off the map or occupied by another of my type reverse my direction
        RobotInfo botAtNext = rc.senseRobotAtLocation(next);
        if (!rc.onTheMap(next) || (botAtNext != null && botAtNext.type == RobotPlayer.rt)) {
            circlingDir = RobotPlayer.directions[circlingDir].opposite().ordinal();
            circlingCW = !circlingCW;
            return true;
        }
        return false;
    }

    public boolean circle(RobotInfo[] _nearbyEnemies, MapLocation _myLocation) throws GameActionException {
        nearbyEnemies = _nearbyEnemies;
        myLocation = _myLocation;

        // try to move outward from circleDir
        int reverseDir = circlingCW ? (circlingDir - 1 + 8) % 8 : (circlingDir + 1) % 8;
        MapLocation next = myLocation.add(RobotPlayer.directions[reverseDir]);
        if (next.distanceSquaredTo(rallyPoint) <= circleRadius) {
            if (!this.reverse(next)) {
                return rc.isCoreReady() && Utils.moveInDirToLeastDamage(nearbyEnemies, myLocation, RobotPlayer.directions[reverseDir]);
            }
        }

        // try to move in the direction of circleDir
        next = myLocation.add(RobotPlayer.directions[circlingDir]);
        if (this.reverse(next)) {
            next = myLocation.add(RobotPlayer.directions[circlingDir]);
        }
        if (next.distanceSquaredTo(rallyPoint) <= circleRadius && rc.onTheMap(next)) {
            return rc.isCoreReady() && Utils.moveInDirToLeastDamage(nearbyEnemies, myLocation, RobotPlayer.directions[circlingDir]);
        }

        // try to move inward one from circleDir
        int nextDir = circlingCW ? (circlingDir + 1) % 8 : (circlingDir - 1 + 8) % 8;
        next = myLocation.add(RobotPlayer.directions[nextDir]);
        if (this.reverse(next)) return false;
        if (next.distanceSquaredTo(rallyPoint) <= circleRadius && rc.onTheMap(next)) {
            return rc.isCoreReady() && Utils.moveInDirToLeastDamage(nearbyEnemies, myLocation, RobotPlayer.directions[nextDir]);
        }

        // try to move inward two from circleDir
        // if we do this change circleDir to the direction we will be going
        nextDir = circlingCW ? (circlingDir + 2) % 8 : (circlingDir - 2 + 8) % 8;
        next = myLocation.add(RobotPlayer.directions[nextDir]);
        if (this.reverse(next)) return false;
        if (next.distanceSquaredTo(rallyPoint) <= circleRadius && rc.onTheMap(next)) {
            circlingDir = nextDir;
            return rc.isCoreReady() && Utils.moveInDirToLeastDamage(nearbyEnemies, myLocation, RobotPlayer.directions[circlingDir]);
        }

        return rc.isCoreReady() && Utils.moveInDirToLeastDamage(nearbyEnemies, myLocation, myLocation.directionTo(rallyPoint));
    }

}