package util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

public class ImageUtils {

    public static BufferedImage safeRead(File f) {
        try {
            BufferedImage img = ImageIO.read(f);
            if (img == null) return null;
            return img;
        } catch (Exception e) {
            return null;
        }
    }
}
