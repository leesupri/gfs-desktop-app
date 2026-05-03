package com.gfs.app.ui;

import com.gfs.app.model.PurchaseDetailRow;
import com.gfs.app.model.PurchaseOrderSummaryRow;
import com.gfs.app.model.PurchaseSummaryRow;
import com.gfs.app.service.PurchaseService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
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

public class PurchaseReportController {

    // ── Tab pane ──────────────────────────────────────────────────
    @FXML private TabPane tabPane;

    // ── Shared filters ────────────────────────────────────────────
    @FXML private DatePicker       startDatePicker;
    @FXML private DatePicker       endDatePicker;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private ComboBox<String> partnerFilter;
    @FXML private ComboBox<String> warehouseFilter;
    @FXML private TextField        itemField;
    @FXML private Label            resultLabel;
    @FXML private Label            grandTotalLabel;

    // ── Purchase Summary tab ──────────────────────────────────────
    @FXML private TreeTableView<PurchaseSummaryRow>            summaryTree;
    @FXML private TreeTableColumn<PurchaseSummaryRow, String>  sumColCategory;
    @FXML private TreeTableColumn<PurchaseSummaryRow, String>  sumColName;
    @FXML private TreeTableColumn<PurchaseSummaryRow, String>  sumColCode;
    @FXML private TreeTableColumn<PurchaseSummaryRow, String>  sumColQty;
    @FXML private TreeTableColumn<PurchaseSummaryRow, String>  sumColUom;
    @FXML private TreeTableColumn<PurchaseSummaryRow, String>  sumColTotalCost;

    // ── Purchase Detail (by category) tab ────────────────────────
    @FXML private TreeTableView<PurchaseDetailRow>             detailCatTree;
    @FXML private TreeTableColumn<PurchaseDetailRow, String>   dcColGroup;
    @FXML private TreeTableColumn<PurchaseDetailRow, String>   dcColDate;
    @FXML private TreeTableColumn<PurchaseDetailRow, String>   dcColInvoice;
    @FXML private TreeTableColumn<PurchaseDetailRow, String>   dcColPurchaseQty;
    @FXML private TreeTableColumn<PurchaseDetailRow, String>   dcColPurchaseUom;
    @FXML private TreeTableColumn<PurchaseDetailRow, String>   dcColQty;
    @FXML private TreeTableColumn<PurchaseDetailRow, String>   dcColUom;
    @FXML private TreeTableColumn<PurchaseDetailRow, String>   dcColUnitCost;
    @FXML private TreeTableColumn<PurchaseDetailRow, String>   dcColTotal;
    @FXML private TreeTableColumn<PurchaseDetailRow, String>   dcColPartner;
    @FXML private TreeTableColumn<PurchaseDetailRow, String>   dcColWarehouse;

    // ── Purchase Detail (by partner) tab ─────────────────────────
    @FXML private TreeTableView<PurchaseDetailRow>             detailParTree;
    @FXML private TreeTableColumn<PurchaseDetailRow, String>   dpColGroup;
    @FXML private TreeTableColumn<PurchaseDetailRow, String>   dpColDate;
    @FXML private TreeTableColumn<PurchaseDetailRow, String>   dpColInvoice;
    @FXML private TreeTableColumn<PurchaseDetailRow, String>   dpColCategory;
    @FXML private TreeTableColumn<PurchaseDetailRow, String>   dpColItem;
    @FXML private TreeTableColumn<PurchaseDetailRow, String>   dpColPurchaseQty;
    @FXML private TreeTableColumn<PurchaseDetailRow, String>   dpColPurchaseUom;
    @FXML private TreeTableColumn<PurchaseDetailRow, String>   dpColQty;
    @FXML private TreeTableColumn<PurchaseDetailRow, String>   dpColUom;
    @FXML private TreeTableColumn<PurchaseDetailRow, String>   dpColUnitCost;
    @FXML private TreeTableColumn<PurchaseDetailRow, String>   dpColTotal;
    @FXML private TreeTableColumn<PurchaseDetailRow, String>   dpColWarehouse;

