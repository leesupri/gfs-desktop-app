package com.gfs.app.ui;

import com.gfs.app.model.WarehouseConsumptionRow;
import com.gfs.app.service.WarehouseConsumptionService;
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
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class WarehouseConsumptionController {

    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private TextField  warehouseField;
    @FXML private TextField  itemField;          // NEW
    @FXML private Label      resultLabel;
    @FXML private Label      totalLabel;
    @FXML private TreeTableView<WarehouseConsumptionTreeRow>            treeTableView;
    @FXML private TreeTableColumn<WarehouseConsumptionTreeRow, String>  warehouseColumn;
    @FXML private TreeTableColumn<WarehouseConsumptionTreeRow, String>  itemColumn;
    @FXML private TreeTableColumn<WarehouseConsumptionTreeRow, String>  uomColumn;
    @FXML private TreeTableColumn<WarehouseConsumptionTreeRow, String>  quantityColumn;
    @FXML private TreeTableColumn<WarehouseConsumptionTreeRow, String>  totalCostColumn;
    @FXML private StackPane  loadingOverlay;
    @FXML private ProgressIndicator progressIndicator;

    private final WarehouseConsumptionService service       = new WarehouseConsumptionService();
    private final DecimalFormat               decimalFormat = createEuropeanDecimalFormat();
    private Task<List<WarehouseConsumptionRow>> currentLoadTask;

    // -------------------------------------------------------------------------
    // Inner tree-row model
    // -------------------------------------------------------------------------
    public static class WarehouseConsumptionTreeRow {
        private final String type; // "WAREHOUSE" or "ITEM"
        private final String warehouse;
        private final String item;
        private final String uom;
        private final String quantity;
        private final String totalCost;

        public WarehouseConsumptionTreeRow(String type, String warehouse, String item,
                                           String uom, String quantity, String totalCost) {
            this.type = type; this.warehouse = warehouse; this.item = item;
            this.uom = uom;   this.quantity = quantity;   this.totalCost = totalCost;
        }

        public String getType()      { return type; }
        public String getWarehouse() { return warehouse; }
        public String getItem()      { return item; }
        public String getUom()       { return uom; }
        public String getQuantity()  { return quantity; }
        public String getTotalCost() { return totalCost; }
    }

    // -------------------------------------------------------------------------
    // Initialise
    // -------------------------------------------------------------------------
    @FXML
    public void initialize() {
        startDatePicker.setValue(LocalDate.now().minusDays(30));
        endDatePicker.setValue(LocalDate.now());

        warehouseColumn.setCellValueFactory(p -> new ReadOnlyStringWrapper(p.getValue().getValue().getWarehouse()));
        itemColumn     .setCellValueFactory(p -> new ReadOnlyStringWrapper(p.getValue().getValue().getItem()));
        uomColumn      .setCellValueFactory(p -> new ReadOnlyStringWrapper(p.getValue().getValue().getUom()));
        quantityColumn .setCellValueFactory(p -> new ReadOnlyStringWrapper(p.getValue().getValue().getQuantity()));
        totalCostColumn.setCellValueFactory(p -> new ReadOnlyStringWrapper(p.getValue().getValue().getTotalCost()));

        // Allow pressing Enter in either filter field to trigger search
        warehouseField.setOnAction(e -> loadData());
        itemField     .setOnAction(e -> loadData());

        loadData();
    }

    // -------------------------------------------------------------------------
    // Actions
    // -------------------------------------------------------------------------
    @FXML private void handleSearch()  { loadData(); }

    @FXML
    private void handleRefresh() {
        startDatePicker.setValue(LocalDate.now().minusDays(30));
        endDatePicker.setValue(LocalDate.now());
        warehouseField.clear();
        itemField.clear();
        loadData();
    }

    @FXML
    private void handleExport() {
        if (currentLoadTask != null && currentLoadTask.isRunning()) {
            showAlert("Loading in Progress", "Please wait for data to finish loading before exporting.");
            return;
        }

        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate   = endDatePicker.getValue();
        if (startDate == null || endDate == null) {
            showAlert("Invalid Dates", "Please select both start and end dates.");
            return;
        }

        final LocalDate finalStart    = startDate;
        final LocalDate finalEnd      = endDate;
        final String    finalWarehouse = text(warehouseField);
        final String    finalItem      = text(itemField);

        loadingOverlay.setVisible(true);

        Task<List<WarehouseConsumptionRow>> exportTask = new Task<>() {
            @Override protected List<WarehouseConsumptionRow> call() throws Exception {
                return service.getAll(finalStart, finalEnd, finalWarehouse, finalItem);
            }
            @Override protected void succeeded() {
                loadingOverlay.setVisible(false);
                exportToCsv(getValue(), finalStart, finalEnd);
            }
            @Override protected void failed() {
                loadingOverlay.setVisible(false);
                showAlert("Export Error", getException().getMessage());
            }
        };
        new Thread(exportTask).start();
    }

    // -------------------------------------------------------------------------
    // Background data load
    // -------------------------------------------------------------------------
    private void loadData() {
        if (currentLoadTask != null && currentLoadTask.isRunning()) {
            currentLoadTask.cancel();
        }

        LocalDate start = startDatePicker.getValue();
        LocalDate end   = endDatePicker.getValue();
        if (start == null) start = LocalDate.now().minusDays(30);
        if (end   == null) end   = LocalDate.now();
        if (start.isAfter(end)) { LocalDate t = start; start = end; end = t; }

        final LocalDate finalStart    = start;
        final LocalDate finalEnd      = end;
        final String    finalWarehouse = text(warehouseField);
        final String    finalItem      = text(itemField);

        loadingOverlay.setVisible(true);
        resultLabel.setText("Loading...");
        totalLabel.setText("Total Cost: --");

        currentLoadTask = new Task<>() {
            @Override protected List<WarehouseConsumptionRow> call() throws Exception {
                return service.getAll(finalStart, finalEnd, finalWarehouse, finalItem);
            }
            @Override protected void succeeded() {
                loadingOverlay.setVisible(false);
                updateUI(getValue());
            }
            @Override protected void failed() {
                loadingOverlay.setVisible(false);
                getException().printStackTrace();
                resultLabel.setText("Error loading data");
                totalLabel.setText("Total Cost: --");
                showAlert("Database Error", getException().getMessage());
            }
            @Override protected void cancelled() {
                loadingOverlay.setVisible(false);
                resultLabel.setText("Loading cancelled");
                totalLabel.setText("Total Cost: --");
            }
        };
        new Thread(currentLoadTask).start();
    }

    // -------------------------------------------------------------------------
    // Tree UI builder
    // -------------------------------------------------------------------------
    private void updateUI(List<WarehouseConsumptionRow> rows) {
        Map<String, List<WarehouseConsumptionRow>> byWarehouse = rows.stream()
                .collect(Collectors.groupingBy(WarehouseConsumptionRow::getWarehouse));

        TreeItem<WarehouseConsumptionTreeRow> root =
                new TreeItem<>(new WarehouseConsumptionTreeRow("ROOT", "", "", "", "", ""));
        root.setExpanded(true);

        for (Map.Entry<String, List<WarehouseConsumptionRow>> entry : byWarehouse.entrySet()) {
            String warehouseName = entry.getKey();
            List<WarehouseConsumptionRow> items = entry.getValue();
            double warehouseTotal = items.stream().mapToDouble(WarehouseConsumptionRow::getTotalCost).sum();

            TreeItem<WarehouseConsumptionTreeRow> warehouseNode = new TreeItem<>(
                new WarehouseConsumptionTreeRow("WAREHOUSE", warehouseName, "", "", "",
                    decimalFormat.format(warehouseTotal)));
            warehouseNode.setExpanded(true);

            for (WarehouseConsumptionRow row : items) {
                warehouseNode.getChildren().add(new TreeItem<>(
                    new WarehouseConsumptionTreeRow("ITEM", "",
                        row.getItem(), row.getUom(),
                        decimalFormat.format(row.getQuantity()),
                        decimalFormat.format(row.getTotalCost()))));
            }
            root.getChildren().add(warehouseNode);
        }

        treeTableView.setRoot(root);
        treeTableView.setShowRoot(false);

        resultLabel.setText(rows.size() + " item(s)");
        double grandTotal = rows.stream().mapToDouble(WarehouseConsumptionRow::getTotalCost).sum();
        totalLabel.setText("Total Cost: " + decimalFormat.format(grandTotal));
    }

    // -------------------------------------------------------------------------
    // CSV Export
    // -------------------------------------------------------------------------
    private void exportToCsv(List<WarehouseConsumptionRow> rows, LocalDate fromDate, LocalDate toDate) {
        if (rows.isEmpty()) {
            showAlert("No Data", "Nothing to export. Please adjust filters.");
            return;
        }
        try {
            String downloadsPath = System.getProperty("user.home") + File.separator + "Downloads";
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path filePath = Paths.get(downloadsPath, "warehouse_consumption_" + timestamp + ".csv");

            try (PrintWriter writer = new PrintWriter(filePath.toFile(), "UTF-8")) {
                writer.write('\uFEFF');
                writer.println("# Report: Sales Consumption per Warehouse");
                writer.println("# Date From:        " + fromDate);
                writer.println("# Date To:          " + toDate);
                writer.println("# Warehouse Filter: " + (warehouseField.getText().isBlank() ? "All" : warehouseField.getText()));
                writer.println("# Item Filter:      " + (itemField.getText().isBlank()      ? "All" : itemField.getText()));
                writer.println();
                writer.println("Warehouse,Item,UOM,Quantity,Total Cost");

                for (WarehouseConsumptionRow row : rows) {
                    writer.println(String.join(",",
                        csvSafe(row.getWarehouse()),
                        csvSafe(row.getItem()),
                        csvSafe(row.getUom()),
                        formatNumberEuropean(row.getQuantity()),
                        formatNumberEuropean(row.getTotalCost())
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
    // Helpers
    // -------------------------------------------------------------------------
    private static DecimalFormat createEuropeanDecimalFormat() {
        DecimalFormat df = new DecimalFormat("#,###.00");
        df.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.GERMANY));
        return df;
    }

    private String formatNumberEuropean(double value) {
        if (value == 0.0) return "\"0,00\"";
        DecimalFormat df = new DecimalFormat("#,###.00");
        df.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.GERMANY));
        return "\"" + df.format(value) + "\"";
    }

    private String csvSafe(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    /** Safely read a TextField — returns "" if null or blank. */
    private static String text(TextField field) {
        return (field == null || field.getText() == null) ? "" : field.getText().trim();
    }

    private void showExportSuccessDialog(File file) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Export Successful");
        alert.setHeaderText("Report Exported");
        alert.setContentText("File saved to Downloads:\n" + file.getName());

        ButtonType openFileBtn   = new ButtonType("Open File");
        ButtonType openFolderBtn = new ButtonType("Open Folder");
        ButtonType closeBtn      = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
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
}