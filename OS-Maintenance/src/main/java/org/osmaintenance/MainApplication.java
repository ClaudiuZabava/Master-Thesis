package org.osmaintenance;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.osmaintenance.tray.TrayManager;
import org.osmaintenance.ui.MainController;

public class MainApplication extends Application {

    private Process pythonProcess;

    @Override
    public void start(Stage primaryStage) throws Exception {

        Platform.setImplicitExit(false);

        
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("fxml/main_window.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1000, 800);
        scene.setFill(Color.TRANSPARENT);
        primaryStage.initStyle(StageStyle.TRANSPARENT);

        primaryStage.setTitle("SysSphere");
        primaryStage.setScene(scene);
        primaryStage.show();

        MainController controller = fxmlLoader.getController();

        AppWindowManager.setMainWindow(primaryStage, controller);

        TrayManager.initTray();

        startPythonUsageMonitor();
    }

    @Override
    public void stop() throws Exception {
        // Perform any cleanup if necessary
        System.out.println("Application is closing. Performing cleanup...");
        MainController.shutdownExecutors();
        stopPythonUsageMonitor();
        super.stop();
    }

    private void startPythonUsageMonitor() {
        try {
            // Poți înlocui "py" cu "python" sau "python3" în funcție de sistem
            ProcessBuilder builder = new ProcessBuilder("py", "src/main/java/org/osmaintenance/scripts/usageMonitoring.py");
            pythonProcess = builder.start();
            System.out.println("Python usage monitor started.");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(" Failed to start Python script!");
        }
    }

    private void stopPythonUsageMonitor() {
        if (pythonProcess != null && pythonProcess.isAlive()) {
            System.out.println("Stopping Python usage monitor...");
            pythonProcess.destroy(); // trimite semnal de terminare
            try {
                pythonProcess.waitFor(); // așteaptă terminarea
                System.out.println("Python monitor stopped.");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        launch();
    }
}