    // ── Purchase Order Summary tab ────────────────────────────────
    @FXML private TreeTableView<PurchaseOrderSummaryRow>            orderTree;
    @FXML private TreeTableColumn<PurchaseOrderSummaryRow, String>  ordColCategory;
    @FXML private TreeTableColumn<PurchaseOrderSummaryRow, String>  ordColItemName;
    @FXML private TreeTableColumn<PurchaseOrderSummaryRow, String>  ordColItemCode;
    @FXML private TreeTableColumn<PurchaseOrderSummaryRow, String>  ordColQty;
    @FXML private TreeTableColumn<PurchaseOrderSummaryRow, String>  ordColReceived;
    @FXML private TreeTableColumn<PurchaseOrderSummaryRow, String>  ordColUom;
    @FXML private TreeTableColumn<PurchaseOrderSummaryRow, String>  ordColUnitPrice;
    @FXML private TreeTableColumn<PurchaseOrderSummaryRow, String>  ordColTotal;

    @FXML private StackPane loadingOverlay;
    @FXML private ProgressIndicator progressIndicator;

    private final PurchaseService service = new PurchaseService();
    private final DecimalFormat   df      = createDecimalFormat();
    private Task<?>               currentLoadTask;

    // -------------------------------------------------------------------------
    // Init
    // -------------------------------------------------------------------------
    @FXML
    public void initialize() {
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now());

        setComboPlaceholder(categoryFilter,  "Loading…");
        setComboPlaceholder(partnerFilter,   "Loading…");
        setComboPlaceholder(warehouseFilter, "Loading…");

        itemField.setOnAction(e -> loadCurrentTab());

        setupSummaryColumns();
        setupDetailCatColumns();
        setupDetailParColumns();
        setupOrderColumns();

