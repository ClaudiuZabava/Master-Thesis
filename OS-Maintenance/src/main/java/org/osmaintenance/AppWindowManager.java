package org.osmaintenance;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.osmaintenance.ui.MainController;

import java.io.IOException;

public class AppWindowManager {
    private static Stage mainWindow;
    private static Stage popupStage;

    private static MainController mainController;

    public static void setMainWindow(Stage stage,  MainController controller) {
        mainWindow = stage;
        mainController = controller;
    }

    public static void showMainWindow(int tabIndex) {
        if (mainController != null) {
            System.out.println("Switching to tab index: " + tabIndex);
            mainController.selectTab(tabIndex); // ex: 1 for "Performance"
        }

        mainWindow.show();
    }

    public static void showPopupWindow() {
        if (popupStage != null && popupStage.isShowing()) {
            popupStage.toFront();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(AppWindowManager.class.getResource("fxml/popup_window.fxml"));
            Parent root = loader.load();
            System.out.println("FXML loaded successfully");

            popupStage = new Stage(StageStyle.TRANSPARENT);
            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            popupStage.setScene(scene);
            popupStage.setAlwaysOnTop(true);
            popupStage.setResizable(false);
            popupStage.setTitle("SysSphere");

            // Get screen bounds (excluding taskbar)
            Rectangle2D bounds = Screen.getPrimary().getVisualBounds();

            double popupWidth = 300;
            double popupHeight = 400;
            double margin = 5; // margin from edges

            popupStage.setX(bounds.getMaxX() - popupWidth - margin);
            popupStage.setY(bounds.getMaxY() - popupHeight - margin);

            popupStage.show();

            popupStage.focusedProperty().addListener((obs, oldVal, isFocused) -> {
                if (!isFocused) popupStage.close();
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
