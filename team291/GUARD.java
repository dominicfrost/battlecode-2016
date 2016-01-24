package team291;

import battlecode.common.*;

public class GUARD {

    static RobotController rc;
    static MapLocation myLocation;
    static RobotInfo[] hostileRobots;
    static RobotInfo[] hostileRobotsInAttackRange;

    private static MapLocation rallyPoint;

    private static GuardStates state = GuardStates.AGGRO;

    private static enum GuardStates {
        AGGRO,
        HURT
    }

    public static void doTurn() throws GameActionException {
        myLocation = rc.getLocation();
        hostileRobots = rc.senseHostileRobots(rc.getLocation(), RobotPlayer.rt.sensorRadiusSquared);
        hostileRobotsInAttackRange = rc.senseHostileRobots(rc.getLocation(), RobotPlayer.rt.attackRadiusSquared);

        if (state != GuardStates.HURT && rc.getHealth() < RobotPlayer.rt.maxHealth / 4) {
            state = GuardStates.HURT;
        } else if (state != GuardStates.AGGRO && rc.getHealth() == RobotPlayer.rt.maxHealth){
            state = GuardStates.AGGRO;
        }

//        Utils.getDistressSignal();

        // TODO: Clear that debris

        if (fleeSinceWeekAndEnemiesNearby()) return;
        if (moveToRallySinceWeak()) return;
        if (attackSinceEnemiesNearby()) return;
        if (advanceSinceEnemiesSensed()) return;
        if (clearNearbyRubble()) return;
        // TODO: move towards distress call
        patrolPerimeter();
    }

    public static boolean fleeSinceWeekAndEnemiesNearby() throws GameActionException {
        if (hostileRobotsInAttackRange.length > 0 && state == GuardStates.HURT) {
            Direction toMove = Utils.flee(rc, hostileRobots, myLocation);
            if (toMove != Direction.NONE) {
                rc.move(toMove);
                return true;
            }
        }
        return false;
    }

    public static boolean moveToRallySinceWeak() throws GameActionException {
        if (state == GuardStates.HURT) {
            // move towards rally
            Utils.tryMove(myLocation.directionTo(rallyPoint));
            return true;
        }
        return false;
    }

    public static boolean attackSinceEnemiesNearby() throws GameActionException {
        if (hostileRobotsInAttackRange.length > 0) {
            if (rc.isWeaponReady()) {
                rc.attackLocation(hostileRobotsInAttackRange[0].location);
            }
            return true;
        }
        return false;
    }

    public static boolean advanceSinceEnemiesSensed() throws GameActionException {
        if (hostileRobots.length > 0) {
            int closestDistance  = 999999;
            RobotInfo closestEnemy = hostileRobots[0];
            for (RobotInfo enemyRobot: hostileRobots) {
                int distanceToRobot = myLocation.distanceSquaredTo(enemyRobot.location);
                if (distanceToRobot < closestDistance) {
                    closestDistance = distanceToRobot;
                    closestEnemy = enemyRobot;
                }
            }
            return Utils.moveThrough(myLocation, myLocation.directionTo(closestEnemy.location));
        }
        return false;
    }

    public static boolean clearNearbyRubble() throws GameActionException {
        RobotController rc = RobotPlayer.rc;

        int offsetIndex = 0;
        int[] offsets = {0,1,-1,2,-2,-3,3,4};
        int dirint = Utils.directionToInt(Direction.NORTH);
        while (offsetIndex < 7 && (rc.senseRubble(myLocation.add(RobotPlayer.directions[(dirint+offsets[offsetIndex]+8)%8])) <= 50)) {
            offsetIndex++;
        }
        if (offsetIndex < 5) {
            rc.clearRubble(RobotPlayer.directions[(dirint + offsets[offsetIndex] + 8) % 8]);
            return true;
        }

        return false;
    }

    public static boolean patrolPerimeter() throws GameActionException {
        return Utils.tryMoveWithinPerimeter(rallyPoint, Utils.getRandomDirection()) || Utils.tryMove(myLocation.directionTo(rallyPoint));
    }

    public static void execute() {
        rc = RobotPlayer.rc;

//        Signal signal = rc.readSignal();
//        if (signal.getMessage()[0] == Utils.MessageType.RALLY_LOCATION.ordinal()) {
//            rallyPoint = Utils.deserializeMapLocation(signal.getMessage()[1]);
//        } else {
//            rallyPoint = rc.getLocation();
//            System.out.println("UHHHH WTF?!? HOW IS THE FIRST SIGNAL NOT A RALLY_LOCATION SIGNAL?!?!");
//        }
        rallyPoint = Utils.getRallyLocation();


        while (true) {
            try {
                if (rc.isCoreReady()) {
                    doTurn();
                } else {
                    // do shit that doesn't need core delay stuff
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
            Clock.yield();
        }
    }
}
