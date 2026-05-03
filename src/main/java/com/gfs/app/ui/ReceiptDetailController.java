package com.gfs.app.ui;

import com.gfs.app.model.ReceiptDetailRow;
import com.gfs.app.service.ReceiptService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
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
import java.util.List;
import java.util.Locale;

public class ReceiptDetailController {

    // ── Header info labels ───────────────────────────────────────
    @FXML private Label labelInvoiceId;
    @FXML private Label labelDate;
    @FXML private Label labelClosed;
    @FXML private Label labelTable;
    @FXML private Label labelType;
    @FXML private Label labelGuest;
    @FXML private Label labelStaff;
    @FXML private Label labelMember;
    @FXML private Label labelNotes;

    // ── Totals ───────────────────────────────────────────────────
    @FXML private Label labelSubtotal;
    @FXML private Label labelDiscount;
    @FXML private Label labelService;
    @FXML private Label labelTax;
    @FXML private Label labelTotal;

    // ── Line items table ─────────────────────────────────────────
    @FXML private TableView<ReceiptDetailRow>           lineTable;
    @FXML private TableColumn<ReceiptDetailRow, String> colIdx;
    @FXML private TableColumn<ReceiptDetailRow, String> colDescription;
    @FXML private TableColumn<ReceiptDetailRow, String> colQty;
    @FXML private TableColumn<ReceiptDetailRow, String> colPrice;

    @FXML private Label       resultLabel;
    @FXML private StackPane   loadingOverlay;

    private final ReceiptService service = new ReceiptService();
    private final DecimalFormat  df      = createDecimalFormat();
    private long currentInvoiceId;

    @FXML
    public void initialize() {
        colIdx        .setCellValueFactory(d -> new SimpleStringProperty(
            String.valueOf(lineTable.getItems().indexOf(d.getValue()) + 1)));
        colDescription.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDescription()));
        colQty        .setCellValueFactory(d -> new SimpleStringProperty(
            String.format("%.0f", d.getValue().getQuantity())));
        colPrice      .setCellValueFactory(d -> new SimpleStringProperty(df.format(d.getValue().getPrice())));

        // Right-align numeric columns
        colQty  .setStyle("-fx-alignment: CENTER-RIGHT;");
        colPrice.setStyle("-fx-alignment: CENTER-RIGHT;");
    }

    /**
     * Called by ReceiptSummaryController immediately after loading the FXML.
     */
    public void loadInvoice(long invoiceId) {
        this.currentInvoiceId = invoiceId;
        labelInvoiceId.setText("Invoice #" + invoiceId);
        loadingOverlay.setVisible(true);
        resultLabel.setText("Loading...");

        Task<List<ReceiptDetailRow>> task = new Task<>() {
            @Override protected List<ReceiptDetailRow> call() {
                return service.getDetail(invoiceId);
            }
            @Override protected void succeeded() {
                loadingOverlay.setVisible(false);
                List<ReceiptDetailRow> rows = getValue();
                updateUI(rows);
            }
            @Override protected void failed() {
                loadingOverlay.setVisible(false);
                getException().printStackTrace();
                resultLabel.setText("Error loading detail");
            }
        };
        new Thread(task).start();
    }

    private void updateUI(List<ReceiptDetailRow> rows) {
        if (rows.isEmpty()) {
            resultLabel.setText("No lines found.");
            return;
        }

        // Use first row for header info (header fields repeat on every line)
        ReceiptDetailRow h = rows.get(0);
        labelDate   .setText(h.getCreatedFormatted());
        labelClosed .setText(h.getClosedFormatted());
        labelTable  .setText(h.getTableName());
        labelType   .setText(h.getType());
        labelGuest  .setText(String.valueOf(h.getGuest()));
        labelStaff  .setText(h.getFullName());
        labelMember .setText(h.getMember().isBlank()  ? "—" : h.getMember());
        labelNotes  .setText(h.getNotes().isBlank()   ? "—" : h.getNotes());

        labelSubtotal.setText(df.format(h.getSubtotal()));
        labelDiscount.setText(df.format(h.getDiscount()));
        labelService .setText(df.format(h.getServiceAmount()));
        labelTax     .setText(df.format(h.getTaxAmount()));
        labelTotal   .setText(df.format(h.getTotal()));

        lineTable.setItems(FXCollections.observableArrayList(rows));
        resultLabel.setText(rows.size() + " line(s)");
    }

    @FXML
    private void handleExport() {
        if (lineTable.getItems().isEmpty()) { showAlert("No Data", "Nothing to export."); return; }
        try {
            String downloadsPath = System.getProperty("user.home") + File.separator + "Downloads";
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path filePath = Paths.get(downloadsPath,
                "receipt_detail_" + currentInvoiceId + "_" + timestamp + ".csv");

            try (PrintWriter w = new PrintWriter(filePath.toFile(), "UTF-8")) {
                w.write('\uFEFF');
                w.println("Invoice ID,Opened,Closed,Table,Type,Guests,Staff,Member,Description,Qty,Price");
                for (ReceiptDetailRow r : lineTable.getItems()) {
                    w.println(String.join(",",
                        csv(String.valueOf(r.getInvoiceId())),
                        csv(r.getCreatedFormatted()),
                        csv(r.getClosedFormatted()),
                        csv(r.getTableName()),
                        csv(r.getType()),
                        csv(String.valueOf(r.getGuest())),
                        csv(r.getFullName()),
                        csv(r.getMember()),
                        csv(r.getDescription()),
                        csv(String.format("%.0f", r.getQuantity())),
                        num(r.getPrice())
                    ));
                }
            }
            Alert alert = new Alert(Alert.AlertType.INFORMATION,
                "Saved to Downloads:\n" + filePath.getFileName());
            alert.setTitle("Export Successful"); alert.setHeaderText(null);
            ButtonType open = new ButtonType("Open File"),
                       close = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(open, close);
            alert.showAndWait().ifPresent(b -> {
                if (b == open) try { Desktop.getDesktop().open(filePath.toFile()); }
                               catch (Exception ex) { ex.printStackTrace(); }
            });
        } catch (Exception e) { e.printStackTrace(); showAlert("Export Error", e.getMessage()); }
    }

    @FXML
    private void handleClose() {
        ((Stage) lineTable.getScene().getWindow()).close();
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
        return "\"" + (v == 0.0 ? "0,00" : df.format(v)) + "\"";
    }

    private static String csv(String v) {
        if (v == null) return "";
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg);
        a.setTitle(title); a.setHeaderText(null); a.showAndWait();
    }
}