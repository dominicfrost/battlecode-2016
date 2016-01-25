package team291;

import battlecode.common.*;

import java.util.ArrayDeque;


public class GUARD {

    static RobotController rc;
    static MapLocation myLocation;
    static RobotInfo[] hostileRobots;
    static RobotInfo[] hostileRobotsInAttackRange;

    public static Circle circler;

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

        if (state != GuardStates.HURT && rc.isInfected() && rc.getHealth() < RobotPlayer.rt.maxHealth / 4) {
            state = GuardStates.HURT;
        } else if (state != GuardStates.AGGRO && (!rc.isInfected() || rc.getHealth() == RobotPlayer.rt.maxHealth)){
            state = GuardStates.AGGRO;
        }

//        Utils.getDistressSignal();

        // TODO: Clear that debris
        if (fleeSinceWeekAndEnemiesNearby()) return;
        if (moveToRallySinceWeak()) return;
        if (attackSinceEnemiesNearby()) return;
        if (advanceSinceEnemiesSensed()) return;
        if (moveToAOI()) return;
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
        double distToRally;
        MapLocation potential;
        while (offsetIndex < 8 ) {//&& (rc.senseRubble() <= 50)) {
            potential = myLocation.add(RobotPlayer.directions[(dirint+offsets[offsetIndex]+8)%8]);
            distToRally = potential.distanceSquaredTo(rallyPoint);
            if (rc.onTheMap(potential) && distToRally <= Utils.distanceSquaredToPerimeter() && rc.senseRubble(potential) > 0) break;
            offsetIndex++;
        }
        if (offsetIndex < 8) {
            rc.clearRubble(RobotPlayer.directions[(dirint + offsets[offsetIndex] + 8) % 8]);
            return true;
        }

        return false;
    }

    public static boolean patrolPerimeter() throws GameActionException {
        circler.setCircleRadius(Utils.distanceSquaredToPerimeter());
        return circler.circle(hostileRobots, myLocation);
    }

    public static boolean moveToAOI() throws GameActionException {
        ArrayDeque<Signal> scoutSignals = Utils.getScoutSignals(rc.emptySignalQueue());
        int[] msg;

        double closestAOIDist = 999999;
        MapLocation closestAOI = null;
        double distToAOI;
        MapLocation aoi;

        for (Signal s: scoutSignals) {
            msg = s.getMessage();
            if (msg[0] == Utils.MessageType.DEN.ordinal() || msg[0] == Utils.MessageType.ENEMY.ordinal()) {
                aoi = Utils.deserializeMapLocation(msg[1]);
                distToAOI = myLocation.distanceSquaredTo(aoi);
                if (distToAOI < closestAOIDist) {
                    closestAOIDist = distToAOI;
                    closestAOI = aoi;
                }
            }
        }

        return closestAOI != null && Utils.moveThrough(myLocation, myLocation.directionTo(closestAOI));
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

        circler = new Circle(rallyPoint, RobotPlayer.rt.sensorRadiusSquared);
        while (true) {
            try {
                if (rc.isCoreReady()) {
                    doTurn();
                } else {
                    // do shit that doesn't need core delay stuff
                    rc.emptySignalQueue();
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
            Clock.yield();
        }
    }
}
