package org.osmaintenance.scripts;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import java.util.ArrayList;
import java.util.List;

public class CpuInfoFetcher {
    public static List<String> retrieveCpuData() {
        SystemInfo systemInfo = new SystemInfo();
        CentralProcessor processor = systemInfo.getHardware().getProcessor();

        String cpuName = processor.getProcessorIdentifier().getName();
        int cpuCores = processor.getPhysicalProcessorCount(); // Physical cores
        int cpuThreads = processor.getLogicalProcessorCount(); // Logical threads (including hyper-threading)

        List<String> cpuInfo = new ArrayList<>();
        cpuInfo.add(cpuName);
        cpuInfo.add("" + cpuCores);
        cpuInfo.add("" + cpuThreads);

        return cpuInfo;
    }
}

