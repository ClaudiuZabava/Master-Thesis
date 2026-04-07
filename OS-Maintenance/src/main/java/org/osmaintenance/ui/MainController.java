package org.osmaintenance.ui;


import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.osmaintenance.scripts.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


public class MainController {

    private static ScheduledExecutorService cpuExecutor;
    private static  ScheduledExecutorService gpuExecutor;
    private static  ScheduledExecutorService ramExecutor;
    private static  ScheduledExecutorService diskExecutor;

    private static long totalBytes = 0;
    private static String tempFilesSize = "0.0";

    private PieChart.Data cpuUsedData;
    private PieChart.Data cpuFreeData;
    private Label cpuPercentageLabel;

    private PieChart.Data gpuUsedData;
    private PieChart.Data gpuFreeData;
    private Label gpuPercentageLabel;

    private PieChart.Data ramUsedData;
    private PieChart.Data ramFreeData;
    private Label ramPercentageLabel;

    private PieChart.Data diskUsedData;
    private PieChart.Data diskFreeData;
    private Label diskPercentageLabel;

    private String gpuUpdateName = GpuInfoFetcher.retrieveGpuData().get(0);
    private String gpuUpdateLink = "";


    @FXML private TabPane tabPane;

    @FXML private Button btnClose;
    @FXML private Button btnMinimize;

    @FXML private VBox junkFilesPane;
    @FXML private VBox duplicateFilesPane;
    @FXML private VBox unusedAppsPane;

    @FXML private Button btnJunkFiles;
    @FXML private Button btnDuplicateFiles;
    @FXML private Button btnUnusedApps;

    @FXML private CheckBox tempFilesCheckbox;
    @FXML private CheckBox memoryDumpCheckbox;
    @FXML private CheckBox browserCachesCheckbox;
    @FXML private CheckBox systemLogsCheckbox;
    @FXML private Label totalTempSize;
    @FXML private Label totalTempUnit;

    @FXML private Label cacheSize0;
    @FXML private Label cacheSize1;
    @FXML private Label cacheSize2;
    @FXML private Label cacheSize3;

    @FXML private Label cacheVolume0;
    @FXML private Label cacheVolume1;
    @FXML private Label cacheVolume2;
    @FXML private Label cacheVolume3;

    @FXML private Button btnScanJunk;
    @FXML private Button btnClearJunk;


    @FXML private Label tempMessage;
    @FXML private Label tempAction;

    @FXML private VBox duplicateEntriesContainer;
    @FXML private Button btnSelectDirectory;
    @FXML private Label selectedDirectory;
    @FXML private CheckBox onlyFilterByNameCheckbox;
    @FXML private Button btnUnusedSelectDirectory;
    @FXML private VBox unusedEntriesContainer;
    @FXML private Label selectedDirectory2;

    @FXML private HBox cpuChartPane;
    @FXML private Label cpuNameLabel;
    @FXML private Label cpuCoresLabel;
    @FXML private Label cpuThreadsLabel;
    @FXML private HBox gpuChartPane;
    @FXML private Label gpuNameLabel;
    @FXML private Label gpuMemoryLabel;
    @FXML private HBox ramChartPane;
    @FXML private Label ramNameLabel;
    @FXML private Label ramCapacityLabel;
    @FXML private Label ramSpeedLabel;
    @FXML private Label ramUsedLabel;
    @FXML private HBox diskChartPane;
    @FXML private Label diskModelLabel;
    @FXML private Label diskTotalLabel;
    @FXML private Label diskFreeLabel;
    @FXML private Label diskUsedLabel;

    @FXML private VBox driverUpdatesEntries;
    @FXML private Button btnCheckUpdatesMicro;
    @FXML private Button btnClearUpdatesMicro;
    @FXML private Label updateMStatusLabel;
    @FXML private Label updaterGpuName;
    @FXML private Button btnCheckUpdates;
    @FXML private Button btnDownloadUpdates;
    @FXML private Label updateStatusLabel;

    public static void shutdownExecutors() {
        if (cpuExecutor != null) cpuExecutor.shutdownNow();
        if (gpuExecutor != null) gpuExecutor.shutdownNow();
        if (ramExecutor != null) ramExecutor.shutdownNow();
        if (diskExecutor != null) diskExecutor.shutdownNow();
    }


    @FXML
    private void initialize() {

        btnClose.setOnAction(e -> {
            if (btnClose.getScene() != null)
                ((Stage) btnClose.getScene().getWindow()).close();
        });

        btnMinimize.setOnAction(e -> {
            if (btnMinimize.getScene() != null)
                ((Stage) btnMinimize.getScene().getWindow()).setIconified(true);
        });

        // Delay dragging logic until scene is available
        btnClose.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                enableWindowDragging(newScene.getRoot());
            }
        });

        updaterGpuName.setText(gpuUpdateName);

