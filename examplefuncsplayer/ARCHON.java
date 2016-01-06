package examplefuncsplayer;

import battlecode.common.*;

public class ARCHON {

    public static void doTurn(RobotController rc) throws GameActionException {

    }

    public static void execute(RobotController rc) {
        while (true) {
            try {
                doTurn(rc);
                Clock.yield();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }
}