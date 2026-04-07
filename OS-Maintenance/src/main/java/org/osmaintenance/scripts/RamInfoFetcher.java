package org.osmaintenance.scripts;

import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.PhysicalMemory;
import java.util.ArrayList;
import java.util.List;

public class RamInfoFetcher {

    private static double totalMemoryGB;

    public static List<String> retrieveRamData() {
        SystemInfo systemInfo = new SystemInfo();
        HardwareAbstractionLayer hardware = systemInfo.getHardware();

        // Get total memory in bytes and convert to GB
        long totalMemoryBytes = hardware.getMemory().getTotal();
        double totalMemoryGB = totalMemoryBytes / (1024.0 * 1024.0 * 1024.0);

        // Get RAM base speed (from first physical memory module)
        PhysicalMemory firstMemoryModule = hardware.getMemory().getPhysicalMemory().get(0);
        double ramBaseSpeedMHz = firstMemoryModule.getClockSpeed();
        double ramBaseSpeedGHz = ramBaseSpeedMHz / 1000000.0; // Convert to GHz

        // Get RAM model/type (from the first physical memory module)
        String ramModel = firstMemoryModule.getManufacturer() + " " + firstMemoryModule.getMemoryType();

        // Prepare and return the list of RAM data
        List<String> ramInfo = new ArrayList<>();
        ramInfo.add(ramModel);
        ramInfo.add(String.format("%.2f GB", totalMemoryGB));
        ramInfo.add(String.format("%.2f GHz", ramBaseSpeedGHz));


        return ramInfo;
    }

    public static double getTotalRam() {
        SystemInfo systemInfo = new SystemInfo();
        HardwareAbstractionLayer hardware = systemInfo.getHardware();
        long totalMemoryBytes = hardware.getMemory().getTotal();
        totalMemoryGB = totalMemoryBytes / (1024.0 * 1024.0 * 1024.0); // Convert to GB
        return totalMemoryGB;
    }

    public static double getAvailableRam() {
        SystemInfo systemInfo = new SystemInfo();
        HardwareAbstractionLayer hardware = systemInfo.getHardware();
        long availableMemoryBytes = hardware.getMemory().getAvailable();
        return availableMemoryBytes / (1024.0 * 1024.0 * 1024.0); // Convert to GB
    }

}
