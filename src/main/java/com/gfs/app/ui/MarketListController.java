package com.gfs.app.ui;

import com.gfs.app.model.MarketListRow;
import com.gfs.app.service.MarketListService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;

import java.text.DecimalFormat;
import java.util.List;


import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.awt.Desktop;

public class MarketListController {

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> categoryFilterBox;

    @FXML
    private ComboBox<String> activeFilterBox;

    @FXML
    private ComboBox<String> salesFilterBox;

    @FXML
    private ComboBox<String> stockedFilterBox;

    @FXML
    private ComboBox<String> purchasedFilterBox;

    @FXML
    private Label resultLabel;

    @FXML
    private TableView<MarketListRow> tableView;

    @FXML
    private TableColumn<MarketListRow, String> codeColumn;

    @FXML
    private TableColumn<MarketListRow, String> nameColumn;

    @FXML
    private TableColumn<MarketListRow, String> categoryColumn;

    @FXML
    private TableColumn<MarketListRow, String> inventoryUomColumn;

    @FXML
    private TableColumn<MarketListRow, String> purchaseUomColumn;

    @FXML
    private TableColumn<MarketListRow, String> recipeUomColumn;

    @FXML
    private TableColumn<MarketListRow, String> purchasePriceColumn;

    @FXML
    private TableColumn<MarketListRow, String> averageCostColumn;

    @FXML
    private TableColumn<MarketListRow, String> activeColumn;

    @FXML
    private TableColumn<MarketListRow, String> salesColumn;

    @FXML
    private TableColumn<MarketListRow, String> stockedColumn;

    @FXML
    private TableColumn<MarketListRow, String> purchasedColumn;

    private final MarketListService service = new MarketListService();
    private final DecimalFormat decimalFormat = new DecimalFormat("#,##0.000");

    @FXML
    public void initialize() {
        activeFilterBox.setItems(FXCollections.observableArrayList("ALL", "ACTIVE", "INACTIVE"));
        salesFilterBox.setItems(FXCollections.observableArrayList("ALL", "YES", "NO"));
        stockedFilterBox.setItems(FXCollections.observableArrayList("ALL", "YES", "NO"));
        purchasedFilterBox.setItems(FXCollections.observableArrayList("ALL", "YES", "NO"));

        categoryFilterBox.setItems(FXCollections.observableArrayList(service.getCategories()));
        categoryFilterBox.getItems().add(0, "ALL");
        categoryFilterBox.setValue("ALL");

        activeFilterBox.setValue("ALL");
        salesFilterBox.setValue("ALL");
        stockedFilterBox.setValue("ALL");
        purchasedFilterBox.setValue("ALL");

        codeColumn.setCellValueFactory(data -> new SimpleStringProperty(nullSafe(data.getValue().getItemCode())));
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(nullSafe(data.getValue().getItemName())));
        categoryColumn.setCellValueFactory(data -> new SimpleStringProperty(nullSafe(data.getValue().getCategory())));
        inventoryUomColumn.setCellValueFactory(data -> new SimpleStringProperty(nullSafe(data.getValue().getInventoryUom())));
        purchaseUomColumn.setCellValueFactory(data -> new SimpleStringProperty(nullSafe(data.getValue().getPurchaseUom())));
        recipeUomColumn.setCellValueFactory(data -> new SimpleStringProperty(nullSafe(data.getValue().getRecipeUom())));
        purchasePriceColumn.setCellValueFactory(data -> new SimpleStringProperty(decimalFormat.format(data.getValue().getPurchasePrice())));
        averageCostColumn.setCellValueFactory(data -> new SimpleStringProperty(decimalFormat.format(data.getValue().getAverageCost())));

        activeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isActive() ? "Active" : "Inactive"));
        salesColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isSales() ? "Yes" : "No"));
        stockedColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isStocked() ? "Yes" : "No"));
        purchasedColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isPurchased() ? "Yes" : "No"));

        activeColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    setText(" " + item + " ");
                    setStyle(item.equals("Active")
                        ? "-fx-text-fill: white; -fx-alignment: CENTER; -fx-background-color: #16a34a; -fx-background-radius: 6; -fx-font-weight: bold;"
                        : "-fx-text-fill: white; -fx-alignment: CENTER; -fx-background-color: #dc2626; -fx-background-radius: 6; -fx-font-weight: bold;");
                }
            }
        });

        salesColumn.setCellFactory(column -> createYesNoBadgeCell());
        stockedColumn.setCellFactory(column -> createYesNoBadgeCell());
        purchasedColumn.setCellFactory(column -> createYesNoBadgeCell());

        searchField.setOnAction(event -> handleSearch());
        loadData();
    }

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
        loadData();
    }

    @FXML
    private void handleExport() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim();

        String category = categoryFilterBox.getValue() == null ? "ALL" : categoryFilterBox.getValue();
        String active = activeFilterBox.getValue() == null ? "ALL" : activeFilterBox.getValue();
        String sales = salesFilterBox.getValue() == null ? "ALL" : salesFilterBox.getValue();
        String stocked = stockedFilterBox.getValue() == null ? "ALL" : stockedFilterBox.getValue();
        String purchased = purchasedFilterBox.getValue() == null ? "ALL" : purchasedFilterBox.getValue();

        List<MarketListRow> rows = service.getAll(
                keyword,
                category,
                active,
                sales,
                stocked,
                purchased
        );

        exportToCsv(rows);
    }

    private void exportToCsv(List<MarketListRow> rows) {
        try {
            String downloadsPath = System.getProperty("user.home") + File.separator + "Downloads";
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path filePath = Paths.get(downloadsPath, "market_list_" + timestamp + ".csv");

            try (PrintWriter writer = new PrintWriter(filePath.toFile(), "UTF-8")) {
                // UTF-8 BOM for Excel
                writer.write('\uFEFF');

                writer.println("Code,Item Name,Category,Inventory UOM,Purchase UOM,Recipe UOM,Purchase Price,Average Cost,Active,Sales,Stocked,Purchased");

                for (MarketListRow r : rows) {
                    writer.println(
                            csvSafe(r.getItemCode()) + "," +
                            csvSafe(r.getItemName()) + "," +
                            csvSafe(r.getCategory()) + "," +
                            csvSafe(r.getInventoryUom()) + "," +
                            csvSafe(r.getPurchaseUom()) + "," +
                            csvSafe(r.getRecipeUom()) + "," +
                            formatNumber(r.getPurchasePrice()) + "," +
                            formatNumber(r.getAverageCost()) + "," +
                            csvSafe(r.isActive() ? "Active" : "Inactive") + "," +
                            csvSafe(r.isSales() ? "Yes" : "No") + "," +
                            csvSafe(r.isStocked() ? "Yes" : "No") + "," +
                            csvSafe(r.isPurchased() ? "Yes" : "No")
                    );
                }
            }

           showExportSuccessDialog(filePath.toFile());

        } catch (Exception e) {
            e.printStackTrace();
            resultLabel.setText("Export failed: " + e.getMessage());
        }
    }

   private void showExportSuccessDialog(File file) {
    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);

    alert.setTitle("Export Successful");
    alert.setHeaderText("Market List Exported");

    alert.setContentText("File saved to Downloads:\n" + file.getName());

    // Buttons
    javafx.scene.control.ButtonType openFileBtn = new javafx.scene.control.ButtonType("Open File");
    javafx.scene.control.ButtonType openFolderBtn = new javafx.scene.control.ButtonType("Open Folder");
    javafx.scene.control.ButtonType closeBtn = new javafx.scene.control.ButtonType("Close", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);

    alert.getButtonTypes().setAll(openFileBtn, openFolderBtn, closeBtn);

    java.util.Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();

    if (result.isPresent()) {
        if (result.get() == openFileBtn) {
            openFile(file);
        } else if (result.get() == openFolderBtn) {
            openFolder(file);
        }
    }

    }

    private void openFolder(File file) {
    try {
        if (java.awt.Desktop.isDesktopSupported()) {
            java.awt.Desktop.getDesktop().open(file.getParentFile());
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
    }

    private void openFile(File file) {
    try {
        if (java.awt.Desktop.isDesktopSupported()) {
            java.awt.Desktop.getDesktop().open(file);
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
    }




    private String formatNumber(double value) {
        return String.format("%.3f", value);
    }

    private String csvSafe(String value) {
        if (value == null) {
            return "";
        }

        String cleaned = value.replace("\"", "\"\"");
        return "\"" + cleaned + "\"";
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
                setText(item);
                 setText(" " + item + " ");
                setStyle(item.equals("Yes")
                        ? "-fx-text-fill: white; -fx-alignment: CENTER; -fx-background-color: #2563eb; -fx-background-radius: 8; -fx-font-weight: bold;"
                        : "-fx-text-fill: white; -fx-alignment: CENTER; -fx-background-color: #6b7280; -fx-background-radius: 8; -fx-font-weight: bold;");
            }
        }
    };
}

    private void loadData() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim();
        String category = categoryFilterBox.getValue();

        List<MarketListRow> rows = service.getAll(
        keyword,
        category,
        activeFilterBox.getValue(),
        salesFilterBox.getValue(),
        stockedFilterBox.getValue(),
        purchasedFilterBox.getValue()
    );

        tableView.setItems(FXCollections.observableArrayList(rows));
        resultLabel.setText(rows.size() + " item(s)");
        
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}