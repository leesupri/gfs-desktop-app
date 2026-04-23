package com.gfs.app.model;

public class ConsumptionDetailRow {

    private final long invoiceId;
    private final String category;
    private final String date;
    private final String resultDescription;
    private final double resultQuantity;
    private final String item;
    private final double quantity;
    private final String uom;
    private final double unitCost;
    private final double totalCost;
    private final String warehouse;

    public ConsumptionDetailRow(long invoiceId,
                                String category,
                                String date,
                                String resultDescription,
                                double resultQuantity,
                                String item,
                                double quantity,
                                String uom,
                                double unitCost,
                                double totalCost,
                                String warehouse) {

        this.invoiceId = invoiceId;
        this.category = category;
        this.date = date;
        this.resultDescription = resultDescription;
        this.resultQuantity = resultQuantity;
        this.item = item;
        this.quantity = quantity;
        this.uom = uom;
        this.unitCost = unitCost;
        this.totalCost = totalCost;
        this.warehouse = warehouse;
    }

    public long getInvoiceId() {
        return invoiceId;
    }

    public String getCategory() {
        return category;
    }

    public String getDate() {
        return date;
    }

    public String getResultDescription() {
        return resultDescription;
    }

    public double getResultQuantity() {
        return resultQuantity;
    }

    public String getItem() {
        return item;
    }

    public double getQuantity() {
        return quantity;
    }

    public String getUom() {
        return uom;
    }

    public double getUnitCost() {
        return unitCost;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public String getWarehouse() {
        return warehouse;
    }
}