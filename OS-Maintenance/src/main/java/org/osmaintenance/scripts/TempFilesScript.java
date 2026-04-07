package org.osmaintenance.scripts;

import com.sun.jna.WString;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TempFilesScript {

    private static String sizeValue = "0";
    private static long totalSize = 0;
    private static boolean scheduleDelete = false;
    private static List<Path> toScan = new ArrayList<>();
    private static final Set<Path> tempDirs = new HashSet<>();
    private static final Set<Path> tempFiles = new HashSet<>();
    private static final Set<Path> deleteLater = new HashSet<>();

//===============================================================================================
//                                Scan for Temp Files
//===============================================================================================

    private static void getTargets(){
        // 1. Verify we are on Windows
        String os = System.getProperty("os.name");
        if (os == null || !os.toLowerCase().contains("windows")) {
            System.out.println("This utility is intended to run on Windows only.");
            return;
        }

        // 2. Find %APPDATA% (which points to ...\Users\<User>\AppData\Roaming)
        String appData = System.getenv("APPDATA");
        if (appData == null) {
            System.out.println("Could not resolve %APPDATA% environment variable. Aborting.");
            return;
        }

        // 3. Compute the parent folder (AppData)
        Path roaming = Paths.get(appData);
        Path appDataRoot = roaming.getParent();
        if (appDataRoot == null) {
            System.out.println("Unexpected path structure: cannot find parent of " + roaming);
            return;
        }

        /// add ysroot windows Temp too
        String systemRoot = System.getenv("SystemRoot");
        Path sysRoot = systemRoot != null ? Paths.get(systemRoot) : null;
        if (sysRoot == null) {
            System.out.println("Could not resolve SystemRoot environment variable. Skipping system Temp directory.");
            return;
        }


        // 4. Build the two Temp‐folder paths we want to clean:
        Path local = appDataRoot.resolve("Local");
        Path localLow = appDataRoot.resolve("LocalLow");
        Path sysRootTemp = sysRoot.resolve("Temp");

        toScan = List.of(local.resolve("Temp"), localLow.resolve("Temp"), sysRootTemp);
    }

    public static void scanTempFiles() {
        getTargets();
        tempDirs.clear();
        tempFiles.clear();
        totalSize = 0;
        sizeValue = "0 B";


        if( toScan.isEmpty()) {
            System.out.println("No valid Temp directories found to scan.");
            return;
        }

        for (Path dirr : toScan) {
            if (Files.exists(dirr) && Files.isDirectory(dirr)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirr)) {
                    for (Path entry : stream) {
                        if (Files.isRegularFile(entry, LinkOption.NOFOLLOW_LINKS)) {
                            totalSize+= Files.size(entry);
                            tempFiles.add(entry);
                        } else if (Files.isDirectory(entry, LinkOption.NOFOLLOW_LINKS)) {
                            tempDirs.add(entry);
                            Files.walkFileTree(entry, new SimpleFileVisitor<Path>() {

                                @Override
                                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                                    try {
                                        totalSize+= Files.size(file);
                                        tempFiles.add(file);
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
                tempDirs.add(dirr);
            }
        }

        if (tempFiles.isEmpty()) {
            System.out.println("No junk files found in the specified directories.");
            sizeValue = "0 B";
            return;
        }
        sizeValue = humanReadableByteCount(totalSize);
        System.out.println("Scan completed. Total junk file size: " +sizeValue);

    }

//===============================================================================================
//                                Clear Temp Files
//===============================================================================================
    public static void cleanTempFiles()
    {
        for (Path file : tempFiles) {
            try {
                if (Files.exists(file)) {
                    Files.deleteIfExists(file);
                } else {
                    System.out.println("File does not exist: " + file);
                }
            } catch (Exception e) {
                deleteLater.add(file);
                scheduleDelete = true;
                System.out.println("One or more files could not be cleaned!: " + file);
            }
        }

        for (Path dir : tempDirs) {
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



        if(scheduleDelete){
            System.out.println("Some files could not be deleted:");
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
                        System.out.println("Cleaning: " + file);
                    } catch (IOException e) {
                        if(!directory.equals(toScan.get(0)) && !directory.equals(toScan.get(1))) {
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

    private static String humanReadableByteCount(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String units = "KMGTPE";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), units.charAt(exp - 1));
    }

    public static boolean getScheduleDelete()
    {
        return scheduleDelete;
    }
}
