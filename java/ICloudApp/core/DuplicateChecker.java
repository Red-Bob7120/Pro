package core;

import util.HashUtils;

import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

public class DuplicateChecker {

    private final ConcurrentHashMap<String, Path> hashMap = new ConcurrentHashMap<>();
    private final int readSize;

    public DuplicateChecker(int readSize) {
        this.readSize = readSize;
    }

    public boolean isDuplicate(Path p) {
        String key = HashUtils.quickHash(p, readSize);
        if (key == null) return false;

        return hashMap.putIfAbsent(key, p) != null;
    }
}
