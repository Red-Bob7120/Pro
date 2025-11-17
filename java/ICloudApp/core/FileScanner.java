package core;

import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import static java.nio.file.FileVisitOption.*;
import static java.nio.file.Files.*;

public class FileScanner {

    private final Set<String> targets;

    public FileScanner(Set<String> validExts) {
        this.targets = validExts;
    }

    public List<Path> scan(File root) throws Exception {
        return walk(root.toPath(), FOLLOW_LINKS)
                .parallel()
                .filter(Files::isRegularFile)
                .filter(p -> {
                    String name = p.getFileName().toString().toLowerCase();
                    int dot = name.lastIndexOf(".");
                    if (dot < 0) return false;
                    return targets.contains(name.substring(dot + 1));
                })
                .collect(Collectors.toList());
    }
}
