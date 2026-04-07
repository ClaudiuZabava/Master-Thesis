package org.osmaintenance.scripts;

import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DiskInfoFetcher {

    public static String retrieveDiskName() {
        SystemInfo systemInfo = new SystemInfo();
        HardwareAbstractionLayer hardware = systemInfo.getHardware();

       return  hardware.getDiskStores().getFirst().getModel();
    }

    public static List<Double> retrieveDiskData() {
        List<Double> diskUsages = new ArrayList<>();

        File[] roots = File.listRoots();
        for (File root : roots) {
            String key = root.getAbsolutePath();
            double total = (double) root.getTotalSpace() / (1024 * 1024 * 1024);
            double free = (double) root.getFreeSpace() / (1024 * 1024 * 1024);
            double used = total - free;
            if(diskUsages.isEmpty())
            {
                diskUsages.add(total);
                diskUsages.add(free);
                diskUsages.add(used);
            }
            else
            {
                diskUsages.set(0, diskUsages.get(0) + total);
                diskUsages.set(1, diskUsages.get(1) + free);
                diskUsages.set(2, diskUsages.get(2) + used);
            }

        }
      return diskUsages;
    }
}
