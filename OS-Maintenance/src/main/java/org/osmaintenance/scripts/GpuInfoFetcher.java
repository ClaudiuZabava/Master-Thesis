package org.osmaintenance.scripts;

import oshi.SystemInfo;
import oshi.hardware.GraphicsCard;
import java.util.ArrayList;
import java.util.List;
public class GpuInfoFetcher {
    public static List<String> retrieveGpuData() {
        SystemInfo systemInfo = new SystemInfo();
        GraphicsCard grafic_processor = systemInfo.getHardware().getGraphicsCards().get(0);

        String gpuName = grafic_processor.getName();
        String gpuMemory = grafic_processor.getVRam() / (1024 * 1024) + " MB";

        List<String> gpuInfo = new ArrayList<>();
        gpuInfo.add(gpuName);
        gpuInfo.add(gpuMemory);


        return gpuInfo;
    }
}
