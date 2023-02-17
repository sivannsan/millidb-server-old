package com.sivannsan.millidb;

import com.sivannsan.foundation.annotation.Nonnull;
import com.sivannsan.foundation.utility.DateUtility;
import com.sivannsan.foundation.utility.FileUtility;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Log on console and store them in the files in logs directory
 */
public final class MilliDBLogs {
    @Nonnull
    private static final File directory = new File("logs");
    private static File file;
    private static int newLines = 0;
    @Nonnull
    private static final List<String> lines = new ArrayList<>();

    public static void makeNewFile() {
        if (!directory.exists()) directory.mkdirs();
        file = new File(directory, newDate() + ".log");
        FileUtility.createFile(file);
    }

    public static void save() {
        new Thread(() -> {
            FileUtility.write(file, String.join("\n", new ArrayList<>(lines)));
            if (lines.size() >= 10000) {
                lines.clear();
                makeNewFile();
            }
        }).start();
    }

    private static void log(String message) {
        System.out.println(message);
        lines.add(message);
        newLines++;
        if (newLines >= 100) {
            save();
            newLines = 0;
        }
    }

    public static void info(String message) {
        log("[" + newDate() + "] [INFO]: " + message);
    }

    public static void warning(String message) {
        log("[" + newDate() + "] [WARNING]: " + message);
    }

    @Nonnull
    private static String newDate() {
        return DateUtility.toString(new Date(), "yyyyMMddHHmmssSSS");
    }
}
