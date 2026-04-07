package org.osmaintenance.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.application.Platform;
import org.osmaintenance.AppWindowManager;

public class PopupController {

    @FXML private Button btnQuickClean;
    @FXML private Button btnOpenPerformance;
    @FXML private Button btnOpenUpdates;
    @FXML private Button btnExit;

    @FXML
    public void initialize() {
        System.out.println("PopupController initialized.");

        bindButtonActions();
    }

    private void bindButtonActions() {
        btnQuickClean.setOnAction(e -> handleQuickClean());
        btnOpenPerformance.setOnAction(e -> handleOpenPerformance());
        btnExit.setOnAction(e -> handleExit());
    }

    private void handleQuickClean() {
        System.out.println("Quick Clean triggered.");
        AppWindowManager.showMainWindow(0);
    }

    private void handleOpenPerformance() {
        System.out.println("Opening main application...");
        AppWindowManager.showMainWindow(1);
    }

    private void handleExit() {
        System.out.println("Exiting application...");
        MainController.shutdownExecutors();
        Platform.exit();
        System.exit(0);
    }
}
