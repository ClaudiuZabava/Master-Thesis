package org.osmaintenance.scripts;

import com.sun.jna.WString;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class MemoryDumpScript {

    private static final List<String> dumpFilePatterns = List.of("*.dmp", "*.mdmp", "memory.dmp");
    private static final Set<Path> scanDirs = new HashSet<>();
    private static final Set<Path> dumpFiles = new HashSet<>();
    private static final Set<Path> deleteLater = new HashSet<>();
    private static boolean scheduleDelete = false;
    private static String sizeValue = "0";
    private static long totalSize = 0;

    //===============================================================================================
    //                                Locate Dump File Directories
    //===============================================================================================
    private static void locateDumpDirs() {
        scanDirs.clear();

        String systemRoot = System.getenv("SystemRoot");
        String localAppData = System.getenv("LOCALAPPDATA");

        if (systemRoot != null) {
            scanDirs.add(Paths.get(systemRoot));
            scanDirs.add(Paths.get(systemRoot, "Minidump"));
        }

        if (localAppData != null) {
            scanDirs.add(Paths.get(localAppData, "CrashDumps"));
        }
    }

    //===============================================================================================
    //                                Scan for Dump Files
    //===============================================================================================
    public static void scanMemoryDumps() {
        locateDumpDirs();
        dumpFiles.clear();
        deleteLater.clear();
        scheduleDelete = false;
        totalSize = 0;
        sizeValue = "0 B";

        for (Path dir : scanDirs) {
            if (Files.exists(dir) && Files.isDirectory(dir)) {
                try {
                    Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            for (String pattern : dumpFilePatterns) {
                                if (file.getFileName().toString().toLowerCase().endsWith(pattern.toLowerCase().replace("*", ""))) {
                                    try {
                                        totalSize += Files.size(file);
                                        dumpFiles.add(file);
                                    } catch (IOException e) {
                                        System.err.println("Failed to read dump file: " + file);
                                    }
                                    break;
                                }
                            }
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path file, IOException exc) {
                            return FileVisitResult.CONTINUE;
                        }
                    });
                } catch (IOException e) {
                    System.err.println("Failed to walk directory: " + dir);
                }
            }
        }

        if (!dumpFiles.isEmpty()) {
            sizeValue = humanReadableByteCount(totalSize);
            System.out.println("Memory dump scan complete. Found " + dumpFiles.size() + " files, total size: " + sizeValue);

        } else {
            System.out.println("No memory dump files found.");
            sizeValue = "0 B";
        }
    }

    //===============================================================================================
    //                                Delete Dump Files
    //===============================================================================================
    public static void cleanMemoryDumps() {
        for (Path file : dumpFiles) {
            try {
                Files.deleteIfExists(file);
                System.out.println("Deleted: " + file);
            } catch (IOException e) {
                deleteLater.add(file);
                scheduleDelete = true;
                System.err.println("Failed to delete: " + file);
            }
        }

        if (scheduleDelete) {
            System.out.println("Some files will be scheduled for deletion on reboot:");
            for (Path p : deleteLater) {
                try {
                    int MOVEFILE_DELAY_UNTIL_REBOOT = 0x00000004;
                    boolean scheduled = WindowsNativeAPI.INSTANCE.MoveFileExW(new WString(p.toString()), null, MOVEFILE_DELAY_UNTIL_REBOOT);
                    System.out.println(p + " scheduled for deletion: " + scheduled);
                } catch (UnsatisfiedLinkError e) {
                    System.err.println("WindowsNativeAPI not found: " + e.getMessage());
                } catch (Exception e) {
                    System.err.println("Failed to schedule for deletion: " + e.getMessage());
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
