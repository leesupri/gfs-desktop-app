package com.gfs.app.ui;

import com.gfs.app.model.ProductionSummaryRow;
import com.gfs.app.repository.ProductionRepository.ProductionHeaderRow;
import com.gfs.app.service.ProductionService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

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

public class ProductionSummaryController {

    // ── Filters ──────────────────────────────────────────────────
    @FXML private DatePicker       startDatePicker;
    @FXML private DatePicker       endDatePicker;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private ComboBox<String> warehouseFilter;
    @FXML private TextField        itemField;

    // ── Summary table (grouped by category) ──────────────────────
    @FXML private TreeTableView<ProductionSummaryRow>            treeTable;
    @FXML private TreeTableColumn<ProductionSummaryRow, String>  colCategory;
    @FXML private TreeTableColumn<ProductionSummaryRow, String>  colItemName;
    @FXML private TreeTableColumn<ProductionSummaryRow, String>  colItemCode;
    @FXML private TreeTableColumn<ProductionSummaryRow, String>  colQuantity;
    @FXML private TreeTableColumn<ProductionSummaryRow, String>  colUom;
    @FXML private TreeTableColumn<ProductionSummaryRow, String>  colWarehouse;

    // ── Production ID list (right panel) ─────────────────────────
    @FXML private ListView<ProductionHeaderRow> productionList;
    @FXML private Label                         productionListLabel;

    // ── Status ───────────────────────────────────────────────────
    @FXML private Label     resultLabel;
    @FXML private StackPane loadingOverlay;
    @FXML private ProgressIndicator progressIndicator;

    private final ProductionService      service = new ProductionService();
    private final DecimalFormat          df      = createDecimalFormat();
    private Task<SummaryAndHeaders>      currentLoadTask;

    // -------------------------------------------------------------------------
    // Init — all DB work deferred to background
    // -------------------------------------------------------------------------
    @FXML
    public void initialize() {
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now());

        setComboPlaceholder(categoryFilter,  "Loading…");
        setComboPlaceholder(warehouseFilter, "Loading…");

        itemField.setOnAction(e -> loadData());

        setupSummaryColumns();
        setupProductionList();

