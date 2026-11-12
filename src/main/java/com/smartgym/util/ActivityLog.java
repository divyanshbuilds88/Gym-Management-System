package com.smartgym.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * FILE HANDLING demo: writes important actions (login, add member ...) to a text file,
 * and can read the file back. File location:  <your home folder>/.smartgym/activity.log
 * Logging must never crash the program, so IO errors are ignored here.
 */
public final class ActivityLog {

    private static final Path LOG_FILE = Paths.get(System.getProperty("user.home"), ".smartgym", "activity.log");
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ActivityLog() { }

    public static synchronized void log(String message) {
        try {
            Files.createDirectories(LOG_FILE.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(LOG_FILE, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                writer.write(LocalDateTime.now().format(FORMAT) + " | " + message);
                writer.newLine();
            }
        } catch (IOException ignored) {
            // logging is optional
        }
    }

    /** Reads the last 'maxLines' lines of the log file (file READING demo). */
    public static synchronized List<String> readLast(int maxLines) {
        List<String> result = new ArrayList<>();
        try {
            if (Files.exists(LOG_FILE)) {
                List<String> all = Files.readAllLines(LOG_FILE, StandardCharsets.UTF_8);
                int from = Math.max(0, all.size() - maxLines);      // Math class
                result.addAll(all.subList(from, all.size()));
            }
        } catch (IOException ignored) {
            // return what we have
        }
        return result;
    }

    public static Path getLogFile() {
        return LOG_FILE;
    }
}
