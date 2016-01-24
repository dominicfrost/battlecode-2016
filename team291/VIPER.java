package team291;

import battlecode.common.*;

public class VIPER {

    public static RobotController rc;
    public static void doTurn() throws GameActionException {
        rc.disintegrate();
    }

    public static void execute() {
        rc = RobotPlayer.rc;
        while (true) {
            try {
                if (rc.isCoreReady()) {
                    doTurn();
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
            Clock.yield();
        }
    }
}
