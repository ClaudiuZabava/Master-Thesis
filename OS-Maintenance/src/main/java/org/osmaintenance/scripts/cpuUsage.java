package org.osmaintenance.scripts;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HardwareAbstractionLayer;

public class cpuUsage {

    // Method to get the CPU usage percentage considering multiple cores
    public static double getCpuUsagePercentage() throws InterruptedException {
        // Initialize SystemInfo object and retrieve hardware details
        SystemInfo systemInfo = new SystemInfo();
        HardwareAbstractionLayer hardware = systemInfo.getHardware();
        CentralProcessor processor = hardware.getProcessor();

        double cpuUsage = processor.getSystemCpuLoad(500)*100;

        return cpuUsage;
    }
}