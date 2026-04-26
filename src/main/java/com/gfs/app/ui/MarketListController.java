package com.gfs.app.ui;

import com.gfs.app.model.MarketListRow;
import com.gfs.app.service.MarketListService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class MarketListController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryFilterBox;
    @FXML private ComboBox<String> activeFilterBox;
    @FXML private ComboBox<String> salesFilterBox;
    @FXML private ComboBox<String> stockedFilterBox;
    @FXML private ComboBox<String> purchasedFilterBox;
    @FXML private Label resultLabel;
    @FXML private TableView<MarketListRow> tableView;
    @FXML private TableColumn<MarketListRow, String> codeColumn;
    @FXML private TableColumn<MarketListRow, String> nameColumn;
    @FXML private TableColumn<MarketListRow, String> categoryColumn;
    @FXML private TableColumn<MarketListRow, String> inventoryUomColumn;
    @FXML private TableColumn<MarketListRow, String> purchaseUomColumn;
    @FXML private TableColumn<MarketListRow, String> recipeUomColumn;
    @FXML private TableColumn<MarketListRow, String> purchasePriceColumn;
    @FXML private TableColumn<MarketListRow, String> averageCostColumn;
    @FXML private TableColumn<MarketListRow, String> activeColumn;
    @FXML private TableColumn<MarketListRow, String> salesColumn;
    @FXML private TableColumn<MarketListRow, String> stockedColumn;
    @FXML private TableColumn<MarketListRow, String> purchasedColumn;
    @FXML private StackPane loadingOverlay;
    @FXML private ProgressIndicator progressIndicator;

    private final MarketListService service = new MarketListService();
    private final DecimalFormat decimalFormat = new DecimalFormat("#,##0.000");
    private Task<List<MarketListRow>> currentLoadTask;
    private Task<Void> currentExportTask;

    @FXML
    public void initialize() {
        // Setup filter combos
        activeFilterBox.setItems(FXCollections.observableArrayList("ALL", "ACTIVE", "INACTIVE"));
        salesFilterBox.setItems(FXCollections.observableArrayList("ALL", "YES", "NO"));
        stockedFilterBox.setItems(FXCollections.observableArrayList("ALL", "YES", "NO"));
        purchasedFilterBox.setItems(FXCollections.observableArrayList("ALL", "YES", "NO"));

        // Load categories from DB (in background? but it's usually small, keep sync)
        List<String> categories = service.getCategories();
        categoryFilterBox.setItems(FXCollections.observableArrayList(categories));
        categoryFilterBox.getItems().add(0, "ALL");
        categoryFilterBox.setValue("ALL");

        activeFilterBox.setValue("ALL");
        salesFilterBox.setValue("ALL");
        stockedFilterBox.setValue("ALL");
        purchasedFilterBox.setValue("ALL");

        // Setup columns
        codeColumn.setCellValueFactory(data -> new SimpleStringProperty(nullSafe(data.getValue().getItemCode())));
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(nullSafe(data.getValue().getItemName())));
        categoryColumn.setCellValueFactory(data -> new SimpleStringProperty(nullSafe(data.getValue().getCategory())));
        inventoryUomColumn.setCellValueFactory(data -> new SimpleStringProperty(nullSafe(data.getValue().getInventoryUom())));
        purchaseUomColumn.setCellValueFactory(data -> new SimpleStringProperty(nullSafe(data.getValue().getPurchaseUom())));
        recipeUomColumn.setCellValueFactory(data -> new SimpleStringProperty(nullSafe(data.getValue().getRecipeUom())));

        // Numeric columns with proper sorting
        purchasePriceColumn.setCellValueFactory(data -> new SimpleStringProperty(decimalFormat.format(data.getValue().getPurchasePrice())));
        averageCostColumn.setCellValueFactory(data -> new SimpleStringProperty(decimalFormat.format(data.getValue().getAverageCost())));

        purchasePriceColumn.setComparator((a, b) -> compareNumericStrings(a, b));
        averageCostColumn.setComparator((a, b) -> compareNumericStrings(a, b));

        // Badge columns
        activeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isActive() ? "Active" : "Inactive"));
        salesColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isSales() ? "Yes" : "No"));
        stockedColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isStocked() ? "Yes" : "No"));
        purchasedColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isPurchased() ? "Yes" : "No"));

        activeColumn.setCellFactory(column -> createBadgeCell("Active", "Inactive"));
        salesColumn.setCellFactory(column -> createYesNoBadgeCell());
        stockedColumn.setCellFactory(column -> createYesNoBadgeCell());
        purchasedColumn.setCellFactory(column -> createYesNoBadgeCell());

        searchField.setOnAction(event -> handleSearch());

        loadData();
    }

    // ------------------------------------------------------------------------
    // Numeric comparator helper
    // ------------------------------------------------------------------------
    private int compareNumericStrings(String a, String b) {
        try {
            double d1 = Double.parseDouble(a.replace(",", ""));
            double d2 = Double.parseDouble(b.replace(",", ""));
            return Double.compare(d1, d2);
        } catch (NumberFormatException e) {
            return a.compareTo(b);
        }
    }

    // ------------------------------------------------------------------------
    // Actions
    // ------------------------------------------------------------------------
    @FXML
    private void handleSearch() {
        loadData();
    }

    @FXML
    private void handleRefresh() {
        searchField.clear();
        activeFilterBox.setValue("ALL");
        salesFilterBox.setValue("ALL");
        stockedFilterBox.setValue("ALL");
        purchasedFilterBox.setValue("ALL");
        categoryFilterBox.setValue("ALL");
        loadData();
    }

    @FXML
    private void handleExport() {
        if (currentLoadTask != null && currentLoadTask.isRunning()) {
            showAlert("Loading in Progress", "Please wait for data to finish loading before exporting.");
            return;
        }
        if (currentExportTask != null && currentExportTask.isRunning()) {
            showAlert("Export in Progress", "An export is already running. Please wait.");
            return;
        }

        final String keyword = searchField.getText() == null ? "" : searchField.getText().trim();
        final String category = categoryFilterBox.getValue() == null ? "ALL" : categoryFilterBox.getValue();
        final String active = activeFilterBox.getValue() == null ? "ALL" : activeFilterBox.getValue();
        final String sales = salesFilterBox.getValue() == null ? "ALL" : salesFilterBox.getValue();
        final String stocked = stockedFilterBox.getValue() == null ? "ALL" : stockedFilterBox.getValue();
        final String purchased = purchasedFilterBox.getValue() == null ? "ALL" : purchasedFilterBox.getValue();

        // Show loading overlay on export as well
        loadingOverlay.setVisible(true);
        progressIndicator.setVisible(true);

        currentExportTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                List<MarketListRow> rows = service.getAll(keyword, category, active, sales, stocked, purchased);
                exportToCsv(rows);
                return null;
            }

            @Override
            protected void succeeded() {
                loadingOverlay.setVisible(false);
            }

            @Override
            protected void failed() {
                loadingOverlay.setVisible(false);
                Throwable ex = getException();
                ex.printStackTrace();
                showAlert("Export Error", ex.getMessage());
            }
        };
        new Thread(currentExportTask).start();
    }

    // ------------------------------------------------------------------------
    // Background loading
    // ------------------------------------------------------------------------
    private void loadData() {
        if (currentLoadTask != null && currentLoadTask.isRunning()) {
            currentLoadTask.cancel();
        }

        final String keyword = searchField.getText() == null ? "" : searchField.getText().trim();
        final String category = categoryFilterBox.getValue() == null ? "ALL" : categoryFilterBox.getValue();
        final String active = activeFilterBox.getValue() == null ? "ALL" : activeFilterBox.getValue();
        final String sales = salesFilterBox.getValue() == null ? "ALL" : salesFilterBox.getValue();
        final String stocked = stockedFilterBox.getValue() == null ? "ALL" : stockedFilterBox.getValue();
        final String purchased = purchasedFilterBox.getValue() == null ? "ALL" : purchasedFilterBox.getValue();

        loadingOverlay.setVisible(true);
        progressIndicator.setVisible(true);
        resultLabel.setText("Loading...");

        currentLoadTask = new Task<>() {
            @Override
            protected List<MarketListRow> call() throws Exception {
                return service.getAll(keyword, category, active, sales, stocked, purchased);
            }

            @Override
            protected void succeeded() {
                loadingOverlay.setVisible(false);
                progressIndicator.setVisible(false);
                List<MarketListRow> rows = getValue();
                tableView.setItems(FXCollections.observableArrayList(rows));
                resultLabel.setText(rows.size() + " item(s)");
            }

            @Override
            protected void failed() {
                loadingOverlay.setVisible(false);
                progressIndicator.setVisible(false);
                Throwable ex = getException();
                ex.printStackTrace();
                resultLabel.setText("Error loading data");
                showAlert("Database Error", ex.getMessage());
            }

            @Override
            protected void cancelled() {
                loadingOverlay.setVisible(false);
                progressIndicator.setVisible(false);
                resultLabel.setText("Loading cancelled");
            }
        };
        new Thread(currentLoadTask).start();
    }

    // ------------------------------------------------------------------------
    // CSV Export logic (same as before, but with better error handling)
    // ------------------------------------------------------------------------
    private void exportToCsv(List<MarketListRow> rows) {
        if (rows.isEmpty()) {
            Platform.runLater(() -> {
                loadingOverlay.setVisible(false);
                showAlert("No Data", "Nothing to export. Please adjust filters.");
            });
            return;
        }

        try {
            String downloadsPath = System.getProperty("user.home") + File.separator + "Downloads";
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path filePath = Paths.get(downloadsPath, "market_list_" + timestamp + ".csv");

            try (PrintWriter writer = new PrintWriter(filePath.toFile(), "UTF-8")) {
                writer.write('\uFEFF'); // UTF-8 BOM for Excel
                writer.println("Code,Item Name,Category,Inventory UOM,Purchase UOM,Recipe UOM,Purchase Price,Average Cost,Active,Sales,Stocked,Purchased");

                for (MarketListRow r : rows) {
                    writer.println(String.join(",",
                            csvSafe(r.getItemCode()),
                            csvSafe(r.getItemName()),
                            csvSafe(r.getCategory()),
                            csvSafe(r.getInventoryUom()),
                            csvSafe(r.getPurchaseUom()),
                            csvSafe(r.getRecipeUom()),
                            csvSafe(formatNumberCSV(r.getPurchasePrice())),
                            csvSafe(formatNumberCSV(r.getAverageCost())),
                            csvSafe(r.isActive() ? "Active" : "Inactive"),
                            csvSafe(r.isSales() ? "Yes" : "No"),
                            csvSafe(r.isStocked() ? "Yes" : "No"),
                            csvSafe(r.isPurchased() ? "Yes" : "No")
                    ));
                }
            }

            Platform.runLater(() -> showExportSuccessDialog(filePath.toFile()));

        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> showAlert("Export Error", "Could not save CSV file.\n" + e.getMessage()));
        }
    }

    private String formatNumberCSV(double value) {
        return String.format(Locale.US, "%.3f", value);
    }

    private String csvSafe(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    // ------------------------------------------------------------------------
    // UI Helpers
    // ------------------------------------------------------------------------
    private TableCell<MarketListRow, String> createBadgeCell(String activeText, String inactiveText) {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    setText(" " + item + " ");
                    boolean isActive = item.equals(activeText);
                    setStyle(isActive
                            ? "-fx-text-fill: white; -fx-alignment: CENTER; -fx-background-color: #16a34a; -fx-background-radius: 6; -fx-font-weight: bold;"
                            : "-fx-text-fill: white; -fx-alignment: CENTER; -fx-background-color: #dc2626; -fx-background-radius: 6; -fx-font-weight: bold;");
                }
            }
        };
    }

    private TableCell<MarketListRow, String> createYesNoBadgeCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    setText(" " + item + " ");
                    setStyle(item.equals("Yes")
                            ? "-fx-text-fill: white; -fx-alignment: CENTER; -fx-background-color: #2563eb; -fx-background-radius: 8; -fx-font-weight: bold;"
                            : "-fx-text-fill: white; -fx-alignment: CENTER; -fx-background-color: #6b7280; -fx-background-radius: 8; -fx-font-weight: bold;");
                }
            }
        };
    }

    private void showExportSuccessDialog(File file) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Export Successful");
        alert.setHeaderText("Market List Exported");
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