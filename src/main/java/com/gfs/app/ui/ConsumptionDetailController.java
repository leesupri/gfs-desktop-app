package com.gfs.app.ui;

import com.gfs.app.model.ConsumptionDetailRow;
import com.gfs.app.model.ConsumptionTreeRow;
import com.gfs.app.service.ConsumptionDetailService;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.StackPane;

import java.awt.Desktop;
import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class ConsumptionDetailController {

    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private TextField invoiceField;
    @FXML private TextField itemField;
    @FXML private TextField warehouseField;
    @FXML private Label resultLabel;
    @FXML private Label totalLabel;
    @FXML private TreeTableView<ConsumptionTreeRow> treeTableView;
    @FXML private TreeTableColumn<ConsumptionTreeRow, String> invoiceColumn;
    @FXML private TreeTableColumn<ConsumptionTreeRow, String> dateColumn;
    @FXML private TreeTableColumn<ConsumptionTreeRow, String> categoryColumn;
    @FXML private TreeTableColumn<ConsumptionTreeRow, String> resultDescriptionColumn;
    @FXML private TreeTableColumn<ConsumptionTreeRow, String> itemColumn;
    @FXML private TreeTableColumn<ConsumptionTreeRow, String> quantityColumn;
    @FXML private TreeTableColumn<ConsumptionTreeRow, String> uomColumn;
    @FXML private TreeTableColumn<ConsumptionTreeRow, String> unitCostColumn;
    @FXML private TreeTableColumn<ConsumptionTreeRow, String> totalCostColumn;
    @FXML private TreeTableColumn<ConsumptionTreeRow, String> warehouseColumn;
    @FXML private StackPane loadingOverlay;
    @FXML private ProgressIndicator progressIndicator;

    private final ConsumptionDetailService service = new ConsumptionDetailService();
    private final DecimalFormat decimalFormat = new DecimalFormat("#,##0.000");
    private Task<List<ConsumptionDetailRow>> currentLoadTask;

    @FXML
    public void initialize() {
        // Default date range: last 30 days
        startDatePicker.setValue(LocalDate.now().minusDays(30));
        endDatePicker.setValue(LocalDate.now());

        // Bind columns to ConsumptionTreeRow properties (all strings)
        invoiceColumn.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getValue().getInvoice()));
        dateColumn.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getValue().getDate()));
        categoryColumn.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getValue().getCategory()));
        resultDescriptionColumn.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getValue().getResultDescription()));
        itemColumn.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getValue().getItem()));
        quantityColumn.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getValue().getQuantity()));
        uomColumn.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getValue().getUom()));
        unitCostColumn.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getValue().getUnitCost()));
        totalCostColumn.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getValue().getTotalCost()));
        warehouseColumn.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getValue().getWarehouse()));

        loadData();
    }

    @FXML
    private void handleSearch() {
        loadData();
    }

    @FXML
    private void handleRefresh() {
        startDatePicker.setValue(LocalDate.now().minusDays(30));
        endDatePicker.setValue(LocalDate.now());
        invoiceField.clear();
        itemField.clear();
        warehouseField.clear();
        loadData();
    }

    @FXML
    private void handleExport() {
        // If a load is in progress, wait or cancel? For simplicity, we disable export while loading.
        if (currentLoadTask != null && currentLoadTask.isRunning()) {
            showAlert("Loading in Progress", "Please wait for data to finish loading before exporting.");
            return;
        }

        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();
        if (startDate == null || endDate == null) {
            showAlert("Invalid Dates", "Please select both start and end dates.");
            return;
        }

        List<ConsumptionDetailRow> rows = null; // we need fresh data for export
        // Re‑fetch in background (or reuse last loaded data if stored)
        exportInBackground(startDate, endDate);
    }

    @FXML
