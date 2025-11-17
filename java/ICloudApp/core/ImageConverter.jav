package core;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class ImageConverter {

    public boolean convertToJpg(File src, File dest) {

        try {
            return tryTurbo(src, dest);
        } catch (Throwable ignore) {}

        return fallback(src, dest);
    }

    private boolean fallback(File src, File dest) {
        try {
            BufferedImage img = ImageIO.read(src);
            if (img == null) return false;
            return ImageIO.write(img, "jpg", dest);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean tryTurbo(File src, File dest) throws Exception {
        org.libjpegturbo.turbojpeg.TJCompressor tj =
                new org.libjpegturbo.turbojpeg.TJCompressor(src.getAbsolutePath());

        byte[] jpeg = tj.compress(85);
        tj.close();

        java.nio.file.Files.write(dest.toPath(), jpeg);
        return true;
    }
}
