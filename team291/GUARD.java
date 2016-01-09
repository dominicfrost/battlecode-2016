package team291;

import battlecode.common.*;

public class GUARD {

    static RobotController rc;

    public static void doTurn() throws GameActionException {
        MapLocation myLocation = rc.getLocation();
        RobotInfo[] enemyRobots = rc.senseNearbyRobots(rc.getLocation(), RobotPlayer.rt.sensorRadiusSquared, RobotPlayer.enemyTeam);
        RobotInfo[] enemyRobotsInAttackRange = rc.senseNearbyRobots(rc.getLocation(), RobotPlayer.rt.attackRadiusSquared, RobotPlayer.enemyTeam);


//        Utils.getDistressSignal();

        // TODO: Clear that debris

        // first, flee if health is low
        if (rc.getHealth() < RobotPlayer.rt.maxHealth / 4) {
            Direction toMove = Utils.flee(rc, enemyRobots, myLocation);
            if (toMove != Direction.NONE) {
                rc.move(toMove);
                return;
            }
        }

        // second, attack if enemies nearby
        if (enemyRobotsInAttackRange.length > 0) {
            rc.attackLocation(enemyRobotsInAttackRange[0].location);
            return;
        }

        // third move towards closes enemy
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
            return;
        }

        // fourth, move towards distress call
//        if () {
//
//        }

        // fifth, move randomly
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
