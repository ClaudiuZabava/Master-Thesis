package org.osmaintenance.tray;

import java.awt.*;
import java.io.IOException;
import javax.imageio.ImageIO;
import javafx.application.Platform;
import org.osmaintenance.AppWindowManager;

public class TrayManager {
    public static void initTray() {
        if (!SystemTray.isSupported()) return;

        try {
            // Use getResourceAsStream instead of getResource to avoid null
            var imageStream = TrayManager.class.getResourceAsStream("app_icon.png");
            if (imageStream == null) {
                System.err.println("Icon image stream is null! Check resource path.");
                return;
            }

            Image image = ImageIO.read(imageStream);
            final TrayIcon trayIcon =new TrayIcon(image.getScaledInstance(32, 32, Image.SCALE_SMOOTH), "OS-Maintenance");
            trayIcon.setImageAutoSize(true);


            trayIcon.addActionListener(e -> {
                System.out.println("Tray icon left-clicked");
                Platform.runLater(AppWindowManager::showPopupWindow);
            });

            // Right-click popup menu
            PopupMenu popupMenu = new PopupMenu();

            MenuItem showItem = new MenuItem("Open");
            showItem.addActionListener(e -> Platform.runLater(() -> {
                AppWindowManager.showMainWindow(0);
            }));

            MenuItem exitItem = new MenuItem("Exit");
            exitItem.addActionListener(e -> {
                SystemTray.getSystemTray().remove(trayIcon);
                Platform.exit();
                System.exit(0);
            });

            popupMenu.add(showItem);
            popupMenu.add(exitItem);
            trayIcon.setPopupMenu(popupMenu);

            SystemTray.getSystemTray().add(trayIcon);

        } catch (IOException | AWTException e) {
            e.printStackTrace();
        }
    }
}