//==============================================================================================
//                                CLEANER Mechanics
//==============================================================================================

        btnJunkFiles.getStyleClass().add("selected");

        btnJunkFiles.setOnAction(e -> {
            btnDuplicateFiles.getStyleClass().remove("selected");
            btnUnusedApps.getStyleClass().remove("selected");
            if( !btnJunkFiles.getStyleClass().contains("selected"))
                btnJunkFiles.getStyleClass().add("selected");
            showPane(junkFilesPane);
        });

        btnDuplicateFiles.setOnAction(e -> {
            btnJunkFiles.getStyleClass().remove("selected");
            btnUnusedApps.getStyleClass().remove("selected");
            if( !btnDuplicateFiles.getStyleClass().contains("selected"))
                btnDuplicateFiles.getStyleClass().add("selected");
            onlyFilterByNameCheckbox.setDisable(true);
            duplicateEntriesContainer.getChildren().clear();
            showPane(duplicateFilesPane);
        });
        btnUnusedApps.setOnAction(e -> {
            btnJunkFiles.getStyleClass().remove("selected");
            btnDuplicateFiles.getStyleClass().remove("selected");
            if( !btnUnusedApps.getStyleClass().contains("selected"))
                btnUnusedApps.getStyleClass().add("selected");
            unusedEntriesContainer.getChildren().clear();
            showPane(unusedAppsPane);
        });

        btnSelectDirectory.setOnAction(e -> {
            onlyFilterByNameCheckbox.setSelected(false);
            onlyFilterByNameCheckbox.setDisable(true);
            String selectedDir = DirectorySelector.selectDirectory();
            selectedDirectory.setText("Selected Directory: " + selectedDir);

            Task<Void> selectDuplicatesDirectory = new Task<>() {
                Map<Integer, List<File>> duplicates;
                @Override
                protected Void call() throws Exception {
                    duplicates = DuplicateFileFinder.findDuplicatesByContent(selectedDir);
                    return null;
                }

                @Override
                protected void succeeded() {
                    duplicateEntriesContainer.getChildren().clear();
                    addEntriesFromMap(duplicates);
                    onlyFilterByNameCheckbox.setDisable(false);
                }

                @Override
                protected void failed() {
                    Throwable ex = getException();
                    ex.printStackTrace();
                    duplicateEntriesContainer.getChildren().clear();
                    selectedDirectory.setText("ERROR: Try selecting a directory again.");
                }
            };
            new Thread(selectDuplicatesDirectory).start();
        });

        btnUnusedSelectDirectory.setOnAction(e -> {
            String selectedDir = DirectorySelector.selectDirectory();
            selectedDirectory2.setText("Selected Directory: " + selectedDir);

            Task<Void> selectUnusedFiles = new Task<>() {
                Map<File, LocalDateTime> unusedFiles;
                @Override
                protected Void call() throws Exception {
                    unusedFiles = FileAccessAnalyzer.getLastAccessTimes(selectedDir);
                    return null;
                }

                @Override
                protected void succeeded() {
                    unusedEntriesContainer.getChildren().clear();
                    for (Map.Entry<File, LocalDateTime> entry : unusedFiles.entrySet()) {
                        File file = entry.getKey();
                        addEntry(file.getName(), file.getAbsolutePath(), entry.getValue(),0, 2);
                    }
                }

                @Override
                protected void failed() {
                    Throwable ex = getException();
                    ex.printStackTrace();
                    duplicateEntriesContainer.getChildren().clear();
                    selectedDirectory2.setText("ERROR: Try selecting a directory again.");
                }
            };
            new Thread(selectUnusedFiles).start();
        });


        onlyFilterByNameCheckbox.setOnAction(e -> {
            if(onlyFilterByNameCheckbox.isSelected()) {

                String selectedDir = selectedDirectory.getText().replace("Selected Directory: ", "");
                Task<Void> filterduplicatesName = new Task<>() {
                    Map<Integer, List<File>> duplicates;
                    @Override
                    protected Void call() throws Exception {
                        duplicates = DuplicateFileFinder.findDuplicatesByName(selectedDir, 0.8);
                        return null;
                    }

                    @Override
                    protected void succeeded() {
                        duplicateEntriesContainer.getChildren().clear();
                        addEntriesFromMap(duplicates);
                    }

                    @Override
                    protected void failed() {
                        Throwable ex = getException();
                        ex.printStackTrace();
                        duplicateEntriesContainer.getChildren().clear();
                        selectedDirectory.setText("ERROR: Try selecting a directory again.");
                    }
                };
                new Thread(filterduplicatesName).start();
            }
            else{
                String selectedDir = selectedDirectory.getText().replace("Selected Directory: ", "");
                Task<Void> filterduplicatesContent = new Task<>() {
                    Map<Integer, List<File>> duplicates;
                    @Override
                    protected Void call() throws Exception {
                        duplicates = DuplicateFileFinder.findDuplicatesByContent(selectedDir);
                        return null;
                    }

                    @Override
                    protected void succeeded() {
                        duplicateEntriesContainer.getChildren().clear();
                        addEntriesFromMap(duplicates);
                    }

                    @Override
                    protected void failed() {
                        Throwable ex = getException();
                        ex.printStackTrace();
                        duplicateEntriesContainer.getChildren().clear();
                        selectedDirectory.setText("ERROR: Try selecting a directory again.");
                    }
                };
                new Thread(filterduplicatesContent).start();
            }
        });