        loadDropdownsThenData();
    }

    // -------------------------------------------------------------------------
    // Phase 1 — dropdowns + data in background
    // -------------------------------------------------------------------------
    private void loadDropdownsThenData() {
        loadingOverlay.setVisible(true);
        resultLabel.setText("Loading…");

        Task<DropdownData> task = new Task<>() {
            @Override protected DropdownData call() {
                return new DropdownData(service.getCategories(), service.getWarehouses());
            }
            @Override protected void succeeded() {
                DropdownData d = getValue();
                populateCombo(categoryFilter,  d.categories());
                populateCombo(warehouseFilter, d.warehouses());
                loadData();
            }
            @Override protected void failed() {
                populateCombo(categoryFilter,  List.of());
                populateCombo(warehouseFilter, List.of());
                loadingOverlay.setVisible(false);
                resultLabel.setText("Ready — dropdown load failed.");
            }
        };
        new Thread(task).start();
    }

    // -------------------------------------------------------------------------
    // Phase 2 — summary table + production ID list, both in one background task
    // -------------------------------------------------------------------------
    private void loadData() {
        if (currentLoadTask != null && currentLoadTask.isRunning()) currentLoadTask.cancel();

        LocalDate start = startDatePicker.getValue();
        LocalDate end   = endDatePicker.getValue();
        if (start == null) start = LocalDate.now();
        if (end   == null) end   = LocalDate.now();
        if (start.isAfter(end)) { LocalDate t = start; start = end; end = t; }

        final LocalDate fs = start, fe = end;
        final String cat  = combo(categoryFilter);
        final String wh   = combo(warehouseFilter);
        final String item = text(itemField);

        loadingOverlay.setVisible(true);
        resultLabel.setText("Loading…");
        productionListLabel.setText("Production Records");

        currentLoadTask = new Task<>() {
            @Override protected SummaryAndHeaders call() {
                List<ProductionSummaryRow>  summary = service.getSummary(fs, fe, cat, wh, item);
                List<ProductionHeaderRow>   headers = service.getHeaders(fs, fe, wh);
                return new SummaryAndHeaders(summary, headers);
            }
            @Override protected void succeeded() {
                loadingOverlay.setVisible(false);
                SummaryAndHeaders result = getValue();
                updateSummaryTree(result.summary());
                updateProductionList(result.headers());
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
    // Summary TreeTable — grouped by category (mirrors JRXML group band)
    // -------------------------------------------------------------------------
    private void setupSummaryColumns() {
        colCategory .setCellValueFactory(p -> sp(p.getValue().getValue().getCategory()));
        colItemName .setCellValueFactory(p -> sp(p.getValue().getValue().getItemName()));
        colItemCode .setCellValueFactory(p -> sp(p.getValue().getValue().getItemCode()));
        colQuantity .setCellValueFactory(p -> sp(df.format(p.getValue().getValue().getQuantity())));
        colUom      .setCellValueFactory(p -> sp(p.getValue().getValue().getUom()));
        colWarehouse.setCellValueFactory(p -> sp(p.getValue().getValue().getWarehouse()));

        colQuantity.setStyle("-fx-alignment: CENTER-RIGHT;");

        // Category rows: bold + warm background; item rows: white
        treeTable.setRowFactory(tv -> new TreeTableRow<>() {
            @Override protected void updateItem(ProductionSummaryRow item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setStyle(""); return; }
                // Category header rows have no itemName (we use a sentinel)
                boolean isCategoryHeader = item.getItemName().isBlank();
                setStyle(isCategoryHeader
                    ? "-fx-background-color:#faf7f4; -fx-font-weight:bold;"
                    : "-fx-background-color:white;");
            }
        });
    }

    private void updateSummaryTree(List<ProductionSummaryRow> rows) {
        // Group by category — mirrors JRXML category group band
        TreeItem<ProductionSummaryRow> root = new TreeItem<>(
            new ProductionSummaryRow("", "", "", 0, "", ""));
        root.setExpanded(true);

        String currentCategory = null;
        TreeItem<ProductionSummaryRow> categoryNode = null;

        for (ProductionSummaryRow row : rows) {
            if (!row.getCategory().equals(currentCategory)) {
                currentCategory = row.getCategory();
                // Category header — use sentinel row (itemName blank)
                categoryNode = new TreeItem<>(
                    new ProductionSummaryRow(currentCategory, "", "", 0, "", ""));
                categoryNode.setExpanded(true);
                root.getChildren().add(categoryNode);
            }
            if (categoryNode != null) {
                categoryNode.getChildren().add(new TreeItem<>(row));
            }
        }

        treeTable.setRoot(root);
        treeTable.setShowRoot(false);
        resultLabel.setText(rows.size() + " item(s) in " +
            root.getChildren().size() + " category(s)");
    }

    // -------------------------------------------------------------------------
    // Production ID list — right panel, click to open detail modal
    // -------------------------------------------------------------------------
    private void setupProductionList() {
        productionList.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(ProductionHeaderRow item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); return; }

                Label idLabel   = new Label("#" + item.id());
                idLabel.setStyle("-fx-font-weight:bold; -fx-text-fill:#3a2316; -fx-font-size:13px;");
                Label dateLabel = new Label(item.dateFormatted());
                dateLabel.setStyle("-fx-font-size:11px; -fx-text-fill:#765f52;");
                Label whLabel   = new Label(item.warehouse());
                whLabel.setStyle("-fx-font-size:11px; -fx-text-fill:#4a3728;");

                javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(2,
                    idLabel,
                    new javafx.scene.layout.HBox(8, dateLabel, whLabel));
                setGraphic(box);
                setText(null);
                setStyle("-fx-cursor:hand; -fx-padding:8 12;");
            }
        });

        // Single-click opens detail modal
        productionList.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, selected) -> {
                if (selected != null) openDetail(selected.id(), selected.dateFormatted(),
                    selected.warehouse());
            });
    }

    private void updateProductionList(List<ProductionHeaderRow> headers) {
        productionList.setItems(FXCollections.observableArrayList(headers));
        productionListLabel.setText("Production Records (" + headers.size() + ")");
    }

    // -------------------------------------------------------------------------
    // Detail modal
    // -------------------------------------------------------------------------
    private void openDetail(long productionId, String date, String warehouse) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/production-detail.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Production Detail — #" + productionId +
                           "  |  " + date + "  |  " + warehouse);
            stage.setMinWidth(860);
            stage.setMinHeight(600);

            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(
                getClass().getResource("/css/app.css").toExternalForm());

            ProductionDetailController ctrl = loader.getController();
            ctrl.loadProduction(productionId);

            stage.setScene(scene);
            stage.show();

            // Clear list selection so the same row can be clicked again
            productionList.getSelectionModel().clearSelection();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not open production detail: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Actions
    // -------------------------------------------------------------------------
    @FXML private void handleSearch() { loadData(); }

    @FXML
    private void handleRefresh() {
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now());
        categoryFilter.setValue("ALL");
        warehouseFilter.setValue("ALL");
        itemField.clear();
        loadData();
    }

    @FXML
    private void handleExport() {
        if (currentLoadTask != null && currentLoadTask.isRunning()) {
            showAlert("Loading", "Please wait for data to finish loading.");
            return;
        }
        final LocalDate start = startDatePicker.getValue();
        final LocalDate end   = endDatePicker.getValue();
        final String cat  = combo(categoryFilter);
        final String wh   = combo(warehouseFilter);
        final String item = text(itemField);

        loadingOverlay.setVisible(true);
        Task<List<ProductionSummaryRow>> task = new Task<>() {
            @Override protected List<ProductionSummaryRow> call() {
                return service.getSummary(start, end, cat, wh, item);
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
        new Thread(task).start();
    }

    private void exportToCsv(List<ProductionSummaryRow> rows) {
        if (rows.isEmpty()) { showAlert("No Data", "Nothing to export."); return; }
        try {
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path fp = Paths.get(System.getProperty("user.home"), "Downloads",
                "production_summary_" + ts + ".csv");
            try (PrintWriter w = new PrintWriter(fp.toFile(), "UTF-8")) {
                w.write('\uFEFF');
                w.println("Category,Item Name,Item Code,Quantity,UOM,Warehouse");
                for (ProductionSummaryRow r : rows) {
                    w.println(String.join(",",
                        csv(r.getCategory()), csv(r.getItemName()), csv(r.getItemCode()),
                        num(r.getQuantity()), csv(r.getUom()), csv(r.getWarehouse())));
                }
            }
            showExportSuccess(fp.toFile());
        } catch (Exception e) { e.printStackTrace(); showAlert("Export Error", e.getMessage()); }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------
    private record DropdownData(List<String> categories, List<String> warehouses) {}
    private record SummaryAndHeaders(List<ProductionSummaryRow> summary,
                                     List<ProductionHeaderRow>  headers) {}

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
    private static String text(TextField f) {
        return (f == null || f.getText() == null) ? "" : f.getText().trim();
    }
    private static String combo(ComboBox<String> c) {
        if (c == null) return "";
        String v = c.getValue();
        return (v == null || v.equals("ALL")) ? "" : v;
    }
    private static SimpleStringProperty sp(String v) {
        return new SimpleStringProperty(v != null ? v : "");
    }
    private static void setComboPlaceholder(ComboBox<String> c, String label) {
        c.setItems(FXCollections.observableArrayList(label));
        c.setValue(label); c.setDisable(true);
    }
    private static void populateCombo(ComboBox<String> c, List<String> values) {
        values.add(0, "ALL");
        c.setItems(FXCollections.observableArrayList(values));
        c.setValue("ALL"); c.setDisable(false);
    }
    private void showExportSuccess(File file) {
        Alert a = new Alert(Alert.AlertType.INFORMATION,
            "Saved to Downloads:\n" + file.getName());
        a.setTitle("Export Successful"); a.setHeaderText("Production Summary Exported");
        ButtonType open = new ButtonType("Open File"),
                   close = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        a.getButtonTypes().setAll(open, close);
        a.showAndWait().ifPresent(b -> {
            if (b == open) try { Desktop.getDesktop().open(file); }
                           catch (Exception e) { e.printStackTrace(); }
        });
    }
    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg);
        a.setTitle(title); a.setHeaderText(null); a.showAndWait();
    }
}