        // Reload when tab changes
        tabPane.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, tab) -> loadCurrentTab());

        loadDropdownsThenData();
    }

    // -------------------------------------------------------------------------
    // Dropdown + initial data in background
    // -------------------------------------------------------------------------
    private void loadDropdownsThenData() {
        loadingOverlay.setVisible(true);
        resultLabel.setText("Loading…");

        Task<DropdownData> task = new Task<>() {
            @Override protected DropdownData call() {
                return new DropdownData(
                    service.getCategories(),
                    service.getPartners(),
                    service.getWarehouses()
                );
            }
            @Override protected void succeeded() {
                DropdownData d = getValue();
                populateCombo(categoryFilter,  d.categories());
                populateCombo(partnerFilter,   d.partners());
                populateCombo(warehouseFilter, d.warehouses());
                loadCurrentTab();
            }
            @Override protected void failed() {
                populateCombo(categoryFilter,  List.of());
                populateCombo(partnerFilter,   List.of());
                populateCombo(warehouseFilter, List.of());
                loadingOverlay.setVisible(false);
                resultLabel.setText("Ready — dropdowns unavailable.");
            }
        };
        new Thread(task).start();
    }

    // -------------------------------------------------------------------------
    // Route to correct loader based on selected tab
    // -------------------------------------------------------------------------
    private void loadCurrentTab() {
        if (currentLoadTask != null && currentLoadTask.isRunning()) currentLoadTask.cancel();

        int idx = tabPane.getSelectionModel().getSelectedIndex();
        switch (idx) {
            case 0 -> loadSummary();
            case 1 -> loadDetailByCategory();
            case 2 -> loadDetailByPartner();
            case 3 -> loadOrderSummary();
        }
    }

    // -------------------------------------------------------------------------
    // Actions
    // -------------------------------------------------------------------------
    @FXML private void handleSearch()  { loadCurrentTab(); }

    @FXML
    private void handleRefresh() {
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now());
        categoryFilter.setValue("ALL");
        partnerFilter.setValue("ALL");
        warehouseFilter.setValue("ALL");
        itemField.clear();
        loadCurrentTab();
    }

    @FXML
    private void handleExport() {
        if (currentLoadTask != null && currentLoadTask.isRunning()) {
            showAlert("Loading", "Please wait for data to finish loading."); return;
        }
        int idx = tabPane.getSelectionModel().getSelectedIndex();
        switch (idx) {
            case 0 -> exportSummary();
            case 1 -> exportDetailCat();
            case 2 -> exportDetailPar();
            case 3 -> exportOrderSummary();
        }
    }

    // =========================================================================
    // TAB 0 — Purchase Summary
    // =========================================================================
    private void loadSummary() {
        final LocalDate fs = start(), fe = end();
        final String cat = combo(categoryFilter), item = text(itemField);
        beginLoad();
        currentLoadTask = new Task<List<PurchaseSummaryRow>>() {
            @Override protected List<PurchaseSummaryRow> call() {
                return service.getSummary(fs, fe, cat, item);
            }
            @Override protected void succeeded() {
                endLoad();
                List<PurchaseSummaryRow> rows = getValue();
                buildSummaryTree(rows);
                double grand = rows.stream().mapToDouble(PurchaseSummaryRow::getTotalCost).sum();
                resultLabel.setText(rows.size() + " item(s)");
                grandTotalLabel.setText("Grand Total: " + df.format(grand));
            }
            @Override protected void failed()     { loadFailed(); }
            @Override protected void cancelled()  { loadCancelled(); }
        };
        new Thread(currentLoadTask).start();
    }

    private void buildSummaryTree(List<PurchaseSummaryRow> rows) {
        TreeItem<PurchaseSummaryRow> root = new TreeItem<>(
            new PurchaseSummaryRow("", "", "", 0, "", 0));
        root.setExpanded(true);

        String curCat = null; TreeItem<PurchaseSummaryRow> catNode = null;
        Map<String, Double> catTotals = rows.stream()
            .collect(Collectors.groupingBy(PurchaseSummaryRow::getCategory,
                Collectors.summingDouble(PurchaseSummaryRow::getTotalCost)));

        for (PurchaseSummaryRow row : rows) {
            if (!row.getCategory().equals(curCat)) {
                curCat = row.getCategory();
                // Category header sentinel — name filled, rest empty
                catNode = new TreeItem<>(
                    new PurchaseSummaryRow(curCat, "", "",
                        0, "TOTAL: " + df.format(catTotals.getOrDefault(curCat, 0.0)), 0));
                catNode.setExpanded(true);
                root.getChildren().add(catNode);
            }
            if (catNode != null) catNode.getChildren().add(new TreeItem<>(row));
        }
        summaryTree.setRoot(root);
        summaryTree.setShowRoot(false);
    }

    private void setupSummaryColumns() {
        sumColCategory .setCellValueFactory(p -> sp(p.getValue().getValue().getCategory()));
        sumColName     .setCellValueFactory(p -> sp(p.getValue().getValue().getName()));
        sumColCode     .setCellValueFactory(p -> sp(p.getValue().getValue().getCode()));
        sumColQty      .setCellValueFactory(p -> {
            PurchaseSummaryRow r = p.getValue().getValue();
            return sp(r.getName().isBlank() ? "" : df.format(r.getQuantity()));
        });
        sumColUom      .setCellValueFactory(p -> sp(p.getValue().getValue().getUom()));
        sumColTotalCost.setCellValueFactory(p -> {
            PurchaseSummaryRow r = p.getValue().getValue();
            // Category row: show subtotal in Uom column (sentinel trick); item row: show totalCost
            return sp(r.getName().isBlank() ? "" : df.format(r.getTotalCost()));
        });
        sumColQty.setStyle("-fx-alignment: CENTER-RIGHT;");
        sumColTotalCost.setStyle("-fx-alignment: CENTER-RIGHT;");

        summaryTree.setRowFactory(tv -> new TreeTableRow<>() {
            @Override protected void updateItem(PurchaseSummaryRow r, boolean empty) {
                super.updateItem(r, empty);
                if (empty || r == null) { setStyle(""); return; }
                setStyle(r.getName().isBlank()
                    ? "-fx-background-color:#faf7f4; -fx-font-weight:bold;"
                    : "-fx-background-color:white;");
            }
        });
    }

    // =========================================================================
    // TAB 1 — Purchase Detail by Category
    // =========================================================================
    private void loadDetailByCategory() {
        final LocalDate fs = start(), fe = end();
        final String cat = combo(categoryFilter), par = combo(partnerFilter),
                     wh  = combo(warehouseFilter), item = text(itemField);
        beginLoad();
        currentLoadTask = new Task<List<PurchaseDetailRow>>() {
            @Override protected List<PurchaseDetailRow> call() {
                return service.getDetailByCategory(fs, fe, cat, par, wh, item);
            }
            @Override protected void succeeded() {
                endLoad();
                List<PurchaseDetailRow> rows = getValue();
                buildDetailTree(rows, detailCatTree, false);
                updateTotals(rows);
            }
            @Override protected void failed()    { loadFailed(); }
            @Override protected void cancelled() { loadCancelled(); }
        };
        new Thread(currentLoadTask).start();
    }

    private void setupDetailCatColumns() {
        // Group column shows Category header; item rows show ItemName
        dcColGroup      .setCellValueFactory(p -> {
            PurchaseDetailRow r = p.getValue().getValue();
            return sp(r == null ? "" : r.getInvoiceId() == -1 ? r.getCategory() : r.getItemName());
        });
        dcColGroup.setCellFactory(col -> boldHeaderCell());

        dcColDate       .setCellValueFactory(p -> sp(orBlank(p, r -> r.getInvoiceId() > 0, r -> r.getDateFormatted())));
        dcColInvoice    .setCellValueFactory(p -> sp(orBlank(p, r -> r.getInvoiceId() > 0, r -> String.valueOf(r.getInvoiceId()))));
        dcColPurchaseQty.setCellValueFactory(p -> sp(orBlank(p, r -> r.getInvoiceId() > 0, r -> df.format(r.getPurchaseQuantity()))));
        dcColPurchaseUom.setCellValueFactory(p -> sp(orBlank(p, r -> r.getInvoiceId() > 0, r -> r.getPurchaseUom())));
        dcColQty        .setCellValueFactory(p -> sp(orBlank(p, r -> r.getInvoiceId() > 0, r -> df.format(r.getQuantity()))));
        dcColUom        .setCellValueFactory(p -> sp(orBlank(p, r -> r.getInvoiceId() > 0, r -> r.getUom())));
        dcColUnitCost   .setCellValueFactory(p -> sp(orBlank(p, r -> r.getInvoiceId() > 0, r -> df.format(r.getUnitCost()))));
        dcColTotal      .setCellValueFactory(p -> sp(orBlank(p, r -> r.getInvoiceId() > 0, r -> df.format(r.getTotal()))));
        dcColPartner    .setCellValueFactory(p -> sp(orBlank(p, r -> r.getInvoiceId() > 0, r -> r.getPartner())));
        dcColWarehouse  .setCellValueFactory(p -> sp(orBlank(p, r -> r.getInvoiceId() > 0, r -> r.getWarehouse())));

        List.of(dcColPurchaseQty, dcColQty, dcColUnitCost, dcColTotal)
            .forEach(c -> c.setStyle("-fx-alignment: CENTER-RIGHT;"));
        applyDetailRowStyle(detailCatTree);
    }

    // =========================================================================
    // TAB 2 — Purchase Detail by Partner
    // =========================================================================
    private void loadDetailByPartner() {
        final LocalDate fs = start(), fe = end();
        final String cat = combo(categoryFilter), par = combo(partnerFilter),
                     wh  = combo(warehouseFilter), item = text(itemField);
        beginLoad();
        currentLoadTask = new Task<List<PurchaseDetailRow>>() {
            @Override protected List<PurchaseDetailRow> call() {
                return service.getDetailByPartner(fs, fe, cat, par, wh, item);
            }
            @Override protected void succeeded() {
                endLoad();
                List<PurchaseDetailRow> rows = getValue();
                buildDetailTree(rows, detailParTree, true);
                updateTotals(rows);
            }
            @Override protected void failed()    { loadFailed(); }
            @Override protected void cancelled() { loadCancelled(); }
        };
        new Thread(currentLoadTask).start();
    }

    private void setupDetailParColumns() {
        dpColGroup      .setCellValueFactory(p -> {
            PurchaseDetailRow r = p.getValue().getValue();
            return sp(r == null ? "" : r.getInvoiceId() == -1 ? r.getPartner() : r.getItemName());
        });
        dpColGroup.setCellFactory(col -> boldHeaderCell());

        dpColDate       .setCellValueFactory(p -> sp(orBlank(p, r -> r.getInvoiceId() > 0, r -> r.getDateFormatted())));
        dpColInvoice    .setCellValueFactory(p -> sp(orBlank(p, r -> r.getInvoiceId() > 0, r -> String.valueOf(r.getInvoiceId()))));
        dpColCategory   .setCellValueFactory(p -> sp(orBlank(p, r -> r.getInvoiceId() > 0, r -> r.getCategory())));
        dpColItem       .setCellValueFactory(p -> sp(orBlank(p, r -> r.getInvoiceId() > 0, r -> r.getItemName())));
        dpColPurchaseQty.setCellValueFactory(p -> sp(orBlank(p, r -> r.getInvoiceId() > 0, r -> df.format(r.getPurchaseQuantity()))));
        dpColPurchaseUom.setCellValueFactory(p -> sp(orBlank(p, r -> r.getInvoiceId() > 0, r -> r.getPurchaseUom())));
        dpColQty        .setCellValueFactory(p -> sp(orBlank(p, r -> r.getInvoiceId() > 0, r -> df.format(r.getQuantity()))));
        dpColUom        .setCellValueFactory(p -> sp(orBlank(p, r -> r.getInvoiceId() > 0, r -> r.getUom())));
        dpColUnitCost   .setCellValueFactory(p -> sp(orBlank(p, r -> r.getInvoiceId() > 0, r -> df.format(r.getUnitCost()))));
        dpColTotal      .setCellValueFactory(p -> sp(orBlank(p, r -> r.getInvoiceId() > 0, r -> df.format(r.getTotal()))));
        dpColWarehouse  .setCellValueFactory(p -> sp(orBlank(p, r -> r.getInvoiceId() > 0, r -> r.getWarehouse())));

        List.of(dpColPurchaseQty, dpColQty, dpColUnitCost, dpColTotal)
            .forEach(c -> c.setStyle("-fx-alignment: CENTER-RIGHT;"));
        applyDetailRowStyle(detailParTree);
    }

    /**
     * Shared tree builder for both detail tabs.
     * groupByPartner = false → group key = Category
     * groupByPartner = true  → group key = Partner
     * Sentinel rows use invoiceId = -1.
     */
    private void buildDetailTree(List<PurchaseDetailRow> rows,
                                  TreeTableView<PurchaseDetailRow> tree,
                                  boolean groupByPartner) {
        TreeItem<PurchaseDetailRow> root = new TreeItem<>(sentinel("", "", ""));
        root.setExpanded(true);

        Map<String, List<PurchaseDetailRow>> grouped = rows.stream()
            .collect(Collectors.groupingBy(
                groupByPartner ? PurchaseDetailRow::getPartner : PurchaseDetailRow::getCategory,
                LinkedHashMap::new, Collectors.toList()
            ));

        for (Map.Entry<String, List<PurchaseDetailRow>> entry : grouped.entrySet()) {
            String groupKey = entry.getKey();
            List<PurchaseDetailRow> groupRows = entry.getValue();
            double groupTotal = groupRows.stream().mapToDouble(PurchaseDetailRow::getTotal).sum();

            // Sentinel header row — invoiceId=-1 signals "group header"
            PurchaseDetailRow header = sentinel(
                groupByPartner ? "" : groupKey,   // category
                groupByPartner ? groupKey : "",   // partner
                "TOTAL: " + df.format(groupTotal) // stash total in warehouse field
            );
            TreeItem<PurchaseDetailRow> groupNode = new TreeItem<>(header);
            groupNode.setExpanded(false);

            for (PurchaseDetailRow r : groupRows) {
                groupNode.getChildren().add(new TreeItem<>(r));
            }
            root.getChildren().add(groupNode);
        }
        tree.setRoot(root);
        tree.setShowRoot(false);
    }

    // =========================================================================
    // TAB 3 — Purchase Order Summary
    // =========================================================================
    private void loadOrderSummary() {
        final String cat = combo(categoryFilter), item = text(itemField);
        beginLoad();
        currentLoadTask = new Task<List<PurchaseOrderSummaryRow>>() {
            @Override protected List<PurchaseOrderSummaryRow> call() {
                return service.getOrderSummary(cat, item);
            }
            @Override protected void succeeded() {
                endLoad();
                List<PurchaseOrderSummaryRow> rows = getValue();
                buildOrderTree(rows);
                double grand = rows.stream().mapToDouble(PurchaseOrderSummaryRow::getTotal).sum();
                resultLabel.setText(rows.size() + " item(s)");
                grandTotalLabel.setText("Grand Total: " + df.format(grand));
            }
            @Override protected void failed()    { loadFailed(); }
            @Override protected void cancelled() { loadCancelled(); }
        };
        new Thread(currentLoadTask).start();
    }

    private void buildOrderTree(List<PurchaseOrderSummaryRow> rows) {
        TreeItem<PurchaseOrderSummaryRow> root = new TreeItem<>(
            new PurchaseOrderSummaryRow("", "", "", 0, 0, "", 0, 0));
        root.setExpanded(true);

        String curCat = null; TreeItem<PurchaseOrderSummaryRow> catNode = null;
        Map<String, Double> catTotals = rows.stream()
            .collect(Collectors.groupingBy(PurchaseOrderSummaryRow::getCategory,
                Collectors.summingDouble(PurchaseOrderSummaryRow::getTotal)));

        for (PurchaseOrderSummaryRow row : rows) {
            if (!row.getCategory().equals(curCat)) {
                curCat = row.getCategory();
                catNode = new TreeItem<>(new PurchaseOrderSummaryRow(
                    curCat, "", "", 0, 0, "", 0,
                    catTotals.getOrDefault(curCat, 0.0)));
                catNode.setExpanded(true);
                root.getChildren().add(catNode);
            }
            if (catNode != null) catNode.getChildren().add(new TreeItem<>(row));
        }
        orderTree.setRoot(root);
        orderTree.setShowRoot(false);
    }

    private void setupOrderColumns() {
        ordColCategory .setCellValueFactory(p -> sp(p.getValue().getValue().getCategory()));
        ordColItemName .setCellValueFactory(p -> {
            PurchaseOrderSummaryRow r = p.getValue().getValue();
            return sp(r.getItemName().isBlank() ? "" : r.getItemName());
        });
        ordColItemCode .setCellValueFactory(p -> sp(p.getValue().getValue().getItemCode()));
        ordColQty      .setCellValueFactory(p -> {
            PurchaseOrderSummaryRow r = p.getValue().getValue();
            return sp(r.getItemName().isBlank() ? "" : df.format(r.getQuantity()));
        });
        ordColReceived .setCellValueFactory(p -> {
            PurchaseOrderSummaryRow r = p.getValue().getValue();
            return sp(r.getItemName().isBlank() ? "" : df.format(r.getTotalReceived()));
        });
        ordColUom      .setCellValueFactory(p -> sp(p.getValue().getValue().getPurchaseUom()));
        ordColUnitPrice.setCellValueFactory(p -> {
            PurchaseOrderSummaryRow r = p.getValue().getValue();
            return sp(r.getItemName().isBlank() ? "" : df.format(r.getUnitPrice()));
        });
        ordColTotal    .setCellValueFactory(p -> sp(df.format(p.getValue().getValue().getTotal())));

        List.of(ordColQty, ordColReceived, ordColUnitPrice, ordColTotal)
            .forEach(c -> c.setStyle("-fx-alignment: CENTER-RIGHT;"));

        orderTree.setRowFactory(tv -> new TreeTableRow<>() {
            @Override protected void updateItem(PurchaseOrderSummaryRow r, boolean empty) {
                super.updateItem(r, empty);
                if (empty || r == null) { setStyle(""); return; }
                setStyle(r.getItemName().isBlank()
                    ? "-fx-background-color:#faf7f4; -fx-font-weight:bold;"
                    : "-fx-background-color:white;");
            }
        });
    }

    // =========================================================================
    // Export methods
    // =========================================================================
    private void exportSummary() {
        final LocalDate fs = start(), fe = end();
        final String cat = combo(categoryFilter), item = text(itemField);
        runExport(() -> service.getSummary(fs, fe, cat, item), rows -> {
            try (PrintWriter w = csvWriter("purchase_summary")) {
                w.println("Category,Item Name,Code,Quantity,UOM,Total Cost");
                for (PurchaseSummaryRow r : (List<PurchaseSummaryRow>) rows) {
                    w.println(String.join(",", csv(r.getCategory()), csv(r.getName()),
                        csv(r.getCode()), num(r.getQuantity()), csv(r.getUom()), num(r.getTotalCost())));
                }
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    private void exportDetailCat() { exportDetail(false); }
    private void exportDetailPar() { exportDetail(true); }

    private void exportDetail(boolean byPartner) {
        final LocalDate fs = start(), fe = end();
        final String cat = combo(categoryFilter), par = combo(partnerFilter),
                     wh = combo(warehouseFilter), item = text(itemField);
        String prefix = byPartner ? "purchase_detail_by_partner" : "purchase_detail_by_category";
        runExport(
            () -> byPartner ? service.getDetailByPartner(fs, fe, cat, par, wh, item)
                            : service.getDetailByCategory(fs, fe, cat, par, wh, item),
            rows -> {
                try (PrintWriter w = csvWriter(prefix)) {
                    w.println("Category,Item Name,Invoice ID,Date,Purchase Qty,Purchase UOM," +
                              "Conversion,Qty,UOM,Unit Cost,Total,Partner,Warehouse,Created By");
                    for (PurchaseDetailRow r : (List<PurchaseDetailRow>) rows) {
                        w.println(String.join(",",
                            csv(r.getCategory()), csv(r.getItemName()),
                            csv(String.valueOf(r.getInvoiceId())), csv(r.getDateFormatted()),
                            num(r.getPurchaseQuantity()), csv(r.getPurchaseUom()),
                            num(r.getPurchaseConversion()), num(r.getQuantity()),
                            csv(r.getUom()), num(r.getUnitCost()), num(r.getTotal()),
                            csv(r.getPartner()), csv(r.getWarehouse()), csv(r.getCreatedBy())));
                    }
                } catch (Exception e) { e.printStackTrace(); }
            });
    }

    private void exportOrderSummary() {
        final String cat = combo(categoryFilter), item = text(itemField);
        runExport(() -> service.getOrderSummary(cat, item), rows -> {
            try (PrintWriter w = csvWriter("purchase_order_summary")) {
                w.println("Category,Item Name,Item Code,Qty Ordered,Qty Received,UOM,Unit Price,Total");
                for (PurchaseOrderSummaryRow r : (List<PurchaseOrderSummaryRow>) rows) {
                    w.println(String.join(",", csv(r.getCategory()), csv(r.getItemName()),
                        csv(r.getItemCode()), num(r.getQuantity()), num(r.getTotalReceived()),
                        csv(r.getPurchaseUom()), num(r.getUnitPrice()), num(r.getTotal())));
                }
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    @SuppressWarnings("unchecked")
    private void runExport(java.util.concurrent.Callable<List<?>> fetch,
                           java.util.function.Consumer<List<?>> write) {
        loadingOverlay.setVisible(true);
        Task<List<?>> task = new Task<>() {
            @Override protected List<?> call() throws Exception { return fetch.call(); }
            @Override protected void succeeded() {
                loadingOverlay.setVisible(false);
                List<?> rows = getValue();
                if (rows.isEmpty()) { showAlert("No Data", "Nothing to export."); return; }
                write.accept(rows);
            }
            @Override protected void failed() {
                loadingOverlay.setVisible(false);
                showAlert("Export Error", getException().getMessage());
            }
        };
        new Thread(task).start();
    }

    // =========================================================================
    // Utility
    // =========================================================================
    private void beginLoad() {
        loadingOverlay.setVisible(true);
        resultLabel.setText("Loading…");
        grandTotalLabel.setText("Grand Total: --");
    }
    private void endLoad()       { loadingOverlay.setVisible(false); }
    private void loadFailed()    { endLoad(); showAlert("Database Error", currentLoadTask == null ? "" : "Error"); resultLabel.setText("Error"); }
    private void loadCancelled() { endLoad(); resultLabel.setText("Cancelled"); }

    private void updateTotals(List<PurchaseDetailRow> rows) {
        double grand = rows.stream().mapToDouble(PurchaseDetailRow::getTotal).sum();
        resultLabel.setText(rows.size() + " line(s)");
        grandTotalLabel.setText("Grand Total: " + df.format(grand));
    }

    /** Sentinel PurchaseDetailRow used as group header — invoiceId = -1. */
    private static PurchaseDetailRow sentinel(String category, String partner, String warehouse) {
        return new PurchaseDetailRow(category, "", -1, null, 0, "", 0, 0, "", 0, 0, partner, warehouse, "");
    }

    private <R> String orBlank(
            javafx.scene.control.TreeTableColumn.CellDataFeatures<PurchaseDetailRow, String> p,
            java.util.function.Predicate<PurchaseDetailRow> condition,
            java.util.function.Function<PurchaseDetailRow, String> extractor) {
        PurchaseDetailRow r = p.getValue().getValue();
        if (r == null || !condition.test(r)) return "";
        return extractor.apply(r);
    }

    private <R> TreeTableCell<PurchaseDetailRow, String> boldHeaderCell() {
        return new TreeTableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                PurchaseDetailRow r = getTreeTableRow().getItem();
                setStyle(r != null && r.getInvoiceId() == -1
                    ? "-fx-font-weight:bold; -fx-text-fill:#3a2316;"
                    : "-fx-text-fill:#4a3728;");
            }
        };
    }

    private void applyDetailRowStyle(TreeTableView<PurchaseDetailRow> tv) {
        tv.setRowFactory(t -> new TreeTableRow<>() {
            @Override protected void updateItem(PurchaseDetailRow r, boolean empty) {
                super.updateItem(r, empty);
                if (empty || r == null) { setStyle(""); return; }
                setStyle(r.getInvoiceId() == -1
                    ? "-fx-background-color:#faf7f4;"
                    : "-fx-background-color:white;");
            }
        });
    }

    private PrintWriter csvWriter(String prefix) throws Exception {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path fp = Paths.get(System.getProperty("user.home"), "Downloads", prefix + "_" + ts + ".csv");
        PrintWriter w = new PrintWriter(fp.toFile(), "UTF-8");
        w.write('\uFEFF');
        return w;
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
    private LocalDate start() {
        LocalDate d = startDatePicker.getValue();
        return d != null ? d : LocalDate.now();
    }
    private LocalDate end() {
        LocalDate d = endDatePicker.getValue();
        return d != null ? d : LocalDate.now();
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
    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg);
        a.setTitle(title); a.setHeaderText(null); a.showAndWait();
    }

    private record DropdownData(List<String> categories,
                                List<String> partners,
                                List<String> warehouses) {}
}