//==============================================================================================
//                                DRIVERS Update Mechanics
//==============================================================================================

        btnCheckUpdatesMicro.setOnAction(e -> {
            btnCheckUpdatesMicro.setDisable(true);
            driverUpdatesEntries.getChildren().clear();
            updateMStatusLabel.setText("Checking for outdated drivers...");
            Task<Void> checkUpdatesTask = new Task<>() {
                Map<Integer, List<String>> drivers;
                @Override
                protected Void call() throws Exception {
                    drivers = WindowsUpdateFetcher.queryWindowsUpdates();
                    return null;
                }

                @Override
                protected void succeeded() {
                    updateMStatusLabel.setText("Following drivers are outdated:" );
                    addDriverEntriesFromMap(drivers);
                    btnCheckUpdatesMicro.setDisable(false);
                    btnClearUpdatesMicro.setDisable(false);
                }

                @Override
                protected void failed() {
                    Throwable ex = getException();
                    ex.printStackTrace();
                    updateMStatusLabel.setText("Error while checking for updates. Try Again!");
                    driverUpdatesEntries.getChildren().clear();
                    btnCheckUpdatesMicro.setDisable(false);
                }
            };
            new Thread(checkUpdatesTask).start();
        });

        btnClearUpdatesMicro.setOnAction(e -> {
            btnClearUpdatesMicro.setDisable(true);
            driverUpdatesEntries.getChildren().clear();
            updateMStatusLabel.setText("Driver update list cleared. Check again anytime.");
        });

        btnCheckUpdates.setOnAction(e -> {
            btnCheckUpdates.setDisable(true);
            updateStatusLabel.setText("Checking for GPU driver updates...");
            String tempGpu = "";
            if (gpuUpdateName.contains("NVIDIA")) {
                tempGpu = gpuUpdateName;
                tempGpu = tempGpu.replace("NVIDIA", "").trim();
            }
            else
            {
                btnCheckUpdates.setDisable(true);
                updateStatusLabel.setText("This feature is only available for NVIDIA GPUs.");
            }
            String finalTempGpuName = tempGpu;
            Task<Void> checkGpuUpdatesTask = new Task<>() {
                List<String> updateInfo = null;
                @Override
                protected Void call() throws Exception {
                    updateInfo = WindowsUpdateFetcher.getUpdateInfo(finalTempGpuName);
                    return null;
                }

                @Override
                protected void succeeded() {
                    btnCheckUpdates.setDisable(false);
                    if (updateInfo == null || updateInfo.size() < 3) {
                        updateStatusLabel.setText("Failed to retrieve update information.");
                    }
                    else {
                        String currentVersion = updateInfo.get(0);
                        String latestVersion = updateInfo.get(1);
                        String downloadLink = updateInfo.get(2);
                        if(!Objects.equals(currentVersion, latestVersion) && currentVersion!=null && latestVersion != null) {
                            updateStatusLabel.setText("Current version: " + currentVersion + " | Latest version: " + latestVersion + " | Please Update!");
                            btnDownloadUpdates.setDisable(false);
                            gpuUpdateLink = downloadLink;
                        } else if(currentVersion != null && latestVersion != null) {
                            System.out.println("Current version: " + currentVersion + " | Latest version: " + latestVersion + " | Up to Date!");
                        }
                    }
                }

                @Override
                protected void failed() {
                    Throwable ex = getException();
                    ex.printStackTrace();
                    updateMStatusLabel.setText("Error while checking for updates. Try Again!");
                    btnCheckUpdates.setDisable(false);
                }
            };
            new Thread(checkGpuUpdatesTask).start();
        });

        btnDownloadUpdates.setOnAction(e -> {
            if(gpuUpdateLink != null && !gpuUpdateLink.isEmpty()) {
                btnDownloadUpdates.setDisable(true);
                btnCheckUpdates.setDisable(true);
                try {
                    // Open the download link in the default web browser
                    java.awt.Desktop.getDesktop().browse(java.net.URI.create(gpuUpdateLink));
                } catch (IOException ex) {
                    btnCheckUpdates.setDisable(false);
                    updateStatusLabel.setText("Failed to open download link. Please try again.");
                    ex.printStackTrace();
                }
                finally {
                    btnCheckUpdates.setDisable(false);
                    updateStatusLabel.setText("Please install your downloaded update !");
                }

            } else {
                updateStatusLabel.setText("No update link available. Please check for updates first.");
            }
        });