private void handleToday() {
    LocalDate today = LocalDate.now();
    startDatePicker.setValue(today);
    endDatePicker.setValue(today);
    loadData(); // trigger search with single day
}


    // ------------------------------------------------------------------------
    // Data loading with background task and loading overlay
    // ------------------------------------------------------------------------
    private void loadData() {
    // Cancel any ongoing task
    if (currentLoadTask != null && currentLoadTask.isRunning()) {
        currentLoadTask.cancel();
    }

    // Get raw values from UI
    LocalDate start = startDatePicker.getValue();
    LocalDate end = endDatePicker.getValue();
    if (start == null) start = LocalDate.now().minusDays(1);
    if (end == null) end = LocalDate.now();

    // Ensure start <= end
    if (start.isAfter(end)) {
        LocalDate temp = start;
        start = end;
        end = temp;
    }

    // FINAL COPIES (required for use inside Task)
    final LocalDate finalStartDate = start;
    final LocalDate finalEndDate = end;

    // Optional: warn if date range is huge (>90 days)
    if (finalStartDate.until(finalEndDate).getDays() > 90) {
        Alert warning = new Alert(Alert.AlertType.CONFIRMATION);
        warning.setTitle("Large Date Range");
        warning.setHeaderText("You selected a range of " + finalStartDate.until(finalEndDate).getDays() + " days.");
        warning.setContentText("This may take a long time and consume a lot of memory. Do you want to continue?");
        Optional<ButtonType> result = warning.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }
    }

    // FINAL copies of filter strings
    final String invoice = invoiceField.getText() == null ? "" : invoiceField.getText().trim();
    final String item = itemField.getText() == null ? "" : itemField.getText().trim();
    final String warehouse = warehouseField.getText() == null ? "" : warehouseField.getText().trim();

    // Show loading overlay
    loadingOverlay.setVisible(true);
    progressIndicator.setVisible(true);
    resultLabel.setText("Loading...");
    totalLabel.setText("Total Cost: --");

    // Background task
    currentLoadTask = new Task<>() {
        @Override
        protected List<ConsumptionDetailRow> call() throws Exception {
            // Use the final variables
            return service.getAll(finalStartDate, finalEndDate, invoice, item, warehouse);
        }

        @Override
        protected void succeeded() {
            loadingOverlay.setVisible(false);
            progressIndicator.setVisible(false);
            List<ConsumptionDetailRow> rows = getValue();
            updateUI(rows);
        }

        @Override
        protected void failed() {
            loadingOverlay.setVisible(false);
            progressIndicator.setVisible(false);
            Throwable ex = getException();
            ex.printStackTrace();
            resultLabel.setText("Error loading data");
            totalLabel.setText("Total Cost: --");
            showAlert("Database Error", ex.getMessage());
        }

        @Override
        protected void cancelled() {
            loadingOverlay.setVisible(false);
            progressIndicator.setVisible(false);
            resultLabel.setText("Loading cancelled");
            totalLabel.setText("Total Cost: --");
        }
    };

    new Thread(currentLoadTask).start();
}

    private void updateUI(List<ConsumptionDetailRow> rows) {
        if (rows == null) rows = List.of();

        // Build tree: Invoice -> Result Description -> Item
        TreeItem<ConsumptionTreeRow> root = new TreeItem<>(
                new ConsumptionTreeRow("ROOT", "", "", "", "", "", "", "", "", "", "")
        );

        Map<Long, List<ConsumptionDetailRow>> byInvoice = rows.stream()
                .collect(Collectors.groupingBy(ConsumptionDetailRow::getInvoiceId));

        for (Map.Entry<Long, List<ConsumptionDetailRow>> invoiceEntry : byInvoice.entrySet()) {
            Long invoiceId = invoiceEntry.getKey();
            List<ConsumptionDetailRow> invoiceRows = invoiceEntry.getValue();

            String invoiceDate = invoiceRows.isEmpty() ? "" : nullSafe(invoiceRows.get(0).getDate());
            double invoiceTotal = invoiceRows.stream().mapToDouble(ConsumptionDetailRow::getTotalCost).sum();

            TreeItem<ConsumptionTreeRow> invoiceNode = new TreeItem<>(
                    new ConsumptionTreeRow(
                            "INVOICE",
                            String.valueOf(invoiceId),
                            invoiceDate,
                            "",
                            "",
                            "",
                            "",
                            "",
                            "",
                            decimalFormat.format(invoiceTotal),
                            ""
                    )
            );

            Map<String, List<ConsumptionDetailRow>> byResult = invoiceRows.stream()
                    .collect(Collectors.groupingBy(ConsumptionDetailRow::getResultDescription));

            for (Map.Entry<String, List<ConsumptionDetailRow>> resultEntry : byResult.entrySet()) {
                String resultDesc = resultEntry.getKey();
                List<ConsumptionDetailRow> resultRows = resultEntry.getValue();

                double resultQty = resultRows.isEmpty() ? 0 : resultRows.get(0).getResultQuantity();
                double resultTotal = resultRows.stream().mapToDouble(ConsumptionDetailRow::getTotalCost).sum();

                TreeItem<ConsumptionTreeRow> resultNode = new TreeItem<>(
                        new ConsumptionTreeRow(
                                "RESULT",
                                "",
                                "",
                                "",
                                nullSafe(resultDesc),
                                "",
                                decimalFormat.format(resultQty),
                                "",
                                "",
                                decimalFormat.format(resultTotal),
                                ""
                        )
                );

                for (ConsumptionDetailRow row : resultRows) {
                    TreeItem<ConsumptionTreeRow> itemNode = new TreeItem<>(
                            new ConsumptionTreeRow(
                                    "ITEM",
                                    "",
                                    "",
                                    nullSafe(row.getCategory()),
                                    "",
                                    nullSafe(row.getItem()),
                                    decimalFormat.format(row.getQuantity()),
                                    nullSafe(row.getUom()),
                                    decimalFormat.format(row.getUnitCost()),
                                    decimalFormat.format(row.getTotalCost()),
                                    nullSafe(row.getWarehouse())
                            )
                    );
                    resultNode.getChildren().add(itemNode);
                }

                invoiceNode.getChildren().add(resultNode);
            }

            invoiceNode.setExpanded(true);
            root.getChildren().add(invoiceNode);
        }

        treeTableView.setRoot(root);
        treeTableView.setShowRoot(false);

        resultLabel.setText(rows.size() + " row(s)");
        double total = rows.stream().mapToDouble(ConsumptionDetailRow::getTotalCost).sum();
        totalLabel.setText("Total Cost: " + decimalFormat.format(total));
    }

    // ------------------------------------------------------------------------
    // Export in background (optional)
    // ------------------------------------------------------------------------
    private void exportInBackground(LocalDate startDate, LocalDate endDate) {
        Task<List<ConsumptionDetailRow>> exportTask = new Task<>() {
            @Override
            protected List<ConsumptionDetailRow> call() throws Exception {
                return service.getAll(
                        startDate,
                        endDate,
                        invoiceField.getText() == null ? "" : invoiceField.getText().trim(),
                        itemField.getText() == null ? "" : itemField.getText().trim(),
                        warehouseField.getText() == null ? "" : warehouseField.getText().trim()
                );
            }

            @Override
            protected void succeeded() {
                List<ConsumptionDetailRow> rows = getValue();
                exportToCsv(rows);
            }

            @Override
            protected void failed() {
                showAlert("Export Error", getException().getMessage());
            }
        };
        new Thread(exportTask).start();
    }

    private void exportToCsv(List<ConsumptionDetailRow> rows) {
    if (rows.isEmpty()) {
        showAlert("No Data", "Nothing to export. Please adjust filters.");
        return;
    }

    try {
        String downloadsPath = System.getProperty("user.home") + File.separator + "Downloads";
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path filePath = Paths.get(downloadsPath, "consumption_detail_" + timestamp + ".csv");

        try (PrintWriter writer = new PrintWriter(filePath.toFile(), "UTF-8")) {
            writer.write('\uFEFF'); // UTF-8 BOM for Excel

            // Header
            writer.println("Invoice,Date,Category,Result Description,Result Quantity,Item,Quantity,UOM,Unit Cost,Total Cost,Warehouse");

            // Data rows – use String.format for numeric values
            for (ConsumptionDetailRow row : rows) {
                writer.println(String.join(",",
                        csvSafe(String.valueOf(row.getInvoiceId())),
                        csvSafe(row.getDate()),
                        csvSafe(row.getCategory()),
                        csvSafe(row.getResultDescription()),
                        formatNumberCSV(row.getResultQuantity()),
                        csvSafe(row.getItem()),
                        formatNumberCSV(row.getQuantity()),
                        csvSafe(row.getUom()),
                        formatNumberCSV(row.getUnitCost()),
                        formatNumberCSV(row.getTotalCost()),
                        csvSafe(row.getWarehouse())
                ));
            }
        }

        showExportSuccessDialog(filePath.toFile());

    } catch (Exception e) {
        e.printStackTrace();
        showAlert("Export Error", "Could not save CSV file.\n" + e.getMessage());
    }
}

// Dedicated CSV number formatter (no thousand separators)
private String formatNumberCSV(double value) {
    return String.format(Locale.US, "%.3f", value);
}

    private String csvSafe(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private void showExportSuccessDialog(File file) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Export Successful");
        alert.setHeaderText("Consumption Detail Exported");
        alert.setContentText("File saved to Downloads:\n" + file.getName());

        ButtonType openFileBtn = new ButtonType("Open File");
        ButtonType openFolderBtn = new ButtonType("Open Folder");
        ButtonType closeBtn = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(openFileBtn, openFolderBtn, closeBtn);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent()) {
            if (result.get() == openFileBtn) {
                try { Desktop.getDesktop().open(file); } catch (Exception e) { e.printStackTrace(); }
            } else if (result.get() == openFolderBtn) {
                try { Desktop.getDesktop().open(file.getParentFile()); } catch (Exception e) { e.printStackTrace(); }
            }
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}