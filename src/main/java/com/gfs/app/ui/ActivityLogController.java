package com.gfs.app.ui;

import com.gfs.app.model.ActivityLog;
import com.gfs.app.service.ActivityLogService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

import java.awt.Desktop;
import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class ActivityLogController {

    @FXML private DatePicker        startDatePicker;
    @FXML private DatePicker        endDatePicker;
    @FXML private TextField         usernameField;
    @FXML private ComboBox<String>  actionFilter;
    @FXML private Label             resultLabel;

    @FXML private TableView<ActivityLog>              tableView;
    @FXML private TableColumn<ActivityLog, String>    colTimestamp;
    @FXML private TableColumn<ActivityLog, String>    colUsername;
    @FXML private TableColumn<ActivityLog, String>    colAction;
    @FXML private TableColumn<ActivityLog, String>    colDescription;

    @FXML private StackPane         loadingOverlay;
    @FXML private ProgressIndicator progressIndicator;

    private final ActivityLogService service = new ActivityLogService();
    private Task<List<ActivityLog>>  currentLoadTask;

    @FXML
    public void initialize() {
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now());

        // Columns
        colTimestamp  .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCreatedAtFormatted()));
        colUsername   .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUsername()));
        colDescription.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDescription()));

        // Action column — coloured badge
        colAction.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAction()));
        colAction.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); setStyle(""); return; }
                Label badge = new Label(item);
                badge.setStyle(badgeStyle(item));
                setGraphic(badge);
                setText(null);
            }
        });

        // Action filter dropdown
        List<String> actions = service.getDistinctActions();
        actions.add(0, "ALL");
        actionFilter.setItems(FXCollections.observableArrayList(actions));
        actionFilter.setValue("ALL");

        // Enter key triggers search in username field
        usernameField.setOnAction(e -> loadData());

        loadData();
    }

    @FXML private void handleSearch()  { loadData(); }

    @FXML
    private void handleRefresh() {
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now());
        usernameField.clear();
        actionFilter.setValue("ALL");
        loadData();
    }

    @FXML
    private void handleExport() {
        if (currentLoadTask != null && currentLoadTask.isRunning()) {
            showAlert("Loading", "Please wait for data to finish loading.");
            return;
        }
        final LocalDate start    = startDatePicker.getValue();
        final LocalDate end      = endDatePicker.getValue();
        final String    username = text(usernameField);
        final String    action   = actionFilter.getValue();

        loadingOverlay.setVisible(true);
        Task<List<ActivityLog>> exportTask = new Task<>() {
            @Override protected List<ActivityLog> call() {
                return service.getAll(start, end, username, action);
            }
            @Override protected void succeeded() {
                loadingOverlay.setVisible(false);
                exportToCsv(getValue());
            }
            @Override protected void failed() {
                loadingOverlay.setVisible(false);
                showAlert("Export Error", getException().getMessage());
            }
        };
        new Thread(exportTask).start();
    }

    // -------------------------------------------------------------------------
    // Background load
    // -------------------------------------------------------------------------
    private void loadData() {
        if (currentLoadTask != null && currentLoadTask.isRunning()) currentLoadTask.cancel();

        LocalDate start = startDatePicker.getValue();
        LocalDate end   = endDatePicker.getValue();
        if (start == null) start = LocalDate.now();
        if (end   == null) end   = LocalDate.now();
        if (start.isAfter(end)) { LocalDate t = start; start = end; end = t; }

        final LocalDate finalStart    = start;
        final LocalDate finalEnd      = end;
        final String    finalUsername = text(usernameField);
        final String    finalAction   = actionFilter.getValue();

        loadingOverlay.setVisible(true);
        resultLabel.setText("Loading...");

        currentLoadTask = new Task<>() {
            @Override protected List<ActivityLog> call() {
                return service.getAll(finalStart, finalEnd, finalUsername, finalAction);
            }
            @Override protected void succeeded() {
                loadingOverlay.setVisible(false);
                List<ActivityLog> rows = getValue();
                tableView.setItems(FXCollections.observableArrayList(rows));
                resultLabel.setText(rows.size() + " record(s)");
            }
            @Override protected void failed() {
                loadingOverlay.setVisible(false);
                getException().printStackTrace();
                resultLabel.setText("Error loading data");
                showAlert("Database Error", getException().getMessage());
            }
            @Override protected void cancelled() {
                loadingOverlay.setVisible(false);
                resultLabel.setText("Cancelled");
            }
        };
        new Thread(currentLoadTask).start();
    }

    // -------------------------------------------------------------------------
    // CSV Export
    // -------------------------------------------------------------------------
    private void exportToCsv(List<ActivityLog> rows) {
        if (rows.isEmpty()) { showAlert("No Data", "Nothing to export."); return; }
        try {
            String downloadsPath = System.getProperty("user.home") + File.separator + "Downloads";
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path filePath = Paths.get(downloadsPath, "activity_log_" + timestamp + ".csv");

            try (PrintWriter writer = new PrintWriter(filePath.toFile(), "UTF-8")) {
                writer.write('\uFEFF');
                writer.println("Timestamp,Username,Action,Description");
                for (ActivityLog r : rows) {
                    writer.println(String.join(",",
                        csvSafe(r.getCreatedAtFormatted()),
                        csvSafe(r.getUsername()),
                        csvSafe(r.getAction()),
                        csvSafe(r.getDescription())
                    ));
                }
            }
            showExportSuccessDialog(filePath.toFile());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Export Error", e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Badge colour by action type
    // -------------------------------------------------------------------------
    private static String badgeStyle(String action) {
        if (action == null) return "";
        return switch (action) {
            case "LOGIN"         -> "-fx-background-color:#f5ede6; -fx-text-fill:#3a2316;"  +
                                    "-fx-font-size:11px; -fx-font-weight:bold; -fx-background-radius:20; -fx-padding:2 8;";
            case "LOGOUT"        -> "-fx-background-color:#f3f4f6; -fx-text-fill:#4b5563;"  +
                                    "-fx-font-size:11px; -fx-font-weight:bold; -fx-background-radius:20; -fx-padding:2 8;";
            case "LOGIN_FAILED"  -> "-fx-background-color:#fef2f2; -fx-text-fill:#991b1b;"  +
                                    "-fx-font-size:11px; -fx-font-weight:bold; -fx-background-radius:20; -fx-padding:2 8;";
            case "EXPORT_CSV"    -> "-fx-background-color:#eff6ff; -fx-text-fill:#1e40af;"  +
                                    "-fx-font-size:11px; -fx-font-weight:bold; -fx-background-radius:20; -fx-padding:2 8;";
            case "STAFF_CREATE", "ROLE_CREATE"  ->
                                   "-fx-background-color:#f0fdf4; -fx-text-fill:#166534;"  +
                                    "-fx-font-size:11px; -fx-font-weight:bold; -fx-background-radius:20; -fx-padding:2 8;";
            case "STAFF_DELETE", "ROLE_DELETE"  ->
                                   "-fx-background-color:#fef2f2; -fx-text-fill:#991b1b;"  +
                                    "-fx-font-size:11px; -fx-font-weight:bold; -fx-background-radius:20; -fx-padding:2 8;";
            default              -> "-fx-background-color:#f5ede6; -fx-text-fill:#765f52;"  +
                                    "-fx-font-size:11px; -fx-font-weight:bold; -fx-background-radius:20; -fx-padding:2 8;";
        };
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------
    private static String text(TextField f) {
        return (f == null || f.getText() == null) ? "" : f.getText().trim();
    }

    private static String csvSafe(String v) {
        if (v == null) return "";
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

    private void showExportSuccessDialog(File file) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Export Successful");
        alert.setHeaderText("Activity Log Exported");
        alert.setContentText("File saved to Downloads:\n" + file.getName());
        ButtonType openFile   = new ButtonType("Open File");
        ButtonType openFolder = new ButtonType("Open Folder");
        ButtonType close      = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(openFile, openFolder, close);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent()) {
            if (result.get() == openFile)   try { Desktop.getDesktop().open(file); }            catch (Exception e) { e.printStackTrace(); }
            if (result.get() == openFolder) try { Desktop.getDesktop().open(file.getParentFile()); } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void showAlert(String title, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR, message);
        a.setTitle(title); a.setHeaderText(null); a.showAndWait();
    }
}