//==============================================================================================
//                                CLEANER Scan & Delete Mechanics
//==============================================================================================

        resetCacheLabels();

        btnScanJunk.setOnAction(e -> {

            final int[] taskCount = {0};
            AtomicInteger completedTasks = new AtomicInteger(0);
            btnClearJunk.setDisable(true);
            btnScanJunk.setDisable(true);
            resetCacheLabels();
            tempAction.setTextFill(Color.LIGHTSLATEGRAY);
            tempAction.setText("Scanning! Please wait...");
            tempAction.setVisible(true);
            tempMessage.setVisible(false);

            Runnable checkIfDone = () -> {
                if (completedTasks.incrementAndGet() == 4) {
                    tempAction.setVisible(false);
                    if (taskCount[0] == 4) {
                        tempMessage.setText("Scan completed successfully!");
                        tempMessage.setTextFill(Color.LIMEGREEN);
                        tempMessage.setVisible(true);
                        btnClearJunk.setDisable(false);

                        initCacheLabels();

                        increaseAndFormatSize(cacheSize0.getText(), cacheVolume0.getText());
                        increaseAndFormatSize(cacheSize1.getText(), cacheVolume1.getText());
                        increaseAndFormatSize(cacheSize2.getText(), cacheVolume2.getText());
                        increaseAndFormatSize(cacheSize3.getText(), cacheVolume3.getText());

                    } else {
                        tempMessage.setText("Error during scan. try Again !");
                        tempMessage.setTextFill(Color.RED);
                        tempMessage.setVisible(true);
                    }
                    btnScanJunk.setDisable(false);
                }
            };

            Task<Void> scaningBrowserTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    BrowserCacheScript.scanBrowserCaches();
                    return null;
                }

                @Override
                protected void succeeded() {
                    System.out.println("Scanning completed successfully.");
                    taskCount[0]++;
                    initCacheBrowser();
                    checkIfDone.run();
                }

                @Override
                protected void failed() {
                    taskCount[0]--;
                    Throwable ex = getException();
                    ex.printStackTrace();
                    checkIfDone.run();
                }
            };
            new Thread(scaningBrowserTask).start();

            Task<Void> scaningTempTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    TempFilesScript.scanTempFiles();
                    return null;
                }

                @Override
                protected void succeeded() {
                    System.out.println("Scanning completed successfully.");
                    taskCount[0]++;
                    initCacheTemp();
                    checkIfDone.run();
                }

                @Override
                protected void failed() {
                    taskCount[0]--;
                    Throwable ex = getException();
                    ex.printStackTrace();
                    checkIfDone.run();
                }
            };
            new Thread(scaningTempTask).start();

            Task<Void> scaningMemoryTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    MemoryDumpScript.scanMemoryDumps();
                    return null;
                }

                @Override
                protected void succeeded() {
                    System.out.println("Scanning completed successfully.");
                    taskCount[0]++;
                    initCacheMemory();
                    checkIfDone.run();
                }

                @Override
                protected void failed() {
                    taskCount[0]--;
                    Throwable ex = getException();
                    ex.printStackTrace();
                    checkIfDone.run();
                }
            };
            new Thread(scaningMemoryTask).start();

            Task<Void> scaningSystemErrLogsTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    SystemErrLogScript.scanSystemErrorLogs();
                    return null;
                }

                @Override
                protected void succeeded() {
                    System.out.println("Scanning completed successfully.");
                    taskCount[0]++;
                    initCacheSystemLogs();
                    checkIfDone.run();
                }

                @Override
                protected void failed() {
                    taskCount[0]--;
                    Throwable ex = getException();
                    ex.printStackTrace();
                    checkIfDone.run();
                }
            };
            new Thread(scaningSystemErrLogsTask).start();

        });

        btnClearJunk.setOnAction(e -> {
            final int[] taskCount = {0};
            final int[] selectedFields = {0};
            final boolean[] scheduleDelete = {false};
            AtomicInteger completedTasks = new AtomicInteger(0);
            btnClearJunk.setDisable(true);
            btnScanJunk.setDisable(true);
            tempAction.setTextFill(Color.LIGHTSLATEGRAY);
            tempAction.setText("Cleaning...");
            tempAction.setVisible(true);
            tempMessage.setVisible(false);

            Runnable checkIfDone = () -> {
                if (completedTasks.incrementAndGet() == selectedFields[0]) {
                    tempAction.setVisible(false);
                    if (taskCount[0] == selectedFields[0]) {
                        if(scheduleDelete[0]) {
                            tempMessage.setText("Clean completed successfully! Some files will be removed on next reboot");
                        }
                        else{
                            tempMessage.setText("Clean completed successfully!");
                        }
                        resetCacheSizes(1);
                        tempMessage.setTextFill(Color.LIMEGREEN);
                        tempMessage.setVisible(true);
                        btnClearJunk.setDisable(false);
                    } else {
                        tempMessage.setText("Error during scan. Try Again !");
                        tempMessage.setTextFill(Color.RED);
                        tempMessage.setVisible(true);
                    }
                    btnScanJunk.setDisable(false);
                }
            };

            if(tempFilesCheckbox.isSelected())
            {
                selectedFields[0]++;
                tempFilesCheckbox.setDisable(true);
                Task<Void> cleaningTempTask = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        TempFilesScript.cleanTempFiles();
                        return null;
                    }

                    @Override
                    protected void succeeded() {
                        System.out.println("Cleaning completed successfully.");
                        taskCount[0]++;
                        scheduleDelete[0] |=TempFilesScript.getScheduleDelete();
                        tempFilesCheckbox.setDisable(false);
                        checkIfDone.run();
                    }

                    @Override
                    protected void failed() {
                        taskCount[0]--;
                        Throwable ex = getException();
                        ex.printStackTrace();
                        checkIfDone.run();
                    }
                };
                new Thread(cleaningTempTask).start();
            }

            if(browserCachesCheckbox.isSelected())
            {
                selectedFields[0]++;
                browserCachesCheckbox.setDisable(true);
                Task<Void> cleaningBrowserTask = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        BrowserCacheScript.cleanBrowserCaches();
                        return null;
                    }

                    @Override
                    protected void succeeded() {
                        System.out.println("Cleaning completed successfully.");
                        taskCount[0]++;
                        scheduleDelete[0] |= BrowserCacheScript.getScheduleDelete();
                        browserCachesCheckbox.setDisable(false);
                        checkIfDone.run();
                    }

                    @Override
                    protected void failed() {
                        taskCount[0]--;
                        Throwable ex = getException();
                        ex.printStackTrace();
                        checkIfDone.run();

                    }
                };

                new Thread(cleaningBrowserTask).start();
            }

            if(memoryDumpCheckbox.isSelected())
            {
                selectedFields[0]++;
                memoryDumpCheckbox.setDisable(true);
                Task<Void> cleaningMemoryTask = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        MemoryDumpScript.cleanMemoryDumps();
                        return null;
                    }

                    @Override
                    protected void succeeded() {
                        System.out.println("Cleaning completed successfully.");
                        taskCount[0]++;
                        scheduleDelete[0] |= MemoryDumpScript.getScheduleDelete();
                        memoryDumpCheckbox.setDisable(false);
                        checkIfDone.run();
                    }

                    @Override
                    protected void failed() {
                        taskCount[0]--;
                        Throwable ex = getException();
                        ex.printStackTrace();
                        checkIfDone.run();
                    }
                };
                new Thread(cleaningMemoryTask).start();
            }

            if(systemLogsCheckbox.isSelected())
            {
                selectedFields[0]++;
                systemLogsCheckbox.setDisable(true);
                Task<Void> cleaningSystemLogsTask = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        SystemErrLogScript.cleanSystemErrorLogs();
                        return null;
                    }

                    @Override
                    protected void succeeded() {
                        System.out.println("Cleaning completed successfully.");
                        taskCount[0]++;
                        systemLogsCheckbox.setDisable(false);
                        checkIfDone.run();
                    }

                    @Override
                    protected void failed() {
                        taskCount[0]--;
                        Throwable ex = getException();
                        ex.printStackTrace();
                        checkIfDone.run();
                    }
                };
                new Thread(cleaningSystemLogsTask).start();
            }

        });

        tempFilesCheckbox.setOnAction(e -> {
            if (tempFilesCheckbox.isSelected()) {
                cacheSize0.setTextFill(Color.WHITE);
                cacheVolume0.setTextFill(Color.WHITE);
                if(cacheSize0.getText().equals("..."))
                {
                    increaseAndFormatSize("0", "B");
                }
                else
                {
                    increaseAndFormatSize(cacheSize0.getText(), cacheVolume0.getText());
                }

            }
            else
            {
                cacheSize0.setTextFill(Color.GRAY);
                cacheVolume0.setTextFill(Color.GRAY);

                if(cacheSize0.getText().equals("..."))
                {
                    decreaseAndFormatSize("0", "B");
                }
                else
                {
                    decreaseAndFormatSize(cacheSize0.getText(), cacheVolume0.getText());
                }
            }
        });

        memoryDumpCheckbox.setOnAction(e -> {
            if (memoryDumpCheckbox.isSelected()) {
                cacheSize1.setTextFill(Color.WHITE);
                cacheVolume1.setTextFill(Color.WHITE);
                if(cacheSize1.getText().equals("..."))
                {
                    increaseAndFormatSize("0", "B");
                }
                else
                {
                    increaseAndFormatSize(cacheSize1.getText(), cacheVolume1.getText());
                }
            }
            else
            {
                cacheSize1.setTextFill(Color.GRAY);
                cacheVolume1.setTextFill(Color.GRAY);
                if(cacheSize1.getText().equals("..."))
                {
                    decreaseAndFormatSize("0", "B");
                }
                else
                {
                    decreaseAndFormatSize(cacheSize1.getText(), cacheVolume1.getText());
                }
            }
        });

        browserCachesCheckbox.setOnAction(e -> {
            if (browserCachesCheckbox.isSelected()) {
                cacheSize2.setTextFill(Color.WHITE);
                cacheVolume2.setTextFill(Color.WHITE);
                if(cacheSize2.getText().equals("..."))
                {
                    increaseAndFormatSize("0", "B");
                }
                else
                {
                    increaseAndFormatSize(cacheSize2.getText(), cacheVolume2.getText());
                }
            }
            else
            {
                cacheSize2.setTextFill(Color.GRAY);
                cacheVolume2.setTextFill(Color.GRAY);
                if(cacheSize2.getText().equals("..."))
                {
                    decreaseAndFormatSize("0", "B");
                }
                else
                {
                    decreaseAndFormatSize(cacheSize2.getText(), cacheVolume2.getText());
                }
            }
        });

        systemLogsCheckbox.setOnAction(e -> {
            if (systemLogsCheckbox.isSelected()) {
                cacheSize3.setTextFill(Color.WHITE);
                cacheVolume3.setTextFill(Color.WHITE);
                if(cacheSize3.getText().equals("..."))
                {
                    increaseAndFormatSize("0", "B");
                }
                else
                {
                    increaseAndFormatSize(cacheSize3.getText(), cacheVolume3.getText());
                }
            }
            else
            {
                cacheSize3.setTextFill(Color.GRAY);
                cacheVolume3.setTextFill(Color.GRAY);
                if(cacheSize3.getText().equals("..."))
                {
                    decreaseAndFormatSize("0", "B");
                }
                else
                {
                    decreaseAndFormatSize(cacheSize3.getText(), cacheVolume3.getText());
                }
            }
        });

        setupCpuChart(cpuChartPane);
        setupGpuChart(gpuChartPane);
        setupRamChart(ramChartPane);
        setupDiskChart(diskChartPane);

    }

    public void selectTab(int index) {
        tabPane.getSelectionModel().select(index);
    }

