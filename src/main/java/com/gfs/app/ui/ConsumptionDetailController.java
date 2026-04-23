package com.gfs.app.ui;

import com.gfs.app.model.ConsumptionDetailRow;
import com.gfs.app.model.ConsumptionTreeRow;
import com.gfs.app.service.ConsumptionDetailService;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConsumptionDetailController {

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private TextField invoiceField;

    @FXML
    private TextField itemField;

    @FXML
    private TextField warehouseField;

    @FXML
    private Label resultLabel;

    @FXML
    private Label totalLabel;

    @FXML
    private TreeTableView<ConsumptionTreeRow> treeTableView;

    @FXML
    private TreeTableColumn<ConsumptionTreeRow, String> invoiceColumn;

    @FXML
    private TreeTableColumn<ConsumptionTreeRow, String> dateColumn;

    @FXML
    private TreeTableColumn<ConsumptionTreeRow, String> categoryColumn;

    @FXML
    private TreeTableColumn<ConsumptionTreeRow, String> resultDescriptionColumn;

    @FXML
    private TreeTableColumn<ConsumptionTreeRow, String> itemColumn;

    @FXML
    private TreeTableColumn<ConsumptionTreeRow, String> quantityColumn;

    @FXML
    private TreeTableColumn<ConsumptionTreeRow, String> uomColumn;

    @FXML
    private TreeTableColumn<ConsumptionTreeRow, String> unitCostColumn;

    @FXML
    private TreeTableColumn<ConsumptionTreeRow, String> totalCostColumn;

    @FXML
    private TreeTableColumn<ConsumptionTreeRow, String> warehouseColumn;

    private final ConsumptionDetailService service = new ConsumptionDetailService();
    private final DecimalFormat decimalFormat = new DecimalFormat("#,##0.000");

    @FXML
    public void initialize() {
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now());

        invoiceColumn.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getValue().getInvoice()));
        dateColumn.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getValue().getDate()));
        categoryColumn.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getValue().getCategory()));
        resultDescriptionColumn.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getValue().getResultDescription()));
        itemColumn.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getValue().getItem()));
        quantityColumn.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getValue().getQuantity()));
        uomColumn.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getValue().getUom()));
        unitCostColumn.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getValue().getUnitCost()));
        totalCostColumn.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getValue().getTotalCost()));
        warehouseColumn.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getValue().getWarehouse()));

        loadData();
    }

    @FXML
    private void handleSearch() {
        loadData();
    }

    @FXML
    private void handleRefresh() {
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now());
        invoiceField.clear();
        itemField.clear();
        warehouseField.clear();
        loadData();
    }

    private void loadData() {
        List<ConsumptionDetailRow> rows = service.getAll(
                startDatePicker.getValue().toString(),
                endDatePicker.getValue().toString(),
                invoiceField.getText() == null ? "" : invoiceField.getText().trim(),
                itemField.getText() == null ? "" : itemField.getText().trim(),
                warehouseField.getText() == null ? "" : warehouseField.getText().trim()
        );

        TreeItem<ConsumptionTreeRow> root = new TreeItem<>(
                new ConsumptionTreeRow("ROOT", "", "", "", "", "", "", "", "", "", "")
        );

        Map<Long, List<ConsumptionDetailRow>> byInvoice = rows.stream()
                .collect(Collectors.groupingBy(ConsumptionDetailRow::getInvoiceId));

        for (Map.Entry<Long, List<ConsumptionDetailRow>> invoiceEntry : byInvoice.entrySet()) {
            Long invoiceId = invoiceEntry.getKey();
            List<ConsumptionDetailRow> invoiceRows = invoiceEntry.getValue();

            String invoiceDate = invoiceRows.isEmpty() ? "" : nullSafe(invoiceRows.get(0).getDate());
            double invoiceTotal = invoiceRows.stream().mapToDouble(ConsumptionDetailRow::getTotalCost).sum();

            TreeItem<ConsumptionTreeRow> invoiceNode = new TreeItem<>(
                    new ConsumptionTreeRow(
                            "INVOICE",
                            String.valueOf(invoiceId),
                            invoiceDate,
                            "",
                            "",
                            "",
                            "",
                            "",
                            "",
                            decimalFormat.format(invoiceTotal),
                            ""
                    )
            );

            Map<String, List<ConsumptionDetailRow>> byResult = invoiceRows.stream()
                    .collect(Collectors.groupingBy(ConsumptionDetailRow::getResultDescription));

            for (Map.Entry<String, List<ConsumptionDetailRow>> resultEntry : byResult.entrySet()) {
                String resultDescription = resultEntry.getKey();
                List<ConsumptionDetailRow> resultRows = resultEntry.getValue();

                double resultQty = resultRows.isEmpty() ? 0 : resultRows.get(0).getResultQuantity();
                double resultTotal = resultRows.stream().mapToDouble(ConsumptionDetailRow::getTotalCost).sum();

                TreeItem<ConsumptionTreeRow> resultNode = new TreeItem<>(
                        new ConsumptionTreeRow(
                                "RESULT",
                                "",
                                "",
                                "",
                                nullSafe(resultDescription),
                                "",
                                decimalFormat.format(resultQty),
                                "",
                                "",
                                decimalFormat.format(resultTotal),
                                ""
                        )
                );

                for (ConsumptionDetailRow row : resultRows) {
                    TreeItem<ConsumptionTreeRow> itemNode = new TreeItem<>(
                            new ConsumptionTreeRow(
                                    "ITEM",
                                    "",
                                    "",
                                    nullSafe(row.getCategory()),
                                    "",
                                    nullSafe(row.getItem()),
                                    decimalFormat.format(row.getQuantity()),
                                    nullSafe(row.getUom()),
                                    decimalFormat.format(row.getUnitCost()),
                                    decimalFormat.format(row.getTotalCost()),
                                    nullSafe(row.getWarehouse())
                            )
                    );

                    resultNode.getChildren().add(itemNode);
                }

                invoiceNode.getChildren().add(resultNode);
            }

            invoiceNode.setExpanded(true);
            root.getChildren().add(invoiceNode);
        }

        treeTableView.setRoot(root);

        resultLabel.setText(rows.size() + " row(s)");
        double total = rows.stream().mapToDouble(ConsumptionDetailRow::getTotalCost).sum();
        totalLabel.setText("Total Cost: " + decimalFormat.format(total));
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}