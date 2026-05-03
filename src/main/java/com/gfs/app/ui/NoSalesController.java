package com.gfs.app.ui;

import com.gfs.app.model.NoSalesItemRow;
import com.gfs.app.model.NoSalesReceiptRow;
import com.gfs.app.service.NoSalesService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

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

public class NoSalesController {

    // ── Tabs ─────────────────────────────────────────────────────
    @FXML private TabPane tabPane;

    // ── Shared filters ────────────────────────────────────────────
    @FXML private DatePicker       startDatePicker;
    @FXML private DatePicker       endDatePicker;
    @FXML private ComboBox<String> departmentFilter;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private TextField        descriptionField;
    @FXML private TextField        tableField;
    @FXML private TextField        staffField;
    @FXML private Label            resultLabel;
    @FXML private Label            grandTotalLabel;

    // ── Tab 0: Item Summary ───────────────────────────────────────
    // Three-level tree: Department → Category → Description
    @FXML private TreeTableView<NoSalesItemRow>            itemTree;
    @FXML private TreeTableColumn<NoSalesItemRow, String>  iColDept;
    @FXML private TreeTableColumn<NoSalesItemRow, String>  iColCategory;
    @FXML private TreeTableColumn<NoSalesItemRow, String>  iColDescription;
    @FXML private TreeTableColumn<NoSalesItemRow, String>  iColQty;
    @FXML private TreeTableColumn<NoSalesItemRow, String>  iColNetSales;
    @FXML private TreeTableColumn<NoSalesItemRow, String>  iColDisc;
    @FXML private TreeTableColumn<NoSalesItemRow, String>  iColCost;
    @FXML private TreeTableColumn<NoSalesItemRow, String>  iColProfit;
    @FXML private TreeTableColumn<NoSalesItemRow, String>  iColProfitPct;
    @FXML private TreeTableColumn<NoSalesItemRow, String>  iColService;
    @FXML private TreeTableColumn<NoSalesItemRow, String>  iColTax;
    @FXML private TreeTableColumn<NoSalesItemRow, String>  iColTotal;

    // ── Tab 1: Receipt Detail ─────────────────────────────────────
    // Two-level tree: Sales ID → Line items
    @FXML private TreeTableView<NoSalesReceiptRow>            receiptTree;
    @FXML private TreeTableColumn<NoSalesReceiptRow, String>  rColId;
    @FXML private TreeTableColumn<NoSalesReceiptRow, String>  rColCreated;
    @FXML private TreeTableColumn<NoSalesReceiptRow, String>  rColClosed;
    @FXML private TreeTableColumn<NoSalesReceiptRow, String>  rColTable;
    @FXML private TreeTableColumn<NoSalesReceiptRow, String>  rColType;
    @FXML private TreeTableColumn<NoSalesReceiptRow, String>  rColGuest;
    @FXML private TreeTableColumn<NoSalesReceiptRow, String>  rColStaff;
    @FXML private TreeTableColumn<NoSalesReceiptRow, String>  rColDescription;
    @FXML private TreeTableColumn<NoSalesReceiptRow, String>  rColQty;
    @FXML private TreeTableColumn<NoSalesReceiptRow, String>  rColPrice;
    @FXML private TreeTableColumn<NoSalesReceiptRow, String>  rColTotal;

    @FXML private StackPane loadingOverlay;
    @FXML private ProgressIndicator progressIndicator;

    private final NoSalesService service = new NoSalesService();
    private final DecimalFormat   df     = createDecimalFormat();
    private Task<?>               currentTask;

    // -------------------------------------------------------------------------
    // Init
    // -------------------------------------------------------------------------
    @FXML
    public void initialize() {
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now());

        setComboPlaceholder(departmentFilter, "Loading…");
        setComboPlaceholder(categoryFilter,   "Loading…");

        descriptionField.setOnAction(e -> loadCurrentTab());
        tableField      .setOnAction(e -> loadCurrentTab());
        staffField      .setOnAction(e -> loadCurrentTab());

        setupItemColumns();
        setupReceiptColumns();

