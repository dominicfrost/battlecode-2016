package team291;

import java.util.ArrayList;
import battlecode.common.*;

public class Bug {
    static RobotController rc;
    static MapLocation goal = null;
    static ArrayList<MapLocation> currentMLine = null;
    static MapLocation myLocation = null;
    static MapLocation lastLocation = null;
    static MapLocation wallStartLocation = null;
    static Direction myDirection = null;
    static boolean movedClockwise = false;
    static Direction waitToGo = null;

    public enum BugState {
        NO_MLINE,
        ON_MLINE,
        ON_WALL,
        AT_GOAL
    };//if sense den and have good army don't bug

    private static BugState state = BugState.NO_MLINE;

    public static Direction startBuggin(MapLocation goal_in, MapLocation myLocation, int quitThresh) throws GameActionException {
        rc = RobotPlayer.rc;

        //if i changed goals or am not where i left off last start over
        if ((goal == null || lastLocation == null) || !goal.equals(goal_in) || !lastLocation.equals(myLocation)) {
            freshstart();
            goal = goal_in;
        }

        //im at the goal yo!
        if(myLocation.distanceSquaredTo(goal) <= quitThresh) {
            state = BugState.AT_GOAL;
            return Direction.OMNI;
        }

        //if we are back at where we started on the wall
        if (wallStartLocation != null && myLocation.equals(wallStartLocation)){
            freshstart();
            return Direction.OMNI;
        }

        Direction dirToMove = bugNextMove();

        //if its omni we are following the map bounds so restart
        if (dirToMove == Direction.OMNI) {
            freshstart();
            return dirToMove;
        }

        lastLocation = myLocation.add(dirToMove);

        if(lastLocation.distanceSquaredTo(goal) <= quitThresh) {
            state = BugState.AT_GOAL;
            return Direction.OMNI;
        } else if (currentMLine.contains(lastLocation)) {
            state = BugState.ON_MLINE;
        }

        myDirection = dirToMove;
        //Util.debug(rc, "MOVING  " + dirToMove.toString());
        return dirToMove;
    }

    public static void freshstart() {
        state = BugState.NO_MLINE;
        wallStartLocation = null;
        myDirection = null;
        currentMLine = null;
        waitToGo = null;
    }

    public static Direction bugNextMove() throws GameActionException {
        switch (state) {
            case NO_MLINE:
                calcMLine();
                return moveOnMLine();
            case ON_MLINE:
                return moveOnMLine();
            case ON_WALL:
                return followWall();
        }
        //System.out.println("HERERERE??????WHY?????????");
        return Direction.OMNI;
    }

    //get the next location on the mLine and try to move there
    public static Direction moveOnMLine() throws GameActionException {
        int myLocationIndex = currentMLine.indexOf(myLocation);
        //get the next location on the mLine and try to move there
        MapLocation nextLocation = currentMLine.get(myLocationIndex + 1);
        if (rc.isLocationOccupied(nextLocation)) return Direction.NONE;

        Direction nextLocationDir = myLocation.directionTo(nextLocation);
        myDirection = nextLocationDir;
        if (rc.canMove(nextLocationDir)) {
            return nextLocationDir;
        } else {
            return getHandOnWall();
        }
    }

    public static Direction getHandOnWall() throws GameActionException {
        Direction rightDir = myDirection;
        Direction leftDir = myDirection;
        while (true) {
            rightDir = rightDir.rotateRight();
            leftDir = leftDir.rotateLeft();

            if (rc.canMove(rightDir)) {
//                Util.debug(rc, "ON WALL RIGHT");
                state = BugState.ON_WALL;
                movedClockwise = true;
                wallStartLocation = myLocation.add(rightDir);
                return rightDir;
            }

            if (rc.canMove(leftDir)) {
//                Util.debug(rc, "ON WALL LFET");
                state = BugState.ON_WALL;
                movedClockwise = false;
                wallStartLocation = myLocation.add(rightDir);
                return leftDir;
            }
        }
    }

    public static Direction followWall() throws GameActionException {
        //if we can get back on the mline do it
        if (currentMLine.contains(myLocation.add(myDirection)) && rc.canMove(myDirection)) {
//            Util.debug(rc, "BACK ON MLINE");
            state = BugState.ON_MLINE;
            return myDirection;
        }

        Direction backInwards = rotateInDir(myDirection, movedClockwise);
        backInwards = rotateInDir(backInwards, movedClockwise);
        if (rc.canMove(backInwards)) {
            return backInwards;
        }
        backInwards = rotateInDir(myDirection, movedClockwise);
        if (rc.canMove(backInwards)) {
            return backInwards;
        }


        if (!rc.onTheMap(rc.getLocation().add(backInwards))) return Direction.OMNI;
        if (rc.canMove(myDirection)) return myDirection;

        while (true) {
            myDirection = rotateInDir(myDirection, !movedClockwise);
            if (rc.canMove(myDirection)) {
                return myDirection;
            }
        }
    }


    public static Direction rotateInDir(Direction startDir, boolean rotateLeft) throws GameActionException {
        if (rotateLeft) {
            return startDir.rotateLeft();
        } else {
            return startDir.rotateRight();
        }
    }

    public static void calcMLine() throws GameActionException {
        Direction dirToGoal;
        ArrayList<MapLocation> mLine = new ArrayList<>();// change to array??
        MapLocation currentLocation = myLocation;
        while (!currentLocation.equals(goal)) {
            mLine.add(currentLocation);
            dirToGoal = currentLocation.directionTo(goal);
            currentLocation = currentLocation.add(dirToGoal);
            System.out.print("CL ");
            System.out.println(currentLocation);
            System.out.print("G ");
            System.out.println(goal);
        }
        mLine.add(goal);

        currentMLine = mLine;
    }

}