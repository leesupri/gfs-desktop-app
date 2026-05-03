package com.gfs.app.ui;

import com.gfs.app.model.ReceiptSummaryRow;
import com.gfs.app.service.ReceiptService;
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
import java.util.Optional;

public class ReceiptSummaryController {

    @FXML private DatePicker       startDatePicker;
    @FXML private DatePicker       endDatePicker;
    @FXML private TextField        invoiceField;
    @FXML private ComboBox<String> typeFilter;
    @FXML private Label            resultLabel;
    @FXML private Label            totalLabel;

    @FXML private TableView<ReceiptSummaryRow>              tableView;
    @FXML private TableColumn<ReceiptSummaryRow, String>    colInvoice;
    @FXML private TableColumn<ReceiptSummaryRow, String>    colDate;
    @FXML private TableColumn<ReceiptSummaryRow, String>    colType;
    @FXML private TableColumn<ReceiptSummaryRow, String>    colPax;
    @FXML private TableColumn<ReceiptSummaryRow, String>    colSubtotal;
    @FXML private TableColumn<ReceiptSummaryRow, String>    colDiscount;
    @FXML private TableColumn<ReceiptSummaryRow, String>    colService;
    @FXML private TableColumn<ReceiptSummaryRow, String>    colTax;
    @FXML private TableColumn<ReceiptSummaryRow, String>    colRounding;
    @FXML private TableColumn<ReceiptSummaryRow, String>    colTotal;

    @FXML private StackPane        loadingOverlay;
    @FXML private ProgressIndicator progressIndicator;

    private final ReceiptService  service       = new ReceiptService();
    private final DecimalFormat   df            = createDecimalFormat();
    private Task<List<ReceiptSummaryRow>> currentLoadTask;

    @FXML
    public void initialize() {
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now());

        // Type filter dropdown
        List<String> types = service.getTypes();
        types.add(0, "ALL");
        typeFilter.setItems(FXCollections.observableArrayList(types));
        typeFilter.setValue("ALL");

