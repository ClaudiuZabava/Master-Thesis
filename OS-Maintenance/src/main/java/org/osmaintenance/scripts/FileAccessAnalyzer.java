package org.osmaintenance.scripts;

import com.sun.jna.WString;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class FileAccessAnalyzer {

    public static Map<File, LocalDateTime> getLastAccessTimes(String folderPath) throws IOException {
        Map<File, LocalDateTime> accessMap = new HashMap<>();

        Files.walk(Paths.get(folderPath))
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    try {
                        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                        FileTime fileTime = attrs.lastAccessTime();
                        LocalDateTime accessTime = LocalDateTime.ofInstant(fileTime.toInstant(), ZoneId.systemDefault());
                        accessMap.put(path.toFile(), accessTime);
                    } catch (IOException e) {
                        System.err.println("Could not read attributes for file: " + path);
                    }
                });

        // Sort the map by date descending
        return accessMap.entrySet().stream()
                .sorted(Map.Entry.<File, LocalDateTime>comparingByValue())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    public static void deleteFile(Path file) {
        try {
            Files.deleteIfExists(file);
            System.out.println("Deleted: " + file);
        } catch (Exception e) {
            try {
                int MOVEFILE_DELAY_UNTIL_REBOOT = 0x00000004;
                boolean scheduled = WindowsNativeAPI.INSTANCE.MoveFileExW(new WString(file.toString()), null, MOVEFILE_DELAY_UNTIL_REBOOT);
                System.out.println("Scheduled on reboot: " + file + " => " + scheduled);
            } catch (Exception ex) {
                System.err.println("Failed to schedule deletion for: " + file + " | Error: " + ex.getMessage());
            }
        }
    }
}


