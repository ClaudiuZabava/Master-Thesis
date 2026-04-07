package org.osmaintenance.scripts;

import com.sun.jna.WString;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.*;
import java.util.*;

public class BrowserCacheScript {
    private static String sizeValue = "0";
    private static long totalSize = 0;
    private static final Set<Path> cachePaths = new HashSet<>();
    private static final Set<Path> cacheFiles = new HashSet<>();
    private static final Set<Path> deleteLater = new HashSet<>();
    private static boolean scheduleDelete = false;

    //===============================================================================================
//                                  Browser Cache Targets
//===============================================================================================
    private static void locateCacheDirs() {

        cachePaths.clear();
        String localAppData = System.getenv("LOCALAPPDATA");
        String appData = System.getenv("APPDATA");

        if (localAppData == null || appData == null) {
            System.err.println("Could not resolve APPDATA paths.");
            return;
        }

        Map<String, String> browserCacheDirs = new HashMap<>() {{
            put("Chrome_local", localAppData + "\\Google\\Chrome\\User Data\\Default\\Cache");
            put("Chrome_roaming", appData + "\\Google\\Chrome\\User Data\\Default\\Cache");
            put("Edge_local", localAppData + "\\Microsoft\\Edge\\User Data\\Default\\Cache");
            put("Edge_roaming", appData + "\\Microsoft\\Edge\\User Data\\Default\\Cache");
            put("Firefox_local", localAppData + "\\Mozilla\\Firefox\\Profiles");
            put("Brave_local", localAppData + "\\BraveSoftware\\Brave-Browser\\User Data\\Default\\Cache");
            put("Brave_roaming", appData + "\\BraveSoftware\\Brave-Browser\\User Data\\Default\\Cache");
            put("Opera_local", localAppData + "\\Opera Software\\Opera Stable\\Cache");
            put("Opera_roaming", appData + "\\Opera Software\\Opera Stable\\Cache");
            put("OperaGX1_local", localAppData + "\\Opera Software\\Opera GX Stable\\Cache");
            put("OperaGX1_roaming", appData + "\\Opera Software\\Opera GX Stable\\Cache");
            put("OperaGX2_local", localAppData + "\\Opera Software\\Opera GX Stable\\System Cache");
            put("OperaGX2_roaming", appData + "\\Opera Software\\Opera GX Stable\\System Cache");
            put("Vivaldi_local", localAppData + "\\Vivaldi\\User Data\\Default\\Cache");
            put("Vivaldi_roaming", appData + "\\Vivaldi\\User Data\\Default\\Cache");
        }};

        for (Map.Entry<String, String> entry : browserCacheDirs.entrySet()) {
            Path path = Paths.get(entry.getValue());
            if (Files.exists(path)) {
                if (entry.getKey().equals("Firefox")) {
                    try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                        for (Path profile : stream) {
                            Path cache = profile.resolve("cache2\\entries");
                            if (Files.exists(cache)) cachePaths.add(cache);
                        }
                    } catch (IOException ignored) {}
                } else {
                    cachePaths.add(path);
                }
            }
        }
    }

    //===============================================================================================
//                                  Scan Cache Files
//===============================================================================================
    public static void scanBrowserCaches() {
        locateCacheDirs();
        cacheFiles.clear();
        totalSize = 0;
        sizeValue = "0 B";

        if (cachePaths.isEmpty()) {
            System.out.println("No browser caches found to scan.");
            return;
        }

        try
        {
            for (Path dir : cachePaths) {
                if (Files.exists(dir) && Files.isDirectory(dir)) {
                    Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            try {
                                totalSize += Files.size(file);
                                cacheFiles.add(file);
                            } catch (Exception e) {
                                System.err.println("Error reading: " + file);
                            }
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path file, IOException exc) {
                            return FileVisitResult.CONTINUE;
                        }
                    });
                }
            }

        }
        catch (IOException e) {
            System.err.println("Error scanning cache directories: " + e.getMessage());
            return;
        }

        if (cacheFiles.isEmpty()) {
            System.out.println("No cache files found in the specified directories.");
            sizeValue = "0 B";
            return;
        }

        sizeValue = humanReadableByteCount(totalSize);
        System.out.println("Scan completed. Total cache size: " + sizeValue);
    }

//===============================================================================================
//                                  Clean Cache Files
//===============================================================================================
    public static void cleanBrowserCaches() {
        for (Path file : cacheFiles) {
            try {
                Files.deleteIfExists(file);
                System.out.println("Deleted: " + file);
            } catch (IOException e) {
                deleteLater.add(file);
                System.out.println("File is in use: " + e.getMessage());
                scheduleDelete = true;
            }
        }

        if (deleteLater.isEmpty()) {
            System.out.println("All cache files deleted successfully.");
        } else {
            System.out.println("Some files could not be deleted now. Schedule on reboot if needed.");
            for (Path p : deleteLater) {
                try {
                    int MOVEFILE_DELAY_UNTIL_REBOOT = 0x00000004;
                    boolean deleteSchedule = WindowsNativeAPI.INSTANCE.MoveFileExW(new WString(p.toString()), null, MOVEFILE_DELAY_UNTIL_REBOOT);
                    // print the result of the move operation
                    System.out.println(p +  " File scheduled for deletion on reboot " + deleteSchedule);
                }
                catch (UnsatisfiedLinkError e) {
                    System.err.println("Failed to load WindowsNativeAPI: " + e.getMessage());
                } catch (Exception e) {
                    System.err.println("Error trying to move file for delayed deletion: " + e.getMessage());
                }
            }
        }
    }

//===============================================================================================
//                                Utility Methods
//===============================================================================================


    private static String humanReadableByteCount(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String units = "KMGTPE";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), units.charAt(exp - 1));
    }

    public static String getTotalSizeValue() {
        return sizeValue.split(" ")[0];
    }

    public static String getTotalSizeUnit() {
        return sizeValue.split(" ")[1];
    }

    public static boolean getScheduleDelete() {
        return scheduleDelete;
    }
}