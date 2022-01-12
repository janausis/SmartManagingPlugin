package germany.jannismartensen.smartmanaging.utility;

import static germany.jannismartensen.smartmanaging.utility.Util.log;

public class RenderLogger {
    public static void start() {
        breaker();
        log("|              Block             |     Status     |  Rendered As |    Time    |", 1);
        breaker();
    }

    public static void breaker() {
        log("+--------------------------------+----------------+--------------+------------+", 1);
    }

    public static void end(String pics, String time) {
        time = getFormattedString(time, 16);
        pics = getFormattedString(pics, 12);
        breaker();
        log("|          Total time:           |"   + time +   "|   Pictures:  |" + pics + "|", 1);
        breaker();
    }

    public static void add(String block, boolean status, String as, String time, int logStatus) {
        block = getFormattedString(block, 32);
        as = getFormattedString(as, 14);
        time = getFormattedString(time, 12);

        String stat;
        if (status) {
            stat = "   Successful   ";
        } else {
            stat = "     Failed     ";
        }

        log("|" + block + "|" + stat + "|" + as + "|" + time + "|", logStatus);
    }

    public static String getFormattedString(String string, int width) {
        if (string.length() > width-2) string = string.substring(0, width-2);
        int stringSpacingLeft = (int) Math.floor((width - string.length())/2.0);
        int stringSpacingRight = stringSpacingLeft;
        while (stringSpacingRight + stringSpacingLeft + string.length() < width) {
            stringSpacingLeft ++;
        }

        return " ".repeat(Math.max(0, stringSpacingLeft)) +
                string.trim() +
                " ".repeat(Math.max(0, stringSpacingRight));
    }
}
