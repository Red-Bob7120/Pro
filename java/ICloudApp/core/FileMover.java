package core;

import java.io.File;
import java.nio.file.*;

public class FileMover {

    public void move(File src, File dest) throws Exception {
        dest.getParentFile().mkdirs();
        try {
            Files.move(src.toPath(), dest.toPath(), StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception ex) {
            Files.move(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
