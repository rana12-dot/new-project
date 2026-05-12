package com.stubserver.backend.util;

import java.io.File;
import java.nio.file.Path;

public final class FilePathGuard {

    private FilePathGuard() {}

    public static boolean isPathInside(String baseDir, String targetPath) {
        Path base = Path.of(baseDir).toAbsolutePath().normalize();
        Path target = Path.of(targetPath).toAbsolutePath().normalize();
        return target.startsWith(base);
    }

    public static boolean isSafeFileName(String name) {
        if (name == null || name.isEmpty()) return false;
        return name.equals(new File(name).getName())
                && !name.contains("..")
                && !name.contains("/")
                && !name.contains("\\");
    }
}