//==============================================================================================
//                                UI update mechanics
//==============================================================================================

    private void resetCacheSizes(int state) {
        if(state == 0) {
            cacheSize0.setText("...");
            cacheVolume0.setText("B");
            cacheSize1.setText("...");
            cacheVolume1.setText("B");
            cacheSize2.setText("...");
            cacheVolume2.setText("B");
            cacheSize3.setText("...");
            cacheVolume3.setText("B");
        }
        else
        {
            if(tempFilesCheckbox.isSelected())
            {
                if(cacheSize0.getText().equals("..."))
                {
                    decreaseAndFormatSize("0", "B");
                }
                else
                {
                    decreaseAndFormatSize(cacheSize0.getText(), cacheVolume0.getText());
                }

                cacheSize0.setText("0");
                cacheVolume0.setText("B");
            }
            if(memoryDumpCheckbox.isSelected())
            {
                if(cacheSize1.getText().equals("..."))
                {
                    decreaseAndFormatSize("0", "B");
                }
                else
                {
                    decreaseAndFormatSize(cacheSize1.getText(), cacheVolume1.getText());
                }

                cacheSize1.setText("0");
                cacheVolume1.setText("B");
            }
            if(browserCachesCheckbox.isSelected())
            {
                if(cacheSize2.getText().equals("..."))
                {
                    decreaseAndFormatSize("0", "B");
                }
                else
                {
                    decreaseAndFormatSize(cacheSize2.getText(), cacheVolume2.getText());
                }

                cacheSize2.setText("0");
                cacheVolume2.setText("B");
            }
            if(systemLogsCheckbox.isSelected())
            {
                if(cacheSize3.getText().equals("..."))
                {
                    decreaseAndFormatSize("0", "B");
                }
                else
                {
                    decreaseAndFormatSize(cacheSize3.getText(), cacheVolume3.getText());
                }

                cacheSize3.setText("0");
                cacheVolume3.setText("B");
            }
        }

    }

    private void resetCacheLabels() {
        resetCacheSizes(0);

        tempFilesCheckbox.setSelected(false);
        memoryDumpCheckbox.setSelected(false);
        browserCachesCheckbox.setSelected(false);
        systemLogsCheckbox.setSelected(false);

        tempFilesCheckbox.setDisable(true);
        memoryDumpCheckbox.setDisable(true);
        browserCachesCheckbox.setDisable(true);
        systemLogsCheckbox.setDisable(true);
    }

    private void initCacheLabels()
    {
        tempFilesCheckbox.setDisable(false);
        memoryDumpCheckbox.setDisable(false);
        browserCachesCheckbox.setDisable(false);
        systemLogsCheckbox.setDisable(false);
    }

    private void initCacheTemp() {
        cacheSize0.setText(TempFilesScript.getTotalSizeValue());
        cacheSize0.setDisable(false);
        cacheVolume0.setText(TempFilesScript.getTotalSizeUnit());
        cacheVolume0.setDisable(false);
        tempFilesCheckbox.setSelected(true);
    }

    private void initCacheMemory() {
        cacheSize1.setText(MemoryDumpScript.getTotalSizeValue());
        cacheVolume1.setText(MemoryDumpScript.getTotalSizeUnit());
        memoryDumpCheckbox.setSelected(true);
    }

    private void initCacheBrowser() {
        cacheSize2.setText(BrowserCacheScript.getTotalSizeValue());
        cacheSize2.setDisable(false);
        cacheVolume2.setText(BrowserCacheScript.getTotalSizeUnit());
        cacheVolume2.setDisable(false);
        browserCachesCheckbox.setSelected(true);
    }

    private void initCacheSystemLogs() {
        cacheSize3.setText(SystemErrLogScript.getTotalSizeValue());
        cacheVolume3.setText(SystemErrLogScript.getTotalSizeUnit());
        systemLogsCheckbox.setSelected(true);
    }

    private void showPane(Node paneToShow) {
        junkFilesPane.setVisible(false); junkFilesPane.setManaged(false);
        duplicateFilesPane.setVisible(false); duplicateFilesPane.setManaged(false);
        unusedAppsPane.setVisible(false); unusedAppsPane.setManaged(false);

        paneToShow.setVisible(true);
        paneToShow.setManaged(true);
    }

    // Drag logic
    private void enableWindowDragging(Node root) {
        final double[] offset = new double[2];

        root.setOnMousePressed(event -> {
            offset[0] = event.getSceneX();
            offset[1] = event.getSceneY();
        });

        root.setOnMouseDragged(event -> {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setX(event.getScreenX() - offset[0]);
            stage.setY(event.getScreenY() - offset[1]);
        });
    }



    private void increaseAndFormatSize(String sizeStr, String unit) {
        double size;
        try {
            size = Double.parseDouble(sizeStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number: " + sizeStr);
        }

        // Convert input to bytes based on unit
        long bytes = switch (unit.toUpperCase()) {
            case "B"  -> (long) size;
            case "KB" -> (long) (size * 1024);
            case "MB" -> (long) (size * 1024 * 1024);
            case "GB" -> (long) (size * 1024 * 1024 * 1024);
            case "TB" -> (long) (size * 1024 * 1024 * 1024 * 1024);
            default   -> throw new IllegalArgumentException("Unknown unit: " + unit);
        };

        // Add to total
        totalBytes += bytes;

        totalTempSize.setText(formatBytes(totalBytes).get(0));
        totalTempUnit.setText(formatBytes(totalBytes).get(1));
    }

    private void decreaseAndFormatSize(String sizeStr, String unit) {
        double size;
        try {
            size = Double.parseDouble(sizeStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number: " + sizeStr);
        }

        // Convert input to bytes based on unit
        long bytes = switch (unit.toUpperCase()) {
            case "B"  -> (long) size;
            case "KB" -> (long) (size * 1024);
            case "MB" -> (long) (size * 1024 * 1024);
            case "GB" -> (long) (size * 1024 * 1024 * 1024);
            case "TB" -> (long) (size * 1024 * 1024 * 1024 * 1024);
            default   -> throw new IllegalArgumentException("Unknown unit: " + unit);
        };

        // Add to total
        totalBytes -= bytes;

        totalTempSize.setText(formatBytes(totalBytes).get(0));
        totalTempUnit.setText(formatBytes(totalBytes).get(1));
    }

    private List<String> formatBytes(long bytes) {
        List<String> formattedSize = new ArrayList<>();
        double value = bytes;
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;

        while (value >= 1024 && unitIndex < units.length - 1) {
            value /= 1024.0;
            unitIndex++;
        }

        formattedSize.add(String.format("%.2f", value));
        formattedSize.add(units[unitIndex]);

        return formattedSize;
    }


//==============================================================================================
//                                Data entries in UI
//==============================================================================================


    private void addEntry(String fileName, String filePath, LocalDateTime fileDate, int clr, int target) {
        HBox row = new HBox();
        row.setSpacing(10);
        row.setAlignment(Pos.CENTER_LEFT);
        if(clr == 0)
        {
            row.setStyle("-fx-background-color: #323556; -fx-background-radius: 5;");
        }
        else
        {
            row.setStyle("-fx-background-color: #434773; -fx-background-radius: 5;");
        }

        row.setPadding(new Insets(10));
        row.setPrefWidth(Region.USE_COMPUTED_SIZE);
        row.setMaxWidth(Double.MAX_VALUE);

        Label label = new Label(fileName);
        label.setStyle("-fx-text-fill: white; -fx-font-size: 15px;");
        label.setMaxWidth(500);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label label_path = new Label(filePath);
        label_path.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");
        label_path.setDisable(true);
        label_path.setVisible(false);
        label_path.setMaxWidth(1);

        // "Open" Button
        Button openButton = new Button("Open");
        openButton.setStyle(
                "-fx-background-color: #4caf50; -fx-text-fill: white; -fx-font-size: 12px; -fx-background-radius: 4;"
        );
        openButton.setOnAction(e -> {
            try {
                String folderPath = label_path.getText();
                if (folderPath != null && !folderPath.isEmpty()) {
                    new ProcessBuilder("explorer.exe", "/select,",  folderPath).start();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });


        Button removeButton = new Button("Remove");
        removeButton.setStyle(
                "-fx-background-color: #ff4c4c; -fx-text-fill: white; -fx-font-size: 12px; -fx-background-radius: 4;"
        );
        removeButton.setOnAction(e -> {
            if(target == 1)
            {
                DuplicateFileFinder.deleteFile(new File(filePath).toPath());
                duplicateEntriesContainer.getChildren().remove(row);
            }
            else if(target == 2)
            {
                FileAccessAnalyzer.deleteFile(new File(filePath).toPath());
                unusedEntriesContainer.getChildren().remove(row);
            }

        });

        if(target == 1)
        {
            row.getChildren().addAll(label, label_path,spacer, openButton,removeButton);
            duplicateEntriesContainer.getChildren().add(row);
        }
        else if(target == 2)
        {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            label.setMaxWidth(400);
            Label label_date = new Label(formatter.format(fileDate));
            label_date.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");
            label_date.setMaxWidth(150);

            Region spacer2 = new Region();
            HBox.setHgrow(spacer2, Priority.ALWAYS);

            row.getChildren().addAll(label, label_path,spacer,label_date, openButton,removeButton);
            unusedEntriesContainer.getChildren().add(row);
        }

    }

    private void addEntriesFromMap(Map<Integer, List<File>> duplicates) {
       int groups = 0;
       int clr =0;
        for( Map.Entry<Integer, List<File>> entry : duplicates.entrySet()) {
            if(groups%2==0)
            {
                groups++;
                clr = 0;
            }
            else
            {
                groups++;
                clr = 1;
            }
            List<File> files = entry.getValue();
            if(files.size() > 1) {
                for(File f : files) {
                    addEntry(f.getName(), f.getAbsolutePath(), null, clr ,1);
                }
            }
        }
    }

    private void addDriverEntriesFromMap(Map<Integer, List<String>> drivers) {
        for( Map.Entry<Integer, List<String>> entry : drivers.entrySet()) {
            List<String> infos = entry.getValue();
            if(!infos.isEmpty()) {
                String link = WindowsUpdateFetcher.generateLink(infos.getFirst());
                if(link != null && !link.isEmpty()) {
                    String driverName = infos.getFirst();
                    String driverVersion = infos.get(1);
                    addUpdateEntry(driverName, driverVersion, link);
                }
            }
        }
    }

    private void addUpdateEntry(String driverName, String driverVersion, String driverLink) {
        HBox row = new HBox();
        row.setSpacing(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: #323556; -fx-background-radius: 5;");

        row.setPadding(new Insets(10));
        row.setPrefWidth(Region.USE_COMPUTED_SIZE);
        row.setMaxWidth(Double.MAX_VALUE);

        Label label = new Label(driverName);
        label.setStyle("-fx-text-fill: white; -fx-font-size: 15px;");
        label.setMaxWidth(500);

        Label label_ver = new Label("Installed: "  + driverVersion);
        label_ver.setStyle("-fx-text-fill: white; -fx-font-size: 15px;");
        label_ver.setMaxWidth(150);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label label_path = new Label(driverLink);
        label_path.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");
        label_path.setDisable(true);
        label_path.setVisible(false);
        label_path.setMaxWidth(1);

        Button downloadButton = new Button("Download Update");
        downloadButton.setStyle(
                "-fx-background-color: #4caf50; -fx-text-fill: white; -fx-font-size: 12px; -fx-background-radius: 4;"
        );
        downloadButton.setOnAction(e -> {
            try {
                WindowsUpdateFetcher.openDriverLink(driverLink);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        row.getChildren().addAll(label, label_path,spacer, label_ver, downloadButton);
        driverUpdatesEntries.getChildren().add(row);

    }


//===============================================================================================
//                                  Parallel Monitoring Threads
//===============================================================================================

    private void startMonitoring(int target) {

        if(target == 0)
        {
            cpuExecutor = Executors.newSingleThreadScheduledExecutor();
            cpuExecutor.scheduleAtFixedRate(() -> {
                try {
                    double usedPercentage = cpuUsage.getCpuUsagePercentage();
                    Platform.runLater(() -> updatePieChart(usedPercentage, 100-usedPercentage, target));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, 0, 2, TimeUnit.SECONDS);
        }
        else if(target == 1)
        {
            gpuExecutor = Executors.newSingleThreadScheduledExecutor();
            gpuExecutor.scheduleAtFixedRate(() -> {
                try {
                    String script = "src/main/java/org/osmaintenance/scripts/gpuUsage.py";
                    String method = "gpuUsagePercentage";

                    ProcessBuilder pb = new ProcessBuilder("py", script, method);
                    pb.redirectErrorStream(true);
                    Process gpuProcess = pb.start();

                    BufferedReader reader = new BufferedReader(new InputStreamReader(gpuProcess.getInputStream()));
                    String line = reader.readLine();
                    if (line != null) {
                        double usedPercentage = Double.parseDouble(line.trim());
                        Platform.runLater(() -> updatePieChart(usedPercentage, 100-usedPercentage, target));
                        //gpuProcess.destroy();
                    }
                    else
                    {
                        System.err.println("No output from GPU usage script.");
                    }
                    gpuProcess.waitFor();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, 0, 5, TimeUnit.SECONDS);
        }
        else if(target == 3)
        {
            diskExecutor = Executors.newSingleThreadScheduledExecutor();
            diskExecutor.scheduleAtFixedRate(() -> {
                try {
                    List<Double> diskData= DiskInfoFetcher.retrieveDiskData();
                    Platform.runLater(() -> updatePieChart(diskData.get(2), diskData.get(1), target));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, 0, 5, TimeUnit.SECONDS);
        }
    }

    private void startRamMonitoring(double totalRam) {
        ramExecutor = Executors.newSingleThreadScheduledExecutor();
        ramExecutor.scheduleAtFixedRate(() -> {
            try {
                double availableRam = RamInfoFetcher.getAvailableRam();
                Platform.runLater(() -> updatePieChart(totalRam, availableRam, 2));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    private void updatePieChart(double usedPercentage, double free, int target) {;
        if(target == 0) {
            if (cpuUsedData != null && cpuFreeData != null && cpuPercentageLabel != null) {
                cpuUsedData.setPieValue(usedPercentage);
                cpuFreeData.setPieValue(free);
                cpuPercentageLabel.setText(String.format("%.0f%%", usedPercentage));
            }
        }
        else if(target == 1) {
            if (gpuUsedData != null && gpuFreeData != null && gpuPercentageLabel != null) {
                gpuUsedData.setPieValue(usedPercentage);
                gpuFreeData.setPieValue(free);
                gpuPercentageLabel.setText(String.format("%.0f%%", usedPercentage));
            }
        }
        else if(target == 2)
        {
            // usedPercentage this time IS the total RAM !
            if( ramUsedData != null && ramFreeData != null && ramPercentageLabel != null) {
                ramUsedData.setPieValue(usedPercentage -free);
                ramFreeData.setPieValue(free);
                ramUsedLabel.setText(String.format("%.2f GB", free));
                ramPercentageLabel.setText(String.format("%.0f%%", ((usedPercentage-free)/usedPercentage)*100));
            }

        }
        else if(target == 3) {
            if (diskUsedData != null && diskFreeData != null && diskPercentageLabel != null) {
                diskUsedData.setPieValue(usedPercentage);
                diskFreeData.setPieValue(free);
                diskUsedLabel.setText(String.format("%.2f GB", usedPercentage));
                diskFreeLabel.setText(String.format("%.2f GB", free));
                diskPercentageLabel.setText(String.format("%.0f%%", (usedPercentage/(free+usedPercentage))*100));
            }
        }

    }


//===============================================================================================
//                                  CPU Monitoring
//===============================================================================================

    private void setupCpuChart(HBox targetBox) {
        double free = 100;

        PieChart pieChart = new PieChart();
        pieChart.setLegendVisible(false);
        pieChart.setLabelsVisible(false);
        pieChart.setPrefSize(120, 120);

        cpuUsedData = new PieChart.Data("Used", 0.0);
        cpuFreeData = new PieChart.Data("Free", free);
        pieChart.getData().addAll(cpuUsedData, cpuFreeData);

        cpuPercentageLabel = new Label(String.format("%.0f%%", 0.0));
        cpuPercentageLabel.setTextFill(Color.WHITE);
        cpuPercentageLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        StackPane chartWithLabel = new StackPane(pieChart, cpuPercentageLabel);
        chartWithLabel.setPrefSize(120, 120);
        chartWithLabel.setAlignment(Pos.CENTER);
        HBox.setMargin(chartWithLabel, new Insets(0, 0, 0, 20));

        List<String> cpuInfo = CpuInfoFetcher.retrieveCpuData();
        if(cpuInfo.size() < 3) {
            System.err.println("CPU information is incomplete.");
            return;
        }

        cpuNameLabel.setText(cpuInfo.get(0));
        cpuCoresLabel.setText(cpuInfo.get(1));
        cpuThreadsLabel.setText(cpuInfo.get(2));

        targetBox.getChildren().clear();
        targetBox.getChildren().add(chartWithLabel);

        startMonitoring(0);
    }


//===============================================================================================
//                                  GPU Monitoring
//===============================================================================================

    private void setupGpuChart(HBox targetBox) {
        List<String> gpuInfo = GpuInfoFetcher.retrieveGpuData();
        if (gpuInfo.size() < 2) {
            System.err.println("GPU information is incomplete.");
            return;
        }
        double free = 100;

        PieChart pieChartGpu = new PieChart();
        pieChartGpu.setLegendVisible(false);
        pieChartGpu.setLabelsVisible(false);
        pieChartGpu.setPrefSize(120, 120);

        gpuUsedData = new PieChart.Data("Used", 0.0);
        gpuFreeData = new PieChart.Data("Free", free);
        pieChartGpu.getData().addAll(gpuUsedData, gpuFreeData);

        gpuPercentageLabel = new Label(String.format("%.0f%%", 0.0));
        gpuPercentageLabel.setTextFill(Color.WHITE);
        gpuPercentageLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        StackPane chartWithLabelGpu = new StackPane(pieChartGpu, gpuPercentageLabel);
        chartWithLabelGpu.setPrefSize(120, 120);
        chartWithLabelGpu.setAlignment(Pos.CENTER);
        HBox.setMargin(chartWithLabelGpu, new Insets(0, 0, 0, 20));

        gpuNameLabel.setText(gpuInfo.get(0));
        gpuMemoryLabel.setText(gpuInfo.get(1));

        targetBox.getChildren().clear();
        targetBox.getChildren().add(chartWithLabelGpu);

        startMonitoring(1);
    }


//===============================================================================================
//                                  RAM Monitoring
//===============================================================================================


    private void setupRamChart(HBox targetBox) {
        List<String> ramInfo = RamInfoFetcher.retrieveRamData();
        if (ramInfo.size() < 3) {
            System.err.println("GPU information is incomplete.");
            return;
        }
        double free = 100;

        PieChart pieChartRam = new PieChart();
        pieChartRam.setLegendVisible(false);
        pieChartRam.setLabelsVisible(false);
        pieChartRam.setPrefSize(120, 120);

        ramUsedData = new PieChart.Data("Used", 0.0);
        ramFreeData = new PieChart.Data("Free", free);
        pieChartRam.getData().addAll(ramUsedData, ramFreeData);

        ramPercentageLabel = new Label(String.format("%.0f%%", 0.0));
        ramPercentageLabel.setTextFill(Color.WHITE);
        ramPercentageLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        StackPane chartWithLabelRam = new StackPane(pieChartRam, ramPercentageLabel);
        chartWithLabelRam.setPrefSize(120, 120);
        chartWithLabelRam.setAlignment(Pos.CENTER);
        HBox.setMargin(chartWithLabelRam, new Insets(0, 0, 0, 20));


        ramNameLabel.setText(ramInfo.get(0));
        ramCapacityLabel.setText(ramInfo.get(1));
        ramSpeedLabel.setText(ramInfo.get(2));
        ramUsedLabel.setText("0 GB");

        double totalRam = RamInfoFetcher.getTotalRam();

        targetBox.getChildren().clear();
        targetBox.getChildren().add(chartWithLabelRam);

        startRamMonitoring(totalRam);
    }

    private void setupDiskChart(HBox targetBox) {
        List<Double> diskInfo = DiskInfoFetcher.retrieveDiskData();
        if (diskInfo.size() < 3) {
            System.err.println("GPU information is incomplete.");
            return;
        }
        double free = 100;

        PieChart pieChartDisk = new PieChart();
        pieChartDisk.setLegendVisible(false);
        pieChartDisk.setLabelsVisible(false);
        pieChartDisk.setPrefSize(120, 120);

        diskUsedData = new PieChart.Data("Used", 0.0);
        diskFreeData = new PieChart.Data("Free", free);
        pieChartDisk.getData().addAll(diskUsedData, diskFreeData);

        diskPercentageLabel = new Label(String.format("%.0f%%", 0.0));
        diskPercentageLabel.setTextFill(Color.WHITE);
        diskPercentageLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        StackPane chartWithLabelDisk = new StackPane(pieChartDisk, diskPercentageLabel);
        chartWithLabelDisk.setPrefSize(120, 120);
        chartWithLabelDisk.setAlignment(Pos.CENTER);
        HBox.setMargin(chartWithLabelDisk, new Insets(0, 0, 0, 20));


        diskModelLabel.setText(DiskInfoFetcher.retrieveDiskName());
        diskTotalLabel.setText(Math.ceil(diskInfo.get(0)) + " GB");
        diskFreeLabel.setText("0 GB");
        diskUsedLabel.setText("0 GB");

        targetBox.getChildren().clear();
        targetBox.getChildren().add(chartWithLabelDisk);

        startMonitoring(3);
    }





}
