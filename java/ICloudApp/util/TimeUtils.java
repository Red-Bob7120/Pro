package util;

public class TimeUtils {

    public static String eta(double remainSec) {
        long s = (long)remainSec;
        long h = s / 3600; s%=3600;
        long m = s / 60;   s%=60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }
}
