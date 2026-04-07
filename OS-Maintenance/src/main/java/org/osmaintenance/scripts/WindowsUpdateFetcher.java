package org.osmaintenance.scripts;
import java.awt.Desktop;
import java.net.URI;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WindowsUpdateFetcher {

    public static String extractTitle(String input) {
        if (input == null) return null;
        int lastDash = input.lastIndexOf(" - ");
        if (lastDash == -1) return input;
        int secondLastDash = input.lastIndexOf(" - ", lastDash - 1);
        if (secondLastDash == -1) return input.substring(0, lastDash);
        return input.substring(0, secondLastDash);
    }

    public static String extractVersion(String input) {
        if (input == null) return null;
        int lastDash = input.lastIndexOf(" - ");
        if (lastDash == -1 || lastDash + 3 >= input.length()) return "";
        return input.substring(lastDash + 3).trim();
    }
    public static Map<Integer, List<String>> queryWindowsUpdates() {
        Map<Integer, List<String>> driver = new HashMap<>();
        try {
            String psCmd = "Get-WindowsUpdate -UpdateType Driver -MicrosoftUpdate | ForEach-Object { $_.Title }";

            ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-Command", psCmd);
            pb.redirectErrorStream(true);

            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            StringBuilder outputBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                outputBuilder.append(line).append("\n");
            }

            process.waitFor();
            process.destroy();

            String output = outputBuilder.toString();
            //System.out.println("Raw Output:\n" + output);


            int count = 0;
            for (String l : output.split("\n")) {
                if (l.trim().length() > 0 && l.contains(" - ")) {
                    String title = extractTitle(l);
                    String version = extractVersion(l);

                    if (title != null && !title.isEmpty()) {
                        if (!driver.containsKey(count)) {
                            driver.put(count, new ArrayList<>());
                        }
                        driver.get(count).add(title);
                        driver.get(count).add(version);
                        count++;
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

        return driver;
    }

    public static String generateLink(String title) {

        if (title == null || title == "") {
            System.err.println("No drivers found.");
            return "";
        }
        StringBuilder urlBuilder = new StringBuilder("https://www.catalog.update.microsoft.com/Search.aspx?q=");
        urlBuilder.append(title.replace(" ", "%20")).append("&scol=DateComputed&sdir=desc");

        return urlBuilder.toString();
    }

    public static void openDriverLink(String url)
    {
        try {
            //String url = "https://www.catalog.update.microsoft.com/Search.aspx?q=realtek&scol=DateComputed&sdir=desc";
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
                //System.out.println("Browser launched!");
            } else {
                //System.out.println("Desktop API is not supported on this system.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<String> getUpdateInfo(String model) {
        Process process = null;
        List<String> output = new ArrayList<>();
        try {
            // Adjust 'python' or 'python3' as needed for your environment
            ProcessBuilder pb = new ProcessBuilder("py", "src/main/java/org/osmaintenance/scripts/scrapper.py", model);
            pb.redirectErrorStream(true);
            process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            process.waitFor(); // Wait for the process to complete
            process.destroy(); // Clean up the process

            // Split the output, expecting tab-separated values
            if (line != null) {
                String[] parts = line.split("\t", -1);
                String current = parts.length > 0 ? parts[0] : null;
                String latest = parts.length > 1 ? parts[1] : null;
                String url = parts.length > 2 ? parts[2] : null;
                output.add(current);
                output.add(latest);
                output.add(url);
            }
            return output;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        } finally {
            // Ensure the process is destroyed
            if (process != null && process.isAlive()) {
                process.destroy();
            }
        }
    }

//    public static void main(String[] args) {
//
//        String gpuName = GpuInfoFetcher.retrieveGpuData().get(0);
//        if (gpuName.contains("NVIDIA")) {
//            gpuName = gpuName.replace("NVIDIA", "").trim();
//        }
//        System.out.println("Fetching updates for GPU: " + gpuName);
//        List<String> updateInfo = getUpdateInfo("RTX 3060");
//        if (updateInfo == null || updateInfo.size() >= 3) {
//            System.out.println("Current Version: " + updateInfo.get(0));
//            System.out.println("Latest Version: " + updateInfo.get(1));
//            System.out.println("Download Link: " + updateInfo.get(2));
//        } else {
//            System.out.println("Failed to retrieve update information.");
//        }
//    }
}
