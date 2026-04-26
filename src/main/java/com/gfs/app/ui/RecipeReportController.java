package com.gfs.app.ui;

import com.gfs.app.model.RecipeRow;
import com.gfs.app.model.RecipeTreeRow;
import com.gfs.app.service.RecipeService;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.StackPane;
import javafx.collections.FXCollections;

import java.awt.Desktop;
import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class RecipeReportController {

    @FXML private TreeTableView<RecipeTreeRow> treeTableView;
    @FXML private TreeTableColumn<RecipeTreeRow, String> recipeNameColumn;
    @FXML private TreeTableColumn<RecipeTreeRow, String> activeColumn;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> salesFilter;
    @FXML private ComboBox<String> purchasedFilter;
    @FXML private ComboBox<String> stockedFilter;
    @FXML private ComboBox<String> activeFilter;   // NEW: replaces activeOnlyCheckBox
    @FXML private TreeTableColumn<RecipeTreeRow, String> salesColumn;
    @FXML private TreeTableColumn<RecipeTreeRow, String> purchasedColumn;
    @FXML private TreeTableColumn<RecipeTreeRow, String> stockedColumn;
    @FXML private TreeTableColumn<RecipeTreeRow, String> productionColumn;
    @FXML private TreeTableColumn<RecipeTreeRow, String> uomColumn;
    @FXML private TreeTableColumn<RecipeTreeRow, String> itemNameColumn;
    @FXML private TreeTableColumn<RecipeTreeRow, String> recQtyColumn;
    @FXML private TreeTableColumn<RecipeTreeRow, String> recipeUomColumn;
    @FXML private TreeTableColumn<RecipeTreeRow, String> invQtyColumn;
    @FXML private TreeTableColumn<RecipeTreeRow, String> invUomColumn;
    @FXML private TreeTableColumn<RecipeTreeRow, String> unitCostColumn;
    @FXML private TreeTableColumn<RecipeTreeRow, String> totalCostColumn;
    @FXML private TreeTableColumn<RecipeTreeRow, String> costPerProductionColumn;
    @FXML private Label resultLabel;
    @FXML private StackPane loadingOverlay;
    @FXML private ProgressIndicator progressIndicator;

    private final RecipeService service = new RecipeService();
    private final DecimalFormat decimalFormat = new DecimalFormat("#,##0.000");
    private Task<List<RecipeRow>> currentLoadTask;

    @FXML
    public void initialize() {
        // Populate combo boxes
        salesFilter.setItems(FXCollections.observableArrayList("ALL", "YES", "NO"));
        purchasedFilter.setItems(FXCollections.observableArrayList("ALL", "YES", "NO"));
        stockedFilter.setItems(FXCollections.observableArrayList("ALL", "YES", "NO"));
        activeFilter.setItems(FXCollections.observableArrayList("ALL", "YES", "NO"));

        salesFilter.setValue("ALL");
        purchasedFilter.setValue("ALL");
        stockedFilter.setValue("ALL");
        activeFilter.setValue("ALL");

        // Event listeners for all filters
        searchField.setOnAction(e -> loadData());
        salesFilter.valueProperty().addListener((obs, old, val) -> loadData());
        purchasedFilter.valueProperty().addListener((obs, old, val) -> loadData());
        stockedFilter.valueProperty().addListener((obs, old, val) -> loadData());
        activeFilter.valueProperty().addListener((obs, old, val) -> loadData());

        // Bind columns
        recipeNameColumn.setCellValueFactory(p -> new ReadOnlyStringWrapper(p.getValue().getValue().getRecipeName()));
        activeColumn.setCellValueFactory(p -> new ReadOnlyStringWrapper(p.getValue().getValue().getRecipeActive()));
        salesColumn.setCellValueFactory(p -> new ReadOnlyStringWrapper(p.getValue().getValue().getSales()));
        purchasedColumn.setCellValueFactory(p -> new ReadOnlyStringWrapper(p.getValue().getValue().getPurchased()));
        stockedColumn.setCellValueFactory(p -> new ReadOnlyStringWrapper(p.getValue().getValue().getStocked()));
        productionColumn.setCellValueFactory(p -> new ReadOnlyStringWrapper(p.getValue().getValue().getProduction()));
        uomColumn.setCellValueFactory(p -> new ReadOnlyStringWrapper(p.getValue().getValue().getUom()));
        itemNameColumn.setCellValueFactory(p -> new ReadOnlyStringWrapper(p.getValue().getValue().getItemName()));
        recQtyColumn.setCellValueFactory(p -> new ReadOnlyStringWrapper(p.getValue().getValue().getRecQty()));
        recipeUomColumn.setCellValueFactory(p -> new ReadOnlyStringWrapper(p.getValue().getValue().getRecipeUom()));
        invQtyColumn.setCellValueFactory(p -> new ReadOnlyStringWrapper(p.getValue().getValue().getInvQty()));
        invUomColumn.setCellValueFactory(p -> new ReadOnlyStringWrapper(p.getValue().getValue().getInvUom()));
        unitCostColumn.setCellValueFactory(p -> new ReadOnlyStringWrapper(p.getValue().getValue().getUnitCost()));
        totalCostColumn.setCellValueFactory(p -> new ReadOnlyStringWrapper(p.getValue().getValue().getTotalCost()));
        costPerProductionColumn.setCellValueFactory(p -> new ReadOnlyStringWrapper(p.getValue().getValue().getCostPerProduction()));

        loadData();
    }

    @FXML
    private void handleSearch() {
        loadData();
    }

    @FXML
    private void handleReset() {
        searchField.clear();
        salesFilter.setValue("ALL");
        purchasedFilter.setValue("ALL");
        stockedFilter.setValue("ALL");
        activeFilter.setValue("ALL");
        loadData();
    }

    @FXML
    private void handleRefresh() {
        loadData();
    }

    @FXML
    private void handleExport() {
        if (currentLoadTask != null && currentLoadTask.isRunning()) {
            showAlert("Loading in Progress", "Please wait for data to finish loading.");
            return;
        }
        loadingOverlay.setVisible(true);
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim();
        String sales = salesFilter.getValue() == null ? "ALL" : salesFilter.getValue();
        String purchased = purchasedFilter.getValue() == null ? "ALL" : purchasedFilter.getValue();
        String stocked = stockedFilter.getValue() == null ? "ALL" : stockedFilter.getValue();
        String active = activeFilter.getValue() == null ? "ALL" : activeFilter.getValue();

        Task<List<RecipeRow>> exportTask = new Task<>() {
            @Override
            protected List<RecipeRow> call() {
                return service.getAll(keyword, sales, purchased, stocked, active);
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

    private void loadData() {
        if (currentLoadTask != null && currentLoadTask.isRunning()) {
            currentLoadTask.cancel();
        }

        loadingOverlay.setVisible(true);
        resultLabel.setText("Loading...");
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim();
        String sales = salesFilter.getValue() == null ? "ALL" : salesFilter.getValue();
        String purchased = purchasedFilter.getValue() == null ? "ALL" : purchasedFilter.getValue();
        String stocked = stockedFilter.getValue() == null ? "ALL" : stockedFilter.getValue();
        String active = activeFilter.getValue() == null ? "ALL" : activeFilter.getValue();

        currentLoadTask = new Task<>() {
            @Override
            protected List<RecipeRow> call() {
                return service.getAll(keyword, sales, purchased, stocked, active);
            }
            @Override
            protected void succeeded() {
                loadingOverlay.setVisible(false);
                updateUI(getValue());
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

    private void updateUI(List<RecipeRow> rows) {
        Map<Long, List<RecipeRow>> byRecipe = rows.stream()
                .collect(Collectors.groupingBy(RecipeRow::getRecipeId));

        TreeItem<RecipeTreeRow> root = new TreeItem<>(new RecipeTreeRow("ROOT", "", "", "", "", "", "", "", "", ""));
        root.setExpanded(true);

        for (Map.Entry<Long, List<RecipeRow>> entry : byRecipe.entrySet()) {
            List<RecipeRow> recipeRows = entry.getValue();
            if (recipeRows.isEmpty()) continue;

            RecipeRow first = recipeRows.get(0);
            String recipeName = first.getRecipeName();
            String recipeActive = first.isRecipeActive() ? "Yes" : "No";
            String sales = first.getSales();
            String purchased = first.getPurchased();
            String stocked = first.getStocked();
            String production = decimalFormat.format(first.getProduction());
            String uom = first.getUom();

            double recipeTotalCost = recipeRows.stream().mapToDouble(RecipeRow::getTotalCost).sum();
            double costPerUnit = first.getProduction() > 0 ? recipeTotalCost / first.getProduction() : 0;
            String formattedTotalCost = decimalFormat.format(recipeTotalCost);
            String formattedCostPerUnit = decimalFormat.format(costPerUnit);

            TreeItem<RecipeTreeRow> recipeNode = new TreeItem<>(new RecipeTreeRow(
                    "RECIPE", recipeName, recipeActive, sales, purchased, stocked,
                    production, uom, formattedTotalCost, formattedCostPerUnit));
            recipeNode.setExpanded(true);

            for (RecipeRow row : recipeRows) {
                TreeItem<RecipeTreeRow> ingredientNode = new TreeItem<>(new RecipeTreeRow(
                        "INGREDIENT",
                        row.getItemName(),
                        decimalFormat.format(row.getRecQty()),
                        row.getRecipeUom(),
                        decimalFormat.format(row.getInvQty()),
                        row.getInvUom(),
                        decimalFormat.format(row.getUnitCost()),
                        decimalFormat.format(row.getTotalCost())
                ));
                recipeNode.getChildren().add(ingredientNode);
            }
            root.getChildren().add(recipeNode);
        }

        treeTableView.setRoot(root);
        treeTableView.setShowRoot(false);
        resultLabel.setText(byRecipe.size() + " recipe(s)");
    }

    // ------------------------------------------------------------------------
    // Export utilities (unchanged)
    // ------------------------------------------------------------------------
    private void exportToCsv(List<RecipeRow> rows) {
        if (rows.isEmpty()) {
            showAlert("No Data", "No recipes to export.");
            return;
        }

        Map<Long, List<RecipeRow>> byRecipe = rows.stream()
                .collect(Collectors.groupingBy(RecipeRow::getRecipeId));

        try {
            String downloadsPath = System.getProperty("user.home") + File.separator + "Downloads";
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path filePath = Paths.get(downloadsPath, "recipe_report_" + timestamp + ".csv");

            try (PrintWriter writer = new PrintWriter(filePath.toFile(), "UTF-8")) {
                writer.write('\uFEFF');
                writer.println("Recipe Name,Active,Sales,Purchased,Stocked,Production Qty,UOM,Ingredient,Recipe Qty,Recipe UOM,Inventory Qty,Inv UOM,Unit Cost,Ingredient Cost,Recipe Total Cost,Cost per Production Unit");

                for (Map.Entry<Long, List<RecipeRow>> entry : byRecipe.entrySet()) {
                    List<RecipeRow> recipeRows = entry.getValue();
                    RecipeRow first = recipeRows.get(0);
                    double recipeTotal = recipeRows.stream().mapToDouble(RecipeRow::getTotalCost).sum();
                    double costPerUnit = first.getProduction() > 0 ? recipeTotal / first.getProduction() : 0;

                    for (RecipeRow row : recipeRows) {
                        writer.println(String.join(",",
                                csvSafe(first.getRecipeName()),
                                csvSafe(first.isRecipeActive() ? "Yes" : "No"),
                                csvSafe(first.getSales()),
                                csvSafe(first.getPurchased()),
                                csvSafe(first.getStocked()),
                                formatNumberCSV(first.getProduction()),
                                csvSafe(first.getUom()),
                                csvSafe(row.getItemName()),
                                formatNumberCSV(row.getRecQty()),
                                csvSafe(row.getRecipeUom()),
                                formatNumberCSV(row.getInvQty()),
                                csvSafe(row.getInvUom()),
                                formatNumberCSV(row.getUnitCost()),
                                formatNumberCSV(row.getTotalCost()),
                                formatNumberCSV(recipeTotal),
                                formatNumberCSV(costPerUnit)
                        ));
                    }
                }
            }
            showExportSuccessDialog(filePath.toFile());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Export Error", e.getMessage());
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

    private void showExportSuccessDialog(File file) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Export Successful");
        alert.setHeaderText("Recipe Report Exported");
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