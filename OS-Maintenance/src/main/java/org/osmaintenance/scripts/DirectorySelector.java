package org.osmaintenance.scripts;

import javax.swing.*;
import java.io.File;

public class DirectorySelector {


    public static String selectDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select a Directory");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        int result = chooser.showOpenDialog(null);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedDir = chooser.getSelectedFile();
            String path = selectedDir.getAbsolutePath();
            System.out.println("Selected directory: " + path);
            return path;

            // You can store this path for later use if needed
            // For example: Config.savePath(path); or return path;
        } else {
            System.out.println("Directory selection was cancelled.");
            return null;
        }
    }
}
