package com.gfs.app.ui;

import com.gfs.app.model.ItemSalesRow;
import com.gfs.app.model.ItemSalesTreeRow;
import com.gfs.app.service.ItemSalesService;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
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
import java.util.*;
import java.util.stream.Collectors;

public class ItemSalesReportController {

    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private TextField itemSearchField;
    @FXML private ComboBox<String> departmentFilter;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private TreeTableView<ItemSalesTreeRow> treeTableView;
    @FXML private TreeTableColumn<ItemSalesTreeRow, String> departmentColumn;
    @FXML private TreeTableColumn<ItemSalesTreeRow, String> categoryColumn;
    @FXML private TreeTableColumn<ItemSalesTreeRow, String> itemNameColumn;
    @FXML private TreeTableColumn<ItemSalesTreeRow, String> itemCodeColumn;
    @FXML private TreeTableColumn<ItemSalesTreeRow, String> quantityColumn;
    @FXML private TreeTableColumn<ItemSalesTreeRow, String> subtotalColumn;
    @FXML private TreeTableColumn<ItemSalesTreeRow, String> costColumn;
    @FXML private TreeTableColumn<ItemSalesTreeRow, String> discountColumn;
    @FXML private TreeTableColumn<ItemSalesTreeRow, String> serviceChargeColumn;
    @FXML private TreeTableColumn<ItemSalesTreeRow, String> taxColumn;
    @FXML private TreeTableColumn<ItemSalesTreeRow, String> profitColumn;
    @FXML private TreeTableColumn<ItemSalesTreeRow, String> totalColumn;
    @FXML private TreeTableColumn<ItemSalesTreeRow, String> costPercentageColumn;
    @FXML private Label resultLabel;
    @FXML private StackPane loadingOverlay;
    @FXML private ProgressIndicator progressIndicator;

    private final ItemSalesService service = new ItemSalesService();
    private final DecimalFormat decimalFormat = createEuropeanDecimalFormat();
    private Task<List<ItemSalesRow>> currentLoadTask;

    @FXML
    public void initialize() {
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now());

        // Bind columns
        departmentColumn.setCellValueFactory(p -> new ReadOnlyStringWrapper(p.getValue().getValue().getDepartment()));
        categoryColumn.setCellValueFactory(p -> new ReadOnlyStringWrapper(p.getValue().getValue().getCategory()));
        itemNameColumn.setCellValueFactory(p -> new ReadOnlyStringWrapper(p.getValue().getValue().getItemName()));
        itemCodeColumn.setCellValueFactory(p -> new ReadOnlyStringWrapper(p.getValue().getValue().getItemCode()));
        quantityColumn.setCellValueFactory(p -> new ReadOnlyStringWrapper(p.getValue().getValue().getQuantity()));
        subtotalColumn.setCellValueFactory(p -> new ReadOnlyStringWrapper(p.getValue().getValue().getSubtotal()));
        costColumn.setCellValueFactory(p -> new ReadOnlyStringWrapper(p.getValue().getValue().getCost()));
        discountColumn.setCellValueFactory(p -> new ReadOnlyStringWrapper(p.getValue().getValue().getDiscount()));
        serviceChargeColumn.setCellValueFactory(p -> new ReadOnlyStringWrapper(p.getValue().getValue().getServiceCharge()));
        taxColumn.setCellValueFactory(p -> new ReadOnlyStringWrapper(p.getValue().getValue().getTax()));
        profitColumn.setCellValueFactory(p -> new ReadOnlyStringWrapper(p.getValue().getValue().getProfit()));
        totalColumn.setCellValueFactory(p -> new ReadOnlyStringWrapper(p.getValue().getValue().getTotal()));
        costPercentageColumn.setCellValueFactory(p -> new ReadOnlyStringWrapper(p.getValue().getValue().getCostPercentage()));

        // Colour the cost percentage column
        setupCostPercentageColoring();

        // Load department filter dropdown
        List<String> depts = service.getDepartments();
        depts.add(0, "ALL");
        departmentFilter.setItems(FXCollections.observableArrayList(depts));
        departmentFilter.setValue("ALL");
        categoryFilter.setItems(FXCollections.observableArrayList("ALL"));
        categoryFilter.setValue("ALL");

