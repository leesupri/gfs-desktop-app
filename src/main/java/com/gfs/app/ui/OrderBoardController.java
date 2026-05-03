package com.gfs.app.ui;

import com.gfs.app.model.OrderBillSummary;
import com.gfs.app.model.OrderLineRow;
import com.gfs.app.service.OrderBoardService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
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

public class OrderBoardController {

    @FXML private DatePicker       startDatePicker;
    @FXML private DatePicker       endDatePicker;
    @FXML private TextField        invoiceField;
    @FXML private TextField        tableField;
    @FXML private ComboBox<String> stationFilter;
    @FXML private ComboBox<String> departmentFilter;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private TextField        keywordField;

    @FXML private Label resultLabel;
    @FXML private Label grossLabel;
    @FXML private Label discountLabel;
    @FXML private Label netLabel;

    @FXML private TreeTableView<Object>           treeTable;
    @FXML private TreeTableColumn<Object, String> colInvoice;
    @FXML private TreeTableColumn<Object, String> colDate;
    @FXML private TreeTableColumn<Object, String> colType;
    @FXML private TreeTableColumn<Object, String> colTable;
    @FXML private TreeTableColumn<Object, String> colStation;
    @FXML private TreeTableColumn<Object, String> colDept;
    @FXML private TreeTableColumn<Object, String> colItem;
    @FXML private TreeTableColumn<Object, String> colQty;
    @FXML private TreeTableColumn<Object, String> colPrice;
    @FXML private TreeTableColumn<Object, String> colDiscount;
    @FXML private TreeTableColumn<Object, String> colNet;
    @FXML private TreeTableColumn<Object, String> colEmployee;
    @FXML private TreeTableColumn<Object, String> colClosedBy;

    @FXML private StackPane         loadingOverlay;
    @FXML private ProgressIndicator progressIndicator;

    private final OrderBoardService      service = new OrderBoardService();
    private final DecimalFormat          df      = createDecimalFormat();
    private Task<List<OrderBillSummary>> currentLoadTask;

    // -------------------------------------------------------------------------
    // Init — zero DB work on the FX thread
    // -------------------------------------------------------------------------
    @FXML
    public void initialize() {
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now());

        // Set combos to disabled placeholder — DB not touched yet
        setComboPlaceholder(stationFilter,    "Loading…");
        setComboPlaceholder(departmentFilter, "Loading…");
        setComboPlaceholder(categoryFilter,   "Loading…");

        invoiceField.setOnAction(e -> loadData());
        tableField  .setOnAction(e -> loadData());
        keywordField.setOnAction(e -> loadData());

        setupColumns();