        // Column bindings
        colInvoice .setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getInvoiceId())));
        colDate    .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDateFormatted()));
        colType    .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getType()));
        colPax     .setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getPax())));
        colSubtotal.setCellValueFactory(d -> new SimpleStringProperty(df.format(d.getValue().getSubtotal())));
        colDiscount.setCellValueFactory(d -> new SimpleStringProperty(df.format(d.getValue().getDiscountAmount())));
        colService .setCellValueFactory(d -> new SimpleStringProperty(df.format(d.getValue().getServiceChargeAmount())));
        colTax     .setCellValueFactory(d -> new SimpleStringProperty(df.format(d.getValue().getTax1Amount())));
        colRounding.setCellValueFactory(d -> new SimpleStringProperty(df.format(d.getValue().getRoundingAmount())));
        colTotal   .setCellValueFactory(d -> new SimpleStringProperty(df.format(d.getValue().getTotal())));

        // Invoice column — clickable link style
        colInvoice.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle("-fx-text-fill: #3a2316; -fx-font-weight: bold; -fx-underline: true; -fx-cursor: hand;");
            }
        });

        // Double-click row → open detail dialog
        tableView.setRowFactory(tv -> {
            TableRow<ReceiptSummaryRow> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    openDetail(row.getItem());
                }
            });
            return row;
        });

        invoiceField.setOnAction(e -> loadData());
        loadData();
    }

    @FXML private void handleSearch()  { loadData(); }

    @FXML
    private void handleRefresh() {
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now());
        invoiceField.clear();
        typeFilter.setValue("ALL");
        loadData();
    }

    @FXML
    private void handleExport() {
        if (currentLoadTask != null && currentLoadTask.isRunning()) {
            showAlert("Loading", "Please wait for data to finish loading.");
            return;
        }
        final LocalDate start   = startDatePicker.getValue();
        final LocalDate end     = endDatePicker.getValue();
        final String    invoice = text(invoiceField);
        final String    type    = typeFilter.getValue();

        loadingOverlay.setVisible(true);
        Task<List<ReceiptSummaryRow>> task = new Task<>() {
            @Override protected List<ReceiptSummaryRow> call() {
                return service.getSummary(start, end, invoice, type);
            }
            @Override protected void succeeded() { loadingOverlay.setVisible(false); exportToCsv(getValue()); }
            @Override protected void failed()    { loadingOverlay.setVisible(false); showAlert("Export Error", getException().getMessage()); }
        };
        new Thread(task).start();
    }

    // -------------------------------------------------------------------------
    // Detail dialog — opened on row double-click
    // -------------------------------------------------------------------------
    private void openDetail(ReceiptSummaryRow summary) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/receipt-detail.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Receipt Detail — Invoice #" + summary.getInvoiceId());
            stage.setMinWidth(760);
            stage.setMinHeight(560);

            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(
                getClass().getResource("/css/app.css").toExternalForm());

            ReceiptDetailController controller = loader.getController();
            controller.loadInvoice(summary.getInvoiceId());

            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not open receipt detail: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Background load
    // -------------------------------------------------------------------------
    private void loadData() {
        if (currentLoadTask != null && currentLoadTask.isRunning()) currentLoadTask.cancel();

        LocalDate start = startDatePicker.getValue();
        LocalDate end   = endDatePicker.getValue();
        if (start == null) start = LocalDate.now();
        if (end   == null) end   = LocalDate.now();
        if (start.isAfter(end)) { LocalDate t = start; start = end; end = t; }

        final LocalDate finalStart   = start;
        final LocalDate finalEnd     = end;
        final String    finalInvoice = text(invoiceField);
        final String    finalType    = typeFilter.getValue();

        loadingOverlay.setVisible(true);
        resultLabel.setText("Loading...");
        totalLabel.setText("Grand Total: --");

        currentLoadTask = new Task<>() {
            @Override protected List<ReceiptSummaryRow> call() {
                return service.getSummary(finalStart, finalEnd, finalInvoice, finalType);
            }
            @Override protected void succeeded() {
                loadingOverlay.setVisible(false);
                List<ReceiptSummaryRow> rows = getValue();
                tableView.setItems(FXCollections.observableArrayList(rows));
                resultLabel.setText(rows.size() + " receipt(s)");
                double grand = rows.stream().mapToDouble(ReceiptSummaryRow::getTotal).sum();
                totalLabel.setText("Grand Total: " + df.format(grand));
            }
            @Override protected void failed() {
                loadingOverlay.setVisible(false);
                getException().printStackTrace();
                resultLabel.setText("Error loading data");
                totalLabel.setText("Grand Total: --");
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
    // CSV Export
    // -------------------------------------------------------------------------
    private void exportToCsv(List<ReceiptSummaryRow> rows) {
        if (rows.isEmpty()) { showAlert("No Data", "Nothing to export."); return; }
        try {
            String downloadsPath = System.getProperty("user.home") + File.separator + "Downloads";
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path filePath = Paths.get(downloadsPath, "receipt_summary_" + timestamp + ".csv");

            try (PrintWriter w = new PrintWriter(filePath.toFile(), "UTF-8")) {
                w.write('\uFEFF');
                w.println("Invoice ID,Date,Type,Pax,Subtotal,Discount,Service Charge,Tax,Rounding,Total");
                for (ReceiptSummaryRow r : rows) {
                    w.println(String.join(",",
                        csv(String.valueOf(r.getInvoiceId())),
                        csv(r.getDateFormatted()),
                        csv(r.getType()),
                        csv(String.valueOf(r.getPax())),
                        num(r.getSubtotal()),
                        num(r.getDiscountAmount()),
                        num(r.getServiceChargeAmount()),
                        num(r.getTax1Amount()),
                        num(r.getRoundingAmount()),
                        num(r.getTotal())
                    ));
                }
            }
            showExportSuccess(filePath.toFile());
        } catch (Exception e) { e.printStackTrace(); showAlert("Export Error", e.getMessage()); }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------
    private static DecimalFormat createDecimalFormat() {
        DecimalFormat d = new DecimalFormat("#,###.00");
        d.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.GERMANY));
        return d;
    }

    private String num(double v) {
        if (v == 0.0) return "\"0,00\"";
        return "\"" + df.format(v) + "\"";
    }

    private static String csv(String v) {
        if (v == null) return "";
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

    private static String text(TextField f) {
        return (f == null || f.getText() == null) ? "" : f.getText().trim();
    }

    private void showExportSuccess(File file) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Export Successful"); alert.setHeaderText("Receipt Summary Exported");
        alert.setContentText("Saved to Downloads:\n" + file.getName());
        ButtonType open = new ButtonType("Open File"), folder = new ButtonType("Open Folder"),
                   close = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(open, folder, close);
        alert.showAndWait().ifPresent(b -> {
            try {
                if (b == open)   Desktop.getDesktop().open(file);
                if (b == folder) Desktop.getDesktop().open(file.getParentFile());
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg);
        a.setTitle(title); a.setHeaderText(null); a.showAndWait();
    }
}