package org.osmaintenance.scripts;

import com.sun.jna.WString;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.stream.Collectors;

public class DuplicateFileFinder {

    // -----------------------
    // OPTION 1: NAME SIMILARITY
    // -----------------------

    public static Map<Integer, List<File>> findDuplicatesByName(String folderPath, double threshold) throws IOException {
        List<File> files = Files.walk(Paths.get(folderPath))
                .filter(Files::isRegularFile)
                .map(Path::toFile)
                .collect(Collectors.toList());

        Map<Integer, List<File>> result = new HashMap<>();
        boolean[] visited = new boolean[files.size()];
        int groupId = 1;

        for (int i = 0; i < files.size(); i++) {
            if (visited[i]) continue;

            String name1 = stripExtension(files.get(i).getName()).toLowerCase();
            List<File> similarGroup = new ArrayList<>();
            similarGroup.add(files.get(i));
            visited[i] = true;

            for (int j = i + 1; j < files.size(); j++) {
                if (visited[j]) continue;

                String name2 = stripExtension(files.get(j).getName()).toLowerCase();
                double similarity = getSimilarity(name1, name2);
                if (similarity >= threshold) {
                    similarGroup.add(files.get(j));
                    visited[j] = true;
                }
            }

            if (similarGroup.size() > 1) {
                result.put(groupId++, similarGroup);
            }
        }
        return result;
    }

    private static String stripExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return (lastDot == -1) ? filename : filename.substring(0, lastDot);
    }

    private static double getSimilarity(String s1, String s2) {
        int distance = levenshtein(s1, s2);
        int maxLen = Math.max(s1.length(), s2.length());
        return maxLen == 0 ? 1.0 : 1.0 - (double) distance / maxLen;
    }

    private static int levenshtein(String s1, String s2) {
        int[] prev = new int[s2.length() + 1];
        int[] curr = new int[s2.length() + 1];

        for (int j = 0; j <= s2.length(); j++) prev[j] = j;

        for (int i = 1; i <= s1.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= s2.length(); j++) {
                int insert = curr[j - 1] + 1;
                int delete = prev[j] + 1;
                int replace = prev[j - 1] + (s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1);
                curr[j] = Math.min(insert, Math.min(delete, replace));
            }
            int[] temp = prev;
            prev = curr;
            curr = temp;
        }

        return prev[s2.length()];
    }

    // -----------------------
    // OPTION 2: CONTENT DUPLICATES
    // -----------------------

    public static Map<Integer, List<File>> findDuplicatesByContent(String folderPath) throws IOException, NoSuchAlgorithmException {
        Map<Long, List<File>> filesBySize = new HashMap<>();
        Map<String, List<File>> filesByHash = new HashMap<>();

        Files.walk(Paths.get(folderPath))
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    File file = path.toFile();
                    filesBySize.computeIfAbsent(file.length(), k -> new ArrayList<>()).add(file);
                });

        MessageDigest md = MessageDigest.getInstance("SHA-256");

        for (List<File> group : filesBySize.values()) {
            if (group.size() < 2) continue;

            for (File file : group) {
                String hash = getFileHash(file, md);
                if (hash != null) {
                    filesByHash.computeIfAbsent(hash, k -> new ArrayList<>()).add(file);
                }
            }
        }

        Map<Integer, List<File>> result = new HashMap<>();
        int groupId = 1;
        for (List<File> group : filesByHash.values()) {
            if (group.size() > 1) {
                result.put(groupId++, group);
            }
        }

        return result;
    }

    private static String getFileHash(File file, MessageDigest md) {
        try (InputStream is = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            md.reset();
            while ((bytesRead = is.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
            return Base64.getEncoder().encodeToString(md.digest());
        } catch (IOException e) {
            System.err.println("Failed to hash file: " + file.getAbsolutePath());
            return null;
        }
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

