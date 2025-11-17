package core;

import java.io.File;

public class FailClassifier {

    public static File getFailedFolder(File base, String reason) {

        String folder;

        switch (reason) {
            case "read-fail":    folder = "READ_FAIL"; break;
            case "convert-fail": folder = "CONVERT_FAIL"; break;
            case "error":        folder = "ERROR"; break;
            default:             folder = "UNKNOWN"; break;
        }

        File out = new File(base, folder);
        out.mkdirs();
        return out;
    }
}
