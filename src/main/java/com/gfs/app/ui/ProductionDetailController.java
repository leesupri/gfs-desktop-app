package com.gfs.app.ui;

import com.gfs.app.model.ProductionDetailRow;
import com.gfs.app.service.ProductionService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class ProductionDetailController {

    // ── Header labels ─────────────────────────────────────────────
    @FXML private Label labelProductionId;
    @FXML private Label labelDate;
    @FXML private Label labelWarehouse;
    @FXML private Label labelNotes;
    @FXML private Label resultLabel;

    // ── Recipe ingredient TreeTable ───────────────────────────────
    // Two groups: outer = ProductName, inner = recipe lines
    @FXML private TreeTableView<ProductionDetailRow>            treeTable;
    @FXML private TreeTableColumn<ProductionDetailRow, String>  colItemName;
    @FXML private TreeTableColumn<ProductionDetailRow, String>  colCategory;
    @FXML private TreeTableColumn<ProductionDetailRow, String>  colQty;
    @FXML private TreeTableColumn<ProductionDetailRow, String>  colUom;
    @FXML private TreeTableColumn<ProductionDetailRow, String>  colDescription;

    @FXML private StackPane loadingOverlay;

    private final ProductionService service = new ProductionService();
    private final DecimalFormat     df      = createDecimalFormat();
    private long currentProductionId;

    @FXML
    public void initialize() {
        setupColumns();
    }

    /**
     * Called by ProductionSummaryController immediately after loading this FXML.
     */
    public void loadProduction(long productionId) {
        this.currentProductionId = productionId;
        labelProductionId.setText("Production #" + productionId);
        loadingOverlay.setVisible(true);
        resultLabel.setText("Loading…");

        Task<List<ProductionDetailRow>> task = new Task<>() {
            @Override protected List<ProductionDetailRow> call() {
                return service.getDetail(productionId);
            }
            @Override protected void succeeded() {
                loadingOverlay.setVisible(false);
                updateUI(getValue());
            }
            @Override protected void failed() {
                loadingOverlay.setVisible(false);
                getException().printStackTrace();
                resultLabel.setText("Error loading detail");
            }
        };
        new Thread(task).start();
    }

    // -------------------------------------------------------------------------
    // Column setup — product header row vs ingredient detail row
    // -------------------------------------------------------------------------
    private void setupColumns() {
        // Item name column — product header: bold product name; ingredient: recipe name
        colItemName.setCellValueFactory(p -> {
            ProductionDetailRow r = p.getValue().getValue();
            if (r == null) return sp("");
            // Sentinel: productName set, recipeName blank = product header row
            return r.getRecipeName().isBlank()
                ? sp(r.getProductName())
                : sp(r.getRecipeName());
        });
        colItemName.setCellFactory(col -> new TreeTableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                ProductionDetailRow row = getTreeTableRow().getItem();
                boolean isHeader = row != null && row.getRecipeName().isBlank();
                setText(item);
                setStyle(isHeader
                    ? "-fx-font-weight:bold; -fx-text-fill:#3a2316;"
                    : "-fx-text-fill:#4a3728;");
            }
        });

        colCategory.setCellValueFactory(p -> {
            ProductionDetailRow r = p.getValue().getValue();
            if (r == null) return sp("");
            return r.getRecipeName().isBlank()
                ? sp(r.getCategory())       // product category
                : sp(r.getRecipeCategory()); // ingredient category
        });

        colQty.setCellValueFactory(p -> {
            ProductionDetailRow r = p.getValue().getValue();
            if (r == null) return sp("");
            return r.getRecipeName().isBlank()
                ? sp(df.format(r.getProductQty()))
                : sp(df.format(r.getRecipeQty()));
        });

        colUom.setCellValueFactory(p -> {
            ProductionDetailRow r = p.getValue().getValue();
            if (r == null) return sp("");
            return r.getRecipeName().isBlank()
                ? sp(r.getProductUom())
                : sp(r.getRecipeUom());
        });

        colDescription.setCellValueFactory(p -> {
            ProductionDetailRow r = p.getValue().getValue();
            if (r == null) return sp("");
            return r.getRecipeName().isBlank()
                ? sp(r.getProductDescription())
                : sp(r.getRecipeDescription());
        });

        colQty.setStyle("-fx-alignment: CENTER-RIGHT;");

        // Product header rows: warm background; ingredient rows: white
        treeTable.setRowFactory(tv -> new TreeTableRow<>() {
            @Override protected void updateItem(ProductionDetailRow item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setStyle(""); return; }
                setStyle(item.getRecipeName().isBlank()
                    ? "-fx-background-color:#faf7f4;"
                    : "-fx-background-color:white;");
            }
        });
    }

    // -------------------------------------------------------------------------
    // Build tree — mirrors JRXML two-level group:
    //   Group 1 (outer) on id:          shows notes in footer
    //   Group 2 (inner) on ProductName: shows product header + qty
    //   Detail band:                    one recipe ingredient line
    // -------------------------------------------------------------------------
    private void updateUI(List<ProductionDetailRow> rows) {
        if (rows.isEmpty()) { resultLabel.setText("No data found."); return; }

        // Header info from first row (all rows share same production header)
        ProductionDetailRow h = rows.get(0);
        labelDate.setText(h.getDateFormatted());
        labelWarehouse.setText(h.getWarehouse());
        labelNotes.setText(h.getNotes().isBlank() ? "—" : h.getNotes());

        // Group rows by ProductName preserving order
        Map<String, List<ProductionDetailRow>> byProduct = rows.stream()
            .collect(Collectors.groupingBy(
                ProductionDetailRow::getProductName,
                LinkedHashMap::new,
                Collectors.toList()
            ));

        TreeItem<ProductionDetailRow> root = new TreeItem<>(
            new ProductionDetailRow(0, null, "", "", "", 0, "", "", "", "", 0, "", "", ""));
        root.setExpanded(true);

        int totalIngredients = 0;

        for (Map.Entry<String, List<ProductionDetailRow>> entry : byProduct.entrySet()) {
            List<ProductionDetailRow> productRows = entry.getValue();
            ProductionDetailRow       first       = productRows.get(0);

            // Product header node — sentinel: recipeName left blank
            ProductionDetailRow headerSentinel = new ProductionDetailRow(
                first.getId(), first.getDate(), first.getWarehouse(),
                first.getProductName(), first.getCategory(),
                first.getProductQty(), first.getProductUom(),
                first.getProductDescription(),
                "", "", 0, "", "",   // empty recipe fields → triggers header style
                first.getNotes()
            );

            TreeItem<ProductionDetailRow> productNode = new TreeItem<>(headerSentinel);
            productNode.setExpanded(true);

            for (ProductionDetailRow ingredientRow : productRows) {
                productNode.getChildren().add(new TreeItem<>(ingredientRow));
                totalIngredients++;
            }
            root.getChildren().add(productNode);
        }

        treeTable.setRoot(root);
        treeTable.setShowRoot(false);
        resultLabel.setText(byProduct.size() + " product(s) / " +
                            totalIngredients + " ingredient line(s)");
    }

    // -------------------------------------------------------------------------
    // Actions
    // -------------------------------------------------------------------------
    @FXML
    private void handleExport() {
        List<ProductionDetailRow> rows = collectAllRows();
        if (rows.isEmpty()) { showAlert("No Data", "Nothing to export."); return; }
        try {
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path fp = Paths.get(System.getProperty("user.home"), "Downloads",
                "production_detail_" + currentProductionId + "_" + ts + ".csv");
            try (PrintWriter w = new PrintWriter(fp.toFile(), "UTF-8")) {
                w.write('\uFEFF');
                w.println("Production ID,Date,Warehouse,Product Name,Category," +
                          "Product Qty,Product UOM,Recipe Name,Recipe Category," +
                          "Recipe Qty,Recipe UOM,Recipe Description,Notes");
                for (ProductionDetailRow r : rows) {
                    w.println(String.join(",",
                        csv(String.valueOf(r.getId())),
                        csv(r.getDateFormatted()),
                        csv(r.getWarehouse()),
                        csv(r.getProductName()),
                        csv(r.getCategory()),
                        num(r.getProductQty()),
                        csv(r.getProductUom()),
                        csv(r.getRecipeName()),
                        csv(r.getRecipeCategory()),
                        num(r.getRecipeQty()),
                        csv(r.getRecipeUom()),
                        csv(r.getRecipeDescription()),
                        csv(r.getNotes())
                    ));
                }
            }
            Alert a = new Alert(Alert.AlertType.INFORMATION,
                "Saved to Downloads:\n" + fp.getFileName());
            a.setTitle("Export Successful"); a.setHeaderText(null);
            ButtonType open = new ButtonType("Open File"),
                       close = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
            a.getButtonTypes().setAll(open, close);
            a.showAndWait().ifPresent(b -> {
                if (b == open) try { Desktop.getDesktop().open(fp.toFile()); }
                               catch (Exception e) { e.printStackTrace(); }
            });
        } catch (Exception e) { e.printStackTrace(); showAlert("Export Error", e.getMessage()); }
    }

    @FXML
    private void handleClose() {
        ((Stage) treeTable.getScene().getWindow()).close();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------
    /** Collect all ingredient rows (non-sentinel) from the tree. */
    private List<ProductionDetailRow> collectAllRows() {
        if (treeTable.getRoot() == null) return List.of();
        return treeTable.getRoot().getChildren().stream()
            .flatMap(productNode -> productNode.getChildren().stream())
            .map(ti -> ti.getValue())
            .collect(Collectors.toList());
    }

    private static DecimalFormat createDecimalFormat() {
        DecimalFormat d = new DecimalFormat("#,###.00");
        d.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.GERMANY));
        return d;
    }

    private String num(double v) { return "\"" + df.format(v) + "\""; }
    private static String csv(String v) {
        if (v == null) return "";
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }
    private static SimpleStringProperty sp(String v) {
        return new SimpleStringProperty(v != null ? v : "");
    }
    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg);
        a.setTitle(title); a.setHeaderText(null); a.showAndWait();
    }
}