package util;

import java.io.*;
import java.nio.file.*;
import java.security.*;

public class HashUtils {

    public static String quickHash(Path p, int readSize) {

        try {
            long size = Files.size(p);

            try {
                // xxHash32 사용 (라이브러리 존재 시)
                return size + ":" + xxHash32(p, readSize);
            } catch (Throwable ignore) {}

            // Fallback SHA-256
            return size + ":" + sha256(p, readSize);

        } catch (Exception e) {
            return null;
        }
    }

    private static int xxHash32(Path p, int readSize) throws Exception {
        byte[] buf = new byte[readSize];
        InputStream in = Files.newInputStream(p);
        int r = in.read(buf);
        in.close();
        if (r < 0) r = 0;

        net.jpountz.xxhash.XXHashFactory factory =
                net.jpountz.xxhash.XXHashFactory.fastestInstance();
        return factory.hash32().hash(buf, 0, r, 0);
    }

    private static String sha256(Path p, int readSize) throws Exception {
        byte[] buf = new byte[readSize];
        InputStream in = Files.newInputStream(p);
        int r = in.read(buf);
        in.close();

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(buf, 0, r <= 0 ? 0 : r);

        byte[] dig = md.digest();
        StringBuilder sb = new StringBuilder(dig.length * 2);
        for (byte b : dig) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
