package org.osmaintenance.scripts;

import com.sun.jna.WString;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
public class SystemErrLogScript {
    private static final List<Path> scanDirs = new ArrayList<>();
    private static final Set<Path> errorLogFiles = new HashSet<>();
    private static final Set<Path> errorLogDirs = new HashSet<>();
    private static final Set<Path> deleteLater = new HashSet<>();

    private static boolean scheduleDelete = false;
    private static long totalSize = 0;
    private static String sizeValue = "0";

    // ===============================================================================================
    // Locate Windows Error/System Log Directories
    // ===============================================================================================
    private static void locateErrorLogDirs() {
        scanDirs.clear();

        String localAppData = System.getenv("LOCALAPPDATA");
        String programData = System.getenv("ProgramData");

        if (localAppData != null) {
            scanDirs.add(Paths.get(localAppData, "Microsoft", "Windows", "WER")); // per-user crash reports
        }

        if (programData != null) {
            scanDirs.add(Paths.get(programData, "Microsoft", "Windows", "WER")); // system-wide crash reports
        }
    }

    // ===============================================================================================
    // Scan for Error Log Files
    // ===============================================================================================
    public static void scanSystemErrorLogs() {
        locateErrorLogDirs();
        errorLogFiles.clear();
        deleteLater.clear();
        scheduleDelete = false;
        totalSize = 0;
        sizeValue = "0 B";

        if (scanDirs.isEmpty()) {
            System.out.println("No directories to scan for system error logs.");
            return;
        }

        for (Path dir : scanDirs) {
            if (Files.exists(dir) && Files.isDirectory(dir)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                    for (Path entry : stream) {
                        if (Files.isRegularFile(entry, LinkOption.NOFOLLOW_LINKS)) {
                            totalSize+= Files.size(entry);
                            errorLogFiles.add(entry);
                        } else if (Files.isDirectory(entry, LinkOption.NOFOLLOW_LINKS)) {
                            errorLogDirs.add(entry);
                            Files.walkFileTree(entry, new SimpleFileVisitor<Path>() {

                                @Override
                                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                                    try {
                                        totalSize+= Files.size(file);
                                        errorLogFiles.add(file);
                                    } catch (Exception e) {
                                        System.err.println("Failed to read file: " + file);
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
                } catch (Exception Ex) {
                    System.err.println("Error listing files in Temp: " + Ex.getMessage());
                }
                errorLogDirs.add(dir);
            }
        }

        if (!errorLogFiles.isEmpty()) {
            sizeValue = humanReadableByteCount(totalSize);
            System.out.println("Scan complete. Found " + errorLogFiles.size() + " system error logs. Total size: " + sizeValue);
            errorLogFiles.forEach(file -> System.out.println(" - " + file));
        } else {
            System.out.println("No system error logs found.");
            sizeValue = "0 B";
        }
    }


    // ===============================================================================================
    // Clean Error Log Files
    // ===============================================================================================
    public static void cleanSystemErrorLogs() {
        for (Path file : errorLogFiles) {
            try {
                Files.deleteIfExists(file);
                System.out.println("Deleted: " + file);
            } catch (IOException e) {
                deleteLater.add(file);
                scheduleDelete = true;
                System.err.println("Failed to delete: " + file);
            }
        }

        for (Path dir : errorLogDirs) {
            try {
                if (Files.exists(dir)) {
                    deleteDirectoryContentsSilently(dir);
                }
            } catch (Exception e) {
                deleteLater.add(dir);
                scheduleDelete = true;
                System.out.println("One or more directories could not be cleaned!: " + dir);
            }
        }

        if (scheduleDelete) {
            System.out.println("Some files could not be deleted and will be scheduled for deletion on reboot:");
            for (Path file : deleteLater) {
                try {
                    int MOVEFILE_DELAY_UNTIL_REBOOT = 0x00000004;
                    boolean scheduled = WindowsNativeAPI.INSTANCE.MoveFileExW(new WString(file.toString()), null, MOVEFILE_DELAY_UNTIL_REBOOT);
                    System.out.println("Scheduled on reboot: " + file + " => " + scheduled);
                } catch (Exception e) {
                    System.err.println("Failed to schedule deletion for: " + file + " | Error: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Recursively deletes everything _inside_ the given directory (but not the directory itself),
     * catching and suppressing any exceptions so that no dialog ever appears.
     * Any path that cannot be deleted gets added to 'failedDeletes'.
     */
    private static void deleteDirectoryContentsSilently(Path directory) {
        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                // Delete files as we encounter them
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    try {
                        Files.deleteIfExists(file);
                    } catch (IOException e) {
                        if(!directory.equals(scanDirs.get(0)) && !directory.equals(scanDirs.get(1))) {
                            deleteLater.add(directory);
                        }
                        deleteLater.add(file);
                        scheduleDelete = true;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    // We do NOT delete the root Temp directory itself; only its contents
                    if (!dir.equals(directory)) {
                        try {
                            Files.deleteIfExists(dir);
                        } catch (IOException e) {
                            deleteLater.add(dir);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                // If we can’t even enter a directory, record it and continue
                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    deleteLater.add(file);
                    scheduleDelete = true;
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException walkEx) {
            // This usually won't happen—if it does, record the root folder as “failed”
            deleteLater.add(directory);
            scheduleDelete = true;
        }
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

    private static String humanReadableByteCount(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