        tabPane.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, tab) -> loadCurrentTab());

        loadDropdownsThenData();
    }

    // -------------------------------------------------------------------------
    // Dropdowns + data
    // -------------------------------------------------------------------------
    private void loadDropdownsThenData() {
        loadingOverlay.setVisible(true);
        resultLabel.setText("Loading…");

        Task<DropdownData> task = new Task<>() {
            @Override protected DropdownData call() {
                return new DropdownData(service.getDepartments(), service.getCategories());
            }
            @Override protected void succeeded() {
                DropdownData d = getValue();
                populateCombo(departmentFilter, d.departments());
                populateCombo(categoryFilter,   d.categories());
                loadCurrentTab();
            }
            @Override protected void failed() {
                populateCombo(departmentFilter, List.of());
                populateCombo(categoryFilter,   List.of());
                loadingOverlay.setVisible(false);
                resultLabel.setText("Ready.");
            }
        };
        new Thread(task).start();
    }

    private void loadCurrentTab() {
        if (currentTask != null && currentTask.isRunning()) currentTask.cancel();
        int idx = tabPane.getSelectionModel().getSelectedIndex();
        if (idx == 0) loadItemSummary();
        else          loadReceiptDetail();
    }

    // -------------------------------------------------------------------------
    // Actions
    // -------------------------------------------------------------------------
    @FXML private void handleSearch()  { loadCurrentTab(); }

    @FXML
    private void handleRefresh() {
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now());
        departmentFilter.setValue("ALL");
        categoryFilter.setValue("ALL");
        descriptionField.clear();
        tableField.clear();
        staffField.clear();
        loadCurrentTab();
    }

    @FXML
    private void handleExport() {
        if (currentTask != null && currentTask.isRunning()) {
            showAlert("Loading", "Please wait for data to finish loading."); return;
        }
        int idx = tabPane.getSelectionModel().getSelectedIndex();
        if (idx == 0) exportItemSummary();
        else          exportReceiptDetail();
    }

    // =========================================================================
    // TAB 0 — Item Summary
    // Three-level tree: Department → Category → Item description
    // Mirrors JRXML group bands: department, Category, then detail
    // =========================================================================
    private void loadItemSummary() {
        final LocalDate fs = start(), fe = end();
        final String dept = combo(departmentFilter), cat = combo(categoryFilter);
        final String desc = text(descriptionField);

        beginLoad();
        currentTask = new Task<List<NoSalesItemRow>>() {
            @Override protected List<NoSalesItemRow> call() {
                return service.getItemSummary(fs, fe, dept, cat, desc);
            }
            @Override protected void succeeded() {
                endLoad();
                List<NoSalesItemRow> rows = getValue();
                buildItemTree(rows);
                updateItemTotals(rows);
            }
            @Override protected void failed()    { loadFailed(); }
            @Override protected void cancelled() { loadCancelled(); }
        };
        new Thread(currentTask).start();
    }

    private void buildItemTree(List<NoSalesItemRow> rows) {
        TreeItem<NoSalesItemRow> root = new TreeItem<>(sentinel("", "", ""));
        root.setExpanded(true);

        // Group: Department → Category → items
        Map<String, List<NoSalesItemRow>> byDept = rows.stream()
            .collect(Collectors.groupingBy(NoSalesItemRow::getDepartment,
                LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<String, List<NoSalesItemRow>> deptEntry : byDept.entrySet()) {
            String deptName = deptEntry.getKey();
            List<NoSalesItemRow> deptRows = deptEntry.getValue();

            TreeItem<NoSalesItemRow> deptNode = new TreeItem<>(sentinel(deptName, "", "D"));
            deptNode.setExpanded(true);

            Map<String, List<NoSalesItemRow>> byCat = deptRows.stream()
                .collect(Collectors.groupingBy(NoSalesItemRow::getCategory,
                    LinkedHashMap::new, Collectors.toList()));

            for (Map.Entry<String, List<NoSalesItemRow>> catEntry : byCat.entrySet()) {
                String catName = catEntry.getKey();
                List<NoSalesItemRow> catRows = catEntry.getValue();

                TreeItem<NoSalesItemRow> catNode = new TreeItem<>(sentinel(deptName, catName, "C"));
                catNode.setExpanded(true);

                for (NoSalesItemRow row : catRows) {
                    catNode.getChildren().add(new TreeItem<>(row));
                }
                deptNode.getChildren().add(catNode);
            }
            root.getChildren().add(deptNode);
        }

        itemTree.setRoot(root);
        itemTree.setShowRoot(false);
    }

    private void setupItemColumns() {
        // Department column — shows dept for dept node, category for cat node, blank for items
        iColDept.setCellValueFactory(p -> {
            NoSalesItemRow r = p.getValue().getValue();
            if (r == null) return sp("");
            if ("D".equals(r.getDescription())) return sp(r.getDepartment());
            if ("C".equals(r.getDescription())) return sp("");
            return sp("");
        });
        iColDept.setCellFactory(col -> boldCell(r -> "D".equals(r.getDescription())));

        iColCategory.setCellValueFactory(p -> {
            NoSalesItemRow r = p.getValue().getValue();
            if (r == null) return sp("");
            if ("C".equals(r.getDescription())) return sp(r.getCategory());
            if ("D".equals(r.getDescription())) return sp("");
            return sp("");
        });
        iColCategory.setCellFactory(col -> boldCell(r -> "C".equals(r.getDescription())));

        iColDescription.setCellValueFactory(p -> {
            NoSalesItemRow r = p.getValue().getValue();
            if (r == null || "D".equals(r.getDescription()) || "C".equals(r.getDescription())) return sp("");
            return sp(r.getDescription());
        });

        // Numeric columns — only show for leaf rows (not sentinel headers)
        iColQty      .setCellValueFactory(p -> numOrBlank(p, r -> df.format(r.getQuantity())));
        iColNetSales .setCellValueFactory(p -> numOrBlank(p, r -> df.format(r.getNetSales())));
        iColDisc     .setCellValueFactory(p -> numOrBlank(p, r -> df.format(r.getDisc())));
        iColCost     .setCellValueFactory(p -> numOrBlank(p, r -> df.format(r.getCost())));
        iColProfit   .setCellValueFactory(p -> numOrBlank(p, r -> df.format(r.getProfit())));
        iColProfitPct.setCellValueFactory(p -> numOrBlank(p,
            r -> String.format("%.1f%%", r.getProfitPercent() * 100)));
        iColService  .setCellValueFactory(p -> numOrBlank(p, r -> df.format(r.getService())));
        iColTax      .setCellValueFactory(p -> numOrBlank(p, r -> df.format(r.getTax())));
        iColTotal    .setCellValueFactory(p -> numOrBlank(p, r -> df.format(r.getTotal())));

        List.of(iColQty, iColNetSales, iColDisc, iColCost,
                iColProfit, iColProfitPct, iColService, iColTax, iColTotal)
            .forEach(c -> c.setStyle("-fx-alignment: CENTER-RIGHT;"));

        // Colour profit% column: green < 0 (negative cost), yellow mid, red high cost
        iColProfitPct.setCellFactory(col -> new TreeTableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) { setText(null); setStyle(""); return; }
                setText(item);
                try {
                    double pct = Double.parseDouble(item.replace("%", "").trim());
                    setStyle(pct >= 0
                        ? "-fx-text-fill: #166534; -fx-alignment: CENTER-RIGHT;"
                        : "-fx-text-fill: #991b1b; -fx-alignment: CENTER-RIGHT;");
                } catch (NumberFormatException e) { setStyle("-fx-alignment: CENTER-RIGHT;"); }
            }
        });

        itemTree.setRowFactory(tv -> new TreeTableRow<>() {
            @Override protected void updateItem(NoSalesItemRow r, boolean empty) {
                super.updateItem(r, empty);
                if (empty || r == null) { setStyle(""); return; }
                String d = r.getDescription();
                if ("D".equals(d))      setStyle("-fx-background-color:#3a2316; -fx-font-weight:bold;");
                else if ("C".equals(d)) setStyle("-fx-background-color:#faf7f4; -fx-font-weight:bold;");
                else                    setStyle("-fx-background-color:white;");
            }
        });
    }

    private void updateItemTotals(List<NoSalesItemRow> rows) {
        // Only leaf rows (exclude sentinels)
        List<NoSalesItemRow> leaves = rows.stream()
            .filter(r -> !"D".equals(r.getDescription()) && !"C".equals(r.getDescription()))
            .collect(Collectors.toList());
        resultLabel.setText(leaves.size() + " item(s)");
        double grand = leaves.stream().mapToDouble(NoSalesItemRow::getTotal).sum();
        grandTotalLabel.setText("Grand Total: " + df.format(grand));
    }

    // =========================================================================
    // TAB 1 — Receipt Detail
    // Two-level tree: Sales ID header → line items
    // Mirrors JRXML group on id
    // =========================================================================
    private void loadReceiptDetail() {
        final LocalDate fs = start(), fe = end();
        final String tbl = text(tableField), stf = text(staffField);

        beginLoad();
        currentTask = new Task<List<NoSalesReceiptRow>>() {
            @Override protected List<NoSalesReceiptRow> call() {
                return service.getReceiptDetail(fs, fe, tbl, stf);
            }
            @Override protected void succeeded() {
                endLoad();
                List<NoSalesReceiptRow> rows = getValue();
                buildReceiptTree(rows);
                updateReceiptTotals(rows);
            }
            @Override protected void failed()    { loadFailed(); }
            @Override protected void cancelled() { loadCancelled(); }
        };
        new Thread(currentTask).start();
    }

    private void buildReceiptTree(List<NoSalesReceiptRow> rows) {
        TreeItem<NoSalesReceiptRow> root = new TreeItem<>(receiptSentinel());
        root.setExpanded(true);

        Map<Long, List<NoSalesReceiptRow>> byId = rows.stream()
            .collect(Collectors.groupingBy(NoSalesReceiptRow::getId,
                LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<Long, List<NoSalesReceiptRow>> entry : byId.entrySet()) {
            List<NoSalesReceiptRow> saleRows = entry.getValue();
            NoSalesReceiptRow h = saleRows.get(0);

            // Header sentinel — id set, description blank signals group header
            NoSalesReceiptRow header = new NoSalesReceiptRow(
                h.getId(), h.getCreated(), h.getClosed(), h.getNotes(), h.getTableName(),
                h.getSubtotal(), h.getDiscount(), h.getServiceAmount(), h.getTaxAmount(),
                h.getTotal(), h.getType(), h.getFullName(), h.getGuest(), h.getClosedAt(),
                h.getMember(), "", 0, 0  // blank description = header sentinel
            );

            TreeItem<NoSalesReceiptRow> saleNode = new TreeItem<>(header);
            saleNode.setExpanded(false); // collapsed by default

            for (NoSalesReceiptRow line : saleRows) {
                saleNode.getChildren().add(new TreeItem<>(line));
            }
            root.getChildren().add(saleNode);
        }

        receiptTree.setRoot(root);
        receiptTree.setShowRoot(false);
    }

    private void setupReceiptColumns() {
        rColId.setCellValueFactory(p -> {
            NoSalesReceiptRow r = p.getValue().getValue();
            return sp(r != null && r.getDescription().isBlank() ? String.valueOf(r.getId()) : "");
        });
        rColId.setCellFactory(col -> boldCell(r -> r.getDescription().isBlank()));

        rColCreated    .setCellValueFactory(p -> sp(headerOnly(p, r -> r.getCreatedFormatted())));
        rColClosed     .setCellValueFactory(p -> sp(headerOnly(p, r -> r.getClosedFormatted())));
        rColTable      .setCellValueFactory(p -> sp(headerOnly(p, r -> r.getTableName())));
        rColType       .setCellValueFactory(p -> sp(headerOnly(p, r -> r.getType())));
        rColGuest      .setCellValueFactory(p -> sp(headerOnly(p, r -> String.valueOf(r.getGuest()))));
        rColStaff      .setCellValueFactory(p -> sp(headerOnly(p, r -> r.getFullName())));
        rColTotal      .setCellValueFactory(p -> sp(headerOnly(p, r -> df.format(r.getTotal()))));

        rColDescription.setCellValueFactory(p -> {
            NoSalesReceiptRow r = p.getValue().getValue();
            return sp(r != null && !r.getDescription().isBlank() ? r.getDescription() : "");
        });
        rColQty.setCellValueFactory(p -> {
            NoSalesReceiptRow r = p.getValue().getValue();
            return sp(r != null && !r.getDescription().isBlank() ? String.format("%.0f", r.getQuantity()) : "");
        });
        rColPrice.setCellValueFactory(p -> {
            NoSalesReceiptRow r = p.getValue().getValue();
            return sp(r != null && !r.getDescription().isBlank() ? df.format(r.getPrice()) : "");
        });

        List.of(rColQty, rColPrice, rColTotal)
            .forEach(c -> c.setStyle("-fx-alignment: CENTER-RIGHT;"));

        receiptTree.setRowFactory(tv -> new TreeTableRow<>() {
            @Override protected void updateItem(NoSalesReceiptRow r, boolean empty) {
                super.updateItem(r, empty);
                if (empty || r == null) { setStyle(""); return; }
                setStyle(r.getDescription().isBlank()
                    ? "-fx-background-color:#faf7f4; -fx-font-weight:bold;"
                    : "-fx-background-color:white;");
            }
        });
    }

    private void updateReceiptTotals(List<NoSalesReceiptRow> rows) {
        long billCount = rows.stream().map(NoSalesReceiptRow::getId).distinct().count();
        resultLabel.setText(billCount + " sale(s) / " + rows.size() + " line(s)");
        double grand = rows.stream().map(NoSalesReceiptRow::getId).distinct()
            .mapToDouble(id -> rows.stream().filter(r -> r.getId() == id)
                .findFirst().map(NoSalesReceiptRow::getTotal).orElse(0.0))
            .sum();
        grandTotalLabel.setText("Grand Total: " + df.format(grand));
    }

    // =========================================================================
    // Export
    // =========================================================================
    private void exportItemSummary() {
        final LocalDate fs = start(), fe = end();
        final String dept = combo(departmentFilter), cat = combo(categoryFilter);
        final String desc = text(descriptionField);
        loadingOverlay.setVisible(true);
        Task<List<NoSalesItemRow>> task = new Task<>() {
            @Override protected List<NoSalesItemRow> call() {
                return service.getItemSummary(fs, fe, dept, cat, desc);
            }
            @Override protected void succeeded() {
                loadingOverlay.setVisible(false);
                List<NoSalesItemRow> rows = getValue();
                if (rows.isEmpty()) { showAlert("No Data", "Nothing to export."); return; }
                try (PrintWriter w = csvWriter("no_sales_item_summary")) {
                    w.println("Department,Category,Description,Quantity,Net Sales,Discount," +
                              "Cost,Profit,Profit%,Service,Tax,Total");
                    for (NoSalesItemRow r : rows) {
                        w.println(String.join(",",
                            csv(r.getDepartment()), csv(r.getCategory()), csv(r.getDescription()),
                            num(r.getQuantity()), num(r.getNetSales()), num(r.getDisc()),
                            num(r.getCost()), num(r.getProfit()),
                            "\"" + String.format("%.1f%%", r.getProfitPercent() * 100) + "\"",
                            num(r.getService()), num(r.getTax()), num(r.getTotal())));
                    }
                } catch (Exception e) { e.printStackTrace(); showAlert("Export Error", e.getMessage()); }
            }
            @Override protected void failed() {
                loadingOverlay.setVisible(false); showAlert("Export Error", getException().getMessage());
            }
        };
        new Thread(task).start();
    }

    private void exportReceiptDetail() {
        final LocalDate fs = start(), fe = end();
        final String tbl = text(tableField), stf = text(staffField);
        loadingOverlay.setVisible(true);
        Task<List<NoSalesReceiptRow>> task = new Task<>() {
            @Override protected List<NoSalesReceiptRow> call() {
                return service.getReceiptDetail(fs, fe, tbl, stf);
            }
            @Override protected void succeeded() {
                loadingOverlay.setVisible(false);
                List<NoSalesReceiptRow> rows = getValue();
                if (rows.isEmpty()) { showAlert("No Data", "Nothing to export."); return; }
                try (PrintWriter w = csvWriter("no_sales_receipt_detail")) {
                    w.println("Sale ID,Opened,Closed,Table,Type,Guests,Staff,Member," +
                              "Description,Qty,Price,Total");
                    for (NoSalesReceiptRow r : rows) {
                        w.println(String.join(",",
                            csv(String.valueOf(r.getId())),
                            csv(r.getCreatedFormatted()), csv(r.getClosedFormatted()),
                            csv(r.getTableName()), csv(r.getType()),
                            csv(String.valueOf(r.getGuest())), csv(r.getFullName()), csv(r.getMember()),
                            csv(r.getDescription()),
                            csv(String.format("%.0f", r.getQuantity())),
                            num(r.getPrice()), num(r.getTotal())));
                    }
                } catch (Exception e) { e.printStackTrace(); showAlert("Export Error", e.getMessage()); }
            }
            @Override protected void failed() {
                loadingOverlay.setVisible(false); showAlert("Export Error", getException().getMessage());
            }
        };
        new Thread(task).start();
    }

    // =========================================================================
    // Helpers
    // =========================================================================
    private record DropdownData(List<String> departments, List<String> categories) {}

    /** Sentinel item row — description field encodes node type: "D"=dept, "C"=cat */
    private static NoSalesItemRow sentinel(String dept, String cat, String typeCode) {
        return new NoSalesItemRow(dept, cat, typeCode, 0, 0, 0, 0, 0, 0, 0);
    }

    private static NoSalesReceiptRow receiptSentinel() {
        return new NoSalesReceiptRow(0, null, null, "", "", 0, 0, 0, 0, 0, "", "", 0, "", "", "", 0, 0);
    }

    private SimpleStringProperty numOrBlank(
            TreeTableColumn.CellDataFeatures<NoSalesItemRow, String> p,
            java.util.function.Function<NoSalesItemRow, String> extractor) {
        NoSalesItemRow r = p.getValue().getValue();
        if (r == null) return sp("");
        String d = r.getDescription();
        if ("D".equals(d) || "C".equals(d)) return sp("");
        return sp(extractor.apply(r));
    }

    private String headerOnly(
            TreeTableColumn.CellDataFeatures<NoSalesReceiptRow, String> p,
            java.util.function.Function<NoSalesReceiptRow, String> extractor) {
        NoSalesReceiptRow r = p.getValue().getValue();
        if (r == null || !r.getDescription().isBlank()) return "";
        return extractor.apply(r);
    }

    private <T> TreeTableCell<T, String> boldCell(java.util.function.Predicate<T> isHeader) {
        return new TreeTableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                T row = getTreeTableRow().getItem();
                setStyle(row != null && isHeader.test(row)
                    ? "-fx-font-weight:bold; -fx-text-fill:#fffef3;"
                    : "-fx-text-fill:#4a3728;");
            }
        };
    }

    private void beginLoad() {
        loadingOverlay.setVisible(true);
        resultLabel.setText("Loading…");
        grandTotalLabel.setText("Grand Total: --");
    }
    private void endLoad()       { loadingOverlay.setVisible(false); }
    private void loadFailed()    { endLoad(); resultLabel.setText("Error"); }
    private void loadCancelled() { endLoad(); resultLabel.setText("Cancelled"); }

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
    private PrintWriter csvWriter(String prefix) throws Exception {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path fp = Paths.get(System.getProperty("user.home"), "Downloads", prefix + "_" + ts + ".csv");
        PrintWriter w = new PrintWriter(fp.toFile(), "UTF-8");
        w.write('\uFEFF');
        return w;
    }
    private LocalDate start() { LocalDate d = startDatePicker.getValue(); return d != null ? d : LocalDate.now(); }
    private LocalDate end()   { LocalDate d = endDatePicker.getValue();   return d != null ? d : LocalDate.now(); }
    private static String text(TextField f) { return (f == null || f.getText() == null) ? "" : f.getText().trim(); }
    private static String combo(ComboBox<String> c) {
        if (c == null) return ""; String v = c.getValue();
        return (v == null || v.equals("ALL")) ? "" : v;
    }
    private static SimpleStringProperty sp(String v) { return new SimpleStringProperty(v != null ? v : ""); }
    private static void setComboPlaceholder(ComboBox<String> c, String label) {
        c.setItems(FXCollections.observableArrayList(label)); c.setValue(label); c.setDisable(true);
    }
    private static void populateCombo(ComboBox<String> c, List<String> values) {
        values.add(0, "ALL"); c.setItems(FXCollections.observableArrayList(values));
        c.setValue("ALL"); c.setDisable(false);
    }
    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg); a.setTitle(title); a.setHeaderText(null); a.showAndWait();
    }
}