        // Single background task: load all 3 dropdowns first, then fire data load
        loadDropdownsThenData();
    }

    // -------------------------------------------------------------------------
    // Phase 1 — fetch dropdowns + trigger data load, all off the FX thread
    // -------------------------------------------------------------------------
    private void loadDropdownsThenData() {
        loadingOverlay.setVisible(true);
        resultLabel.setText("Loading…");

        Task<DropdownData> task = new Task<>() {
            @Override
            protected DropdownData call() {
                return new DropdownData(
                    service.getStations(),
                    service.getDepartments(),
                    service.getCategories()
                );
            }

            @Override
            protected void succeeded() {
                DropdownData d = getValue();
                populateCombo(stationFilter,    d.stations());
                populateCombo(departmentFilter, d.departments());
                populateCombo(categoryFilter,   d.categories());
                // Now safe to fire data load — combos are ready
                loadData();
            }

            @Override
            protected void failed() {
                // Still allow searching even if dropdowns failed
                populateCombo(stationFilter,    List.of());
                populateCombo(departmentFilter, List.of());
                populateCombo(categoryFilter,   List.of());
                loadingOverlay.setVisible(false);
                resultLabel.setText("Ready — filter dropdowns unavailable.");
            }
        };
        new Thread(task).start();
    }

    // -------------------------------------------------------------------------
    // Actions
    // -------------------------------------------------------------------------
    @FXML private void handleSearch() { loadData(); }

    @FXML
    private void handleRefresh() {
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now());
        invoiceField.clear();
        tableField.clear();
        keywordField.clear();
        stationFilter.setValue("ALL");
        departmentFilter.setValue("ALL");
        categoryFilter.setValue("ALL");
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
        final String inv = text(invoiceField), tbl = text(tableField);
        final String sta = combo(stationFilter), dep = combo(departmentFilter);
        final String cat = combo(categoryFilter), kw  = text(keywordField);

        loadingOverlay.setVisible(true);
        Task<List<OrderBillSummary>> exportTask = new Task<>() {
            @Override protected List<OrderBillSummary> call() {
                return service.getBills(start, end, inv, tbl, sta, dep, cat, kw);
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
        new Thread(exportTask).start();
    }

    // -------------------------------------------------------------------------
    // Phase 2 — background data load, cancels any in-flight task first
    // -------------------------------------------------------------------------
    private void loadData() {
        if (currentLoadTask != null && currentLoadTask.isRunning()) {
            currentLoadTask.cancel();
        }

        LocalDate start = startDatePicker.getValue();
        LocalDate end   = endDatePicker.getValue();
        if (start == null) start = LocalDate.now();
        if (end   == null) end   = LocalDate.now();
        if (start.isAfter(end)) { LocalDate t = start; start = end; end = t; }

        final LocalDate fs = start, fe = end;
        final String inv = text(invoiceField), tbl = text(tableField);
        final String sta = combo(stationFilter), dep = combo(departmentFilter);
        final String cat = combo(categoryFilter), kw  = text(keywordField);

        loadingOverlay.setVisible(true);
        resultLabel.setText("Loading…");
        grossLabel.setText("--");
        discountLabel.setText("--");
        netLabel.setText("--");

        currentLoadTask = new Task<>() {
            @Override protected List<OrderBillSummary> call() {
                return service.getBills(fs, fe, inv, tbl, sta, dep, cat, kw);
            }
            @Override protected void succeeded() {
                loadingOverlay.setVisible(false);
                updateUI(getValue());
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
    // Columns
    // -------------------------------------------------------------------------
    private void setupColumns() {
        colInvoice.setCellValueFactory(p -> {
            Object v = p.getValue().getValue();
            if (v instanceof OrderBillSummary b) return sp(String.valueOf(b.getInvoiceId()));
            return sp("");
        });
        colInvoice.setCellFactory(col -> new TreeTableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) { setText(null); setStyle(""); return; }
                if (getTreeTableRow().getItem() instanceof OrderBillSummary) {
                    setText(item);
                    setStyle("-fx-font-weight:bold; -fx-text-fill:#3a2316;");
                } else { setText(null); setStyle(""); }
            }
        });

        colDate.setCellValueFactory(p -> {
            Object v = p.getValue().getValue();
            if (v instanceof OrderBillSummary b) return sp(b.getDateFormatted());
            if (v instanceof OrderLineRow     r) return sp(r.getCreatedFormatted());
            return sp("");
        });
        colType.setCellValueFactory(p -> {
            Object v = p.getValue().getValue();
            if (v instanceof OrderBillSummary b) return sp(b.getSalesType());
            return sp("");
        });
        colTable.setCellValueFactory(p -> {
            Object v = p.getValue().getValue();
            if (v instanceof OrderBillSummary b) return sp(b.getTableName());
            return sp("");
        });
        colStation.setCellValueFactory(p -> {
            Object v = p.getValue().getValue();
            if (v instanceof OrderBillSummary b) {
                String os = b.getOrderStation(), cs = b.getCloseStation();
                return sp(os.equals(cs) ? os : os + " → " + cs);
            }
            if (v instanceof OrderLineRow r) return sp(r.getCreatedAtHo());
            return sp("");
        });
        colDept.setCellValueFactory(p -> {
            Object v = p.getValue().getValue();
            if (v instanceof OrderLineRow r)
                return sp(r.getDepartment() + " / " + r.getCategory());
            return sp("");
        });
        colItem.setCellValueFactory(p -> {
            Object v = p.getValue().getValue();
            if (v instanceof OrderBillSummary b) return sp(b.getItemCount() + " item(s)");
            if (v instanceof OrderLineRow     r) return sp(r.getDescription());
            return sp("");
        });
        colQty.setCellValueFactory(p -> {
            Object v = p.getValue().getValue();
            if (v instanceof OrderLineRow r) return sp(fmt0(r.getQuantity()));
            return sp("");
        });
        colPrice.setCellValueFactory(p -> {
            Object v = p.getValue().getValue();
            if (v instanceof OrderBillSummary b) return sp(df.format(b.getGross()));
            if (v instanceof OrderLineRow     r) return sp(df.format(r.getLineTotal()));
            return sp("");
        });
        colDiscount.setCellValueFactory(p -> {
            Object v = p.getValue().getValue();
            if (v instanceof OrderBillSummary b) return sp(df.format(b.getDiscount()));
            if (v instanceof OrderLineRow     r) return sp(df.format(r.getDiscountAmount()));
            return sp("");
        });
        colNet.setCellValueFactory(p -> {
            Object v = p.getValue().getValue();
            if (v instanceof OrderBillSummary b) return sp(df.format(b.getNet()));
            return sp("");
        });
        colEmployee.setCellValueFactory(p -> {
            Object v = p.getValue().getValue();
            if (v instanceof OrderBillSummary b) return sp(b.getOrderedBy());
            if (v instanceof OrderLineRow     r) return sp(r.getEmployee());
            return sp("");
        });
        colClosedBy.setCellValueFactory(p -> {
            Object v = p.getValue().getValue();
            if (v instanceof OrderBillSummary b) return sp(b.getClosedBy());
            return sp("");
        });

        List.of(colQty, colPrice, colDiscount, colNet)
            .forEach(c -> c.setStyle("-fx-alignment: CENTER-RIGHT;"));

        treeTable.setRowFactory(tv -> new TreeTableRow<>() {
            @Override protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setStyle(""); return; }
                setStyle(item instanceof OrderBillSummary
                    ? "-fx-background-color:#faf7f4;"
                    : "-fx-background-color:white;");
            }
        });
    }

    // -------------------------------------------------------------------------
    // Tree builder
    // -------------------------------------------------------------------------
    private void updateUI(List<OrderBillSummary> bills) {
        TreeItem<Object> root = new TreeItem<>("ROOT");
        root.setExpanded(true);

        for (OrderBillSummary bill : bills) {
            TreeItem<Object> billNode = new TreeItem<>(bill);
            billNode.setExpanded(false);
            for (OrderLineRow line : bill.getLines()) {
                billNode.getChildren().add(new TreeItem<>(line));
            }
            root.getChildren().add(billNode);
        }

        treeTable.setRoot(root);
        treeTable.setShowRoot(false);

        int    billCount = bills.size();
        int    lineCount = bills.stream().mapToInt(OrderBillSummary::getItemCount).sum();
        double gross     = bills.stream().mapToDouble(OrderBillSummary::getGross).sum();
        double discount  = bills.stream().mapToDouble(OrderBillSummary::getDiscount).sum();

        resultLabel  .setText(billCount + " bill(s) / " + lineCount + " line(s)");
        grossLabel   .setText(df.format(gross));
        discountLabel.setText(df.format(discount));
        netLabel     .setText(df.format(gross - discount));
    }

    // -------------------------------------------------------------------------
    // CSV Export
    // -------------------------------------------------------------------------
    private void exportToCsv(List<OrderBillSummary> bills) {
        if (bills.isEmpty()) { showAlert("No Data", "Nothing to export."); return; }
        try {
            String downloadsPath = System.getProperty("user.home") + File.separator + "Downloads";
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path filePath = Paths.get(downloadsPath, "order_board_" + timestamp + ".csv");

            try (PrintWriter w = new PrintWriter(filePath.toFile(), "UTF-8")) {
                w.write('\uFEFF');
                w.println("Invoice ID,Date,Type,Table,Customer,Order Station,Close Station," +
                          "Ordered By,Closed By,Department,Category,Description,Qty,Unit Price,Line Total,Discount");
                for (OrderBillSummary bill : bills) {
                    for (OrderLineRow r : bill.getLines()) {
                        w.println(String.join(",",
                            csv(String.valueOf(bill.getInvoiceId())),
                            csv(bill.getDateFormatted()),
                            csv(bill.getSalesType()),
                            csv(bill.getTableName()),
                            csv(bill.getCustomer()),
                            csv(bill.getOrderStation()),
                            csv(bill.getCloseStation()),
                            csv(bill.getOrderedBy()),
                            csv(bill.getClosedBy()),
                            csv(r.getDepartment()),
                            csv(r.getCategory()),
                            csv(r.getDescription()),
                            csv(fmt0(r.getQuantity())),
                            num(r.getUnitPrice()),
                            num(r.getLineTotal()),
                            num(r.getDiscountAmount())
                        ));
                    }
                }
            }
            Alert alert = new Alert(Alert.AlertType.INFORMATION,
                "Saved to Downloads:\n" + filePath.getFileName());
            alert.setTitle("Export Successful");
            alert.setHeaderText("Order Board Exported");
            ButtonType open   = new ButtonType("Open File");
            ButtonType folder = new ButtonType("Open Folder");
            ButtonType close  = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(open, folder, close);
            alert.showAndWait().ifPresent(b -> {
                try {
                    if (b == open)   Desktop.getDesktop().open(filePath.toFile());
                    if (b == folder) Desktop.getDesktop().open(filePath.getParent().toFile());
                } catch (Exception ex) { ex.printStackTrace(); }
            });
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Export Error", e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------
    private record DropdownData(List<String> stations,
                                List<String> departments,
                                List<String> categories) {}

    private static DecimalFormat createDecimalFormat() {
        DecimalFormat d = new DecimalFormat("#,###.00");
        d.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.GERMANY));
        return d;
    }

    private String num(double v) {
        return "\"" + (v == 0.0 ? "0,00" : df.format(v)) + "\"";
    }

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

    private static String fmt0(double v) { return String.format("%.0f", v); }

    private static SimpleStringProperty sp(String v) {
        return new SimpleStringProperty(v != null ? v : "");
    }

    private static void setComboPlaceholder(ComboBox<String> combo, String label) {
        combo.setItems(FXCollections.observableArrayList(label));
        combo.setValue(label);
        combo.setDisable(true);
    }

    private static void populateCombo(ComboBox<String> combo, List<String> values) {
        values.add(0, "ALL");
        combo.setItems(FXCollections.observableArrayList(values));
        combo.setValue("ALL");
        combo.setDisable(false);
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg);
        a.setTitle(title);
        a.setHeaderText(null);
        a.showAndWait();
    }
}