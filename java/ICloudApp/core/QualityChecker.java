package core;

import config.AppConfig;
import util.ImageUtils;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;

public class QualityChecker {

    public static class Result {
        public final boolean ok;
        public final String reason;

        public Result(boolean ok, String r) {
            this.ok = ok;
            this.reason = r;
        }
    }

    public Result check(Path path) {

        BufferedImage img = ImageUtils.safeRead(path.toFile());
        if (img == null) return new Result(false, "read-fail");

        int w = img.getWidth();
        int h = img.getHeight();

        if (w < AppConfig.MIN_WIDTH || h < AppConfig.MIN_HEIGHT)
            return new Result(false, "too-small");

        long sum = 0, sumSq = 0;
        int count = 0;

        int stepX = Math.max(1, w / 32);
        int stepY = Math.max(1, h / 32);

        for (int y = 0; y < h; y += stepY) {
            for (int x = 0; x < w; x += stepX) {
                int rgb = img.getRGB(x, y);
                int g = ((rgb>>16)&0xff + (rgb>>8)&0xff + (rgb&0xff)) / 3;
                sum += g;
                sumSq += (long)g * g;
                count++;
            }
        }

        double mean = sum / (double)count;
        double var  = sumSq / (double)count - mean*mean;

        if (var < AppConfig.MONO_VARIANCE)
            return new Result(false, "mono");

        return new Result(true, "ok");
    }
}