        // Listen to department change to update categories
        departmentFilter.valueProperty().addListener((obs, old, dept) -> {
            String selectedDept = dept.equals("ALL") ? "" : dept;
            List<String> cats = service.getCategories(selectedDept);
            cats.add(0, "ALL");
            categoryFilter.setItems(FXCollections.observableArrayList(cats));
            categoryFilter.setValue("ALL");
            loadData();
        });

        // Listen to other filters
        itemSearchField.setOnAction(e -> loadData());
        categoryFilter.valueProperty().addListener((obs, old, cat) -> loadData());
        startDatePicker.valueProperty().addListener((obs, old, val) -> loadData());
        endDatePicker.valueProperty().addListener((obs, old, val) -> loadData());

        loadData();
    }

    @FXML
    private void handleSearch() {
        loadData();
    }

    @FXML
    private void handleReset() {
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now());
        itemSearchField.clear();
        departmentFilter.setValue("ALL");
        categoryFilter.setValue("ALL");
        loadData();
    }

    @FXML
    private void handleExport() {
        if (currentLoadTask != null && currentLoadTask.isRunning()) {
            showAlert("Loading in Progress", "Please wait for data to finish loading.");
            return;
        }

        LocalDate startRaw = startDatePicker.getValue();
        LocalDate endRaw = endDatePicker.getValue();
        if (startRaw == null) startRaw = LocalDate.now().minusDays(30);
        if (endRaw == null) endRaw = LocalDate.now();
        if (startRaw.isAfter(endRaw)) {
            LocalDate temp = startRaw;
            startRaw = endRaw;
            endRaw = temp;
        }
        final LocalDate finalStart = startRaw;
        final LocalDate finalEnd = endRaw;

        final String search = itemSearchField.getText() == null ? "" : itemSearchField.getText().trim();
        final String dept = departmentFilter.getValue() == null ? "ALL" : departmentFilter.getValue();
        final String cat = categoryFilter.getValue() == null ? "ALL" : categoryFilter.getValue();

        loadingOverlay.setVisible(true);

        Task<List<ItemSalesRow>> exportTask = new Task<>() {
            @Override
            protected List<ItemSalesRow> call() {
                return service.getAll(finalStart, finalEnd, search, dept, cat);
            }
            @Override
            protected void succeeded() {
                loadingOverlay.setVisible(false);
                exportToCsv(getValue());
            }
            @Override
            protected void failed() {
                loadingOverlay.setVisible(false);
                showAlert("Export Error", getException().getMessage());
            }
        };
        new Thread(exportTask).start();
    }

    private static DecimalFormat createEuropeanDecimalFormat() {
    DecimalFormat df = new DecimalFormat("#,###.00");
    DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.GERMANY);
    df.setDecimalFormatSymbols(symbols);
    return df;
}

    private void loadData() {
        if (currentLoadTask != null && currentLoadTask.isRunning()) {
            currentLoadTask.cancel();
        }
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();
        if (start == null) start = LocalDate.now().minusDays(30);
        if (end == null) end = LocalDate.now();
        if (start.isAfter(end)) {
            LocalDate temp = start;
            start = end;
            end = temp;
        }
        final LocalDate finalStart = start;
        final LocalDate finalEnd = end;
        final String search = itemSearchField.getText() == null ? "" : itemSearchField.getText().trim();
        final String dept = departmentFilter.getValue() == null ? "ALL" : departmentFilter.getValue();
        final String cat = categoryFilter.getValue() == null ? "ALL" : categoryFilter.getValue();

        loadingOverlay.setVisible(true);
        resultLabel.setText("Loading...");

        currentLoadTask = new Task<>() {
            @Override
            protected List<ItemSalesRow> call() {
                return service.getAll(finalStart, finalEnd, search, dept, cat);
            }
            @Override
            protected void succeeded() {
                loadingOverlay.setVisible(false);
                List<ItemSalesRow> rows = getValue();
                updateUI(rows);
            }
            @Override
            protected void failed() {
                loadingOverlay.setVisible(false);
                Throwable ex = getException();
                ex.printStackTrace();
                resultLabel.setText("Error loading data");
                showAlert("Database Error", ex.getMessage());
            }
            @Override
            protected void cancelled() {
                loadingOverlay.setVisible(false);
                resultLabel.setText("Loading cancelled");
            }
        };
        new Thread(currentLoadTask).start();
    }

    private void updateUI(List<ItemSalesRow> rows) {
        // Group by department, then category
        Map<String, List<ItemSalesRow>> byDept = rows.stream()
                .collect(Collectors.groupingBy(ItemSalesRow::getDepartment));

        // Root node with 14 arguments (type + 13 fields)
        TreeItem<ItemSalesTreeRow> root = new TreeItem<>(new ItemSalesTreeRow(
            "ROOT", "", "", "", "", "", "", "", "", "", "", "", "", ""
        ));
        root.setExpanded(true);

        for (Map.Entry<String, List<ItemSalesRow>> deptEntry : byDept.entrySet()) {
            String deptName = deptEntry.getKey();
            List<ItemSalesRow> deptRows = deptEntry.getValue();

            // Calculate department totals
            double deptQty = deptRows.stream().mapToDouble(ItemSalesRow::getQuantity).sum();
            double deptSub = deptRows.stream().mapToDouble(ItemSalesRow::getSubtotal).sum();
            double deptCost = deptRows.stream().mapToDouble(ItemSalesRow::getCost).sum();
            double deptDisc = deptRows.stream().mapToDouble(ItemSalesRow::getDiscount).sum();
            double deptServ = deptRows.stream().mapToDouble(ItemSalesRow::getServiceCharge).sum();
            double deptTax = deptRows.stream().mapToDouble(ItemSalesRow::getTax).sum();
            double deptProfit = deptRows.stream().mapToDouble(ItemSalesRow::getProfit).sum();
            double deptTotal = deptRows.stream().mapToDouble(ItemSalesRow::getTotal).sum();
            double deptCostPct = (deptSub == 0) ? 0 : (deptCost / deptSub) * 100;
            String deptCostPctStr = String.format("%.1f%%", deptCostPct);

            // Department node
            TreeItem<ItemSalesTreeRow> deptNode = new TreeItem<>(new ItemSalesTreeRow(
                "DEPARTMENT", deptName, null, null, null,
                decimalFormat.format(deptQty), decimalFormat.format(deptSub),
                decimalFormat.format(deptCost), decimalFormat.format(deptDisc),
                decimalFormat.format(deptServ), decimalFormat.format(deptTax),
                decimalFormat.format(deptProfit),   // costPercentage not shown at dept level
                decimalFormat.format(deptTotal), deptCostPctStr
            ));
            deptNode.setExpanded(true);

            // Group by category within department
            Map<String, List<ItemSalesRow>> byCat = deptRows.stream()
                    .collect(Collectors.groupingBy(ItemSalesRow::getCategory));
            for (Map.Entry<String, List<ItemSalesRow>> catEntry : byCat.entrySet()) {
                String catName = catEntry.getKey();
                List<ItemSalesRow> catRows = catEntry.getValue();

                double catQty = catRows.stream().mapToDouble(ItemSalesRow::getQuantity).sum();
                double catSub = catRows.stream().mapToDouble(ItemSalesRow::getSubtotal).sum();
                double catCost = catRows.stream().mapToDouble(ItemSalesRow::getCost).sum();
                double catDisc = catRows.stream().mapToDouble(ItemSalesRow::getDiscount).sum();
                double catServ = catRows.stream().mapToDouble(ItemSalesRow::getServiceCharge).sum();
                double catTax = catRows.stream().mapToDouble(ItemSalesRow::getTax).sum();
                double catProfit = catRows.stream().mapToDouble(ItemSalesRow::getProfit).sum();
                double catTotal = catRows.stream().mapToDouble(ItemSalesRow::getTotal).sum();
                double catCostPct = (catSub == 0) ? 0 : (catCost / catSub) * 100;
                String catCostPctStr = String.format("%.1f%%", catCostPct);

                // Category node
                TreeItem<ItemSalesTreeRow> catNode = new TreeItem<>(new ItemSalesTreeRow(
                    "CATEGORY", deptName, catName, null, null,
                    decimalFormat.format(catQty), decimalFormat.format(catSub),
                    decimalFormat.format(catCost), decimalFormat.format(catDisc),
                    decimalFormat.format(catServ), decimalFormat.format(catTax),
                    decimalFormat.format(catProfit), 
                    decimalFormat.format(catTotal), catCostPctStr
                ));
                catNode.setExpanded(true);

                // Add item rows
                for (ItemSalesRow row : catRows) {
                    String costPct = String.format("%.1f%%", row.getCostPercentage());
                    TreeItem<ItemSalesTreeRow> itemNode = new TreeItem<>(new ItemSalesTreeRow(
                        "ITEM", null, null, row.getItemName(), row.getItemCode(),
                        decimalFormat.format(row.getQuantity()), decimalFormat.format(row.getSubtotal()),
                        decimalFormat.format(row.getCost()), decimalFormat.format(row.getDiscount()),
                        decimalFormat.format(row.getServiceCharge()), decimalFormat.format(row.getTax()),
                        decimalFormat.format(row.getProfit()), 
                        decimalFormat.format(row.getTotal()), costPct
                    ));
                    catNode.getChildren().add(itemNode);
                }
                deptNode.getChildren().add(catNode);
            }
            root.getChildren().add(deptNode);
        }

        treeTableView.setRoot(root);
        treeTableView.setShowRoot(false);
        resultLabel.setText(rows.size() + " item(s)");
    }

    private void setupCostPercentageColoring() {
        costPercentageColumn.setCellFactory(col -> new TreeTableCell<ItemSalesTreeRow, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isEmpty()) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    double pct = 0;
                    try {
                        String num = item.replace("%", "").trim();
                        pct = Double.parseDouble(num);
                    } catch (NumberFormatException e) { /* ignore */ }
                    if (pct < 40) {
                        setStyle("-fx-background-color: #86efac; -fx-text-fill: #14532d;");
                    } else if (pct < 75) {
                        setStyle("-fx-background-color: #fde047; -fx-text-fill: #854d0e;");
                    } else {
                        setStyle("-fx-background-color: #f87171; -fx-text-fill: #7f1a1a;");
                    }
                }
            }
        });
    }

    // ------------------------------------------------------------------------
    // CSV Export
    // ------------------------------------------------------------------------
    private void exportToCsv(List<ItemSalesRow> rows) {
        if (rows.isEmpty()) {
            showAlert("No Data", "Nothing to export.");
            return;
        }
        try {
            String downloadsPath = System.getProperty("user.home") + File.separator + "Downloads";
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path filePath = Paths.get(downloadsPath, "item_sales_report_" + timestamp + ".csv");

            try (PrintWriter writer = new PrintWriter(filePath.toFile(), "UTF-8")) {
                writer.write('\uFEFF');
                writer.println("Department,Category,Item Name,Item Code,Quantity,Revenue,Cost,Discount,Service Charge,Tax,Profit,Total,Cost %");

                for (ItemSalesRow row : rows) {
                    writer.println(String.join(",",
                            csvSafe(row.getDepartment()),
                            csvSafe(row.getCategory()),
                            csvSafe(row.getItemName()),
                            csvSafe(row.getItemCode()),
                            formatNumberEuropean(row.getQuantity()),
                            formatNumberEuropean(row.getSubtotal()),
                            formatNumberEuropean(row.getCost()),
                            formatNumberEuropean(row.getDiscount()),
                            formatNumberEuropean(row.getServiceCharge()),
                            formatNumberEuropean(row.getTax()),
                            formatNumberEuropean(row.getProfit()),
                            formatNumberEuropean(row.getTotal()),
                            formatNumberEuropean(row.getCostPercentage())
                    ));
                }
            }
            showExportSuccessDialog(filePath.toFile());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Export Error", e.getMessage());
        }
    }

    private String formatNumberEuropean(double value) {
        if (value == 0.0) return "\"0,00\"";
        DecimalFormat df = new DecimalFormat("#,###.00");
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.GERMANY);
        df.setDecimalFormatSymbols(symbols);
        return "\"" + df.format(value) + "\"";
    }

    private String csvSafe(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private void showExportSuccessDialog(File file) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Export Successful");
        alert.setHeaderText("Item Sales Report Exported");
        alert.setContentText("File saved to Downloads:\n" + file.getName());
        ButtonType openFile = new ButtonType("Open File");
        ButtonType openFolder = new ButtonType("Open Folder");
        ButtonType close = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(openFile, openFolder, close);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent()) {
            if (result.get() == openFile) {
                try { Desktop.getDesktop().open(file); } catch (Exception e) { e.printStackTrace(); }
            } else if (result.get() == openFolder) {
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