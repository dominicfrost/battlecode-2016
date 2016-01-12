package team291;

import battlecode.common.*;

public class GUARD {

    static RobotController rc;
    static MapLocation myLocation;
    static RobotInfo[] enemyRobots;
    static RobotInfo[] enemyRobotsInAttackRange;

    public static void doTurn() throws GameActionException {
        myLocation = rc.getLocation();
        enemyRobots = rc.senseNearbyRobots(rc.getLocation(), RobotPlayer.rt.sensorRadiusSquared, RobotPlayer.enemyTeam);
        enemyRobotsInAttackRange = rc.senseNearbyRobots(rc.getLocation(), RobotPlayer.rt.attackRadiusSquared, RobotPlayer.enemyTeam);


//        Utils.getDistressSignal();

        // TODO: Clear that debris

        if (fleeSinceWeekAndEnemiesNearby()) return;
        if (moveToRallySinceWeak()) return;
        if (attackSinceEnemiesNearby()) return;
        if (advanceSinceEnemiesSensed()) return;
        // TODO: move towards distress call
        patrolPerimeter();
    }

    public static boolean fleeSinceWeekAndEnemiesNearby() throws GameActionException {
        if (enemyRobotsInAttackRange.length > 0) {
            Direction toMove = Utils.flee(rc, enemyRobots, myLocation);
            if (toMove != Direction.NONE) {
                rc.move(toMove);
                return true;
            }
        }
        return false;
    }

    public static boolean moveToRallySinceWeak() throws GameActionException {
        if (rc.getHealth() < RobotPlayer.rt.maxHealth / 4) {
            // move towards rally
            Direction toMove = Utils.flee(rc, enemyRobots, myLocation);
            if (toMove != Direction.NONE) {
                rc.move(toMove);
                return true;
            }
        }
        return false;
    }

    public static boolean attackSinceEnemiesNearby() throws GameActionException {
        if (rc.isWeaponReady() && enemyRobotsInAttackRange.length > 0) {
            rc.attackLocation(enemyRobotsInAttackRange[0].location);
            return true;
        }
        return false;
    }

    public static boolean advanceSinceEnemiesSensed() throws GameActionException {
        if (enemyRobots.length > 0) {
            int closestDistance  = 999999;
            RobotInfo closestEnemy = enemyRobots[0];
            for (RobotInfo enemyRobot: enemyRobots) {
                int distanceToRobot = myLocation.distanceSquaredTo(enemyRobot.location);
                if (distanceToRobot < closestDistance) {
                    closestDistance = distanceToRobot;
                    closestEnemy = enemyRobot;
                }
            }
            Utils.tryMove(myLocation.directionTo(closestEnemy.location));
            return true;
        }
        return false;
    }

    public static boolean patrolPerimeter() throws GameActionException {
        return false;
    }

    public static void execute() {
        rc = RobotPlayer.rc;

        while (true) {
            try {
                if (rc.isCoreReady()) {
                    doTurn();
                } else {
                    // do shit that doesn't need core delay stuff
                }
                Clock.yield();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
