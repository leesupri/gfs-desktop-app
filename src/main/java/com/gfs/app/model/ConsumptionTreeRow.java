package com.gfs.app.model;

public class ConsumptionTreeRow {
    private final String type;
    private final String invoice;
    private final String date;
    private final String category;
    private final String resultDescription;
    private final String item;
    private final String quantity;
    private final String uom;
    private final String unitCost;
    private final String totalCost;
    private final String warehouse;

    public ConsumptionTreeRow(String type,
                              String invoice,
                              String date,
                              String category,
                              String resultDescription,
                              String item,
                              String quantity,
                              String uom,
                              String unitCost,
                              String totalCost,
                              String warehouse) {
        this.type = type;
        this.invoice = invoice;
        this.date = date;
        this.category = category;
        this.resultDescription = resultDescription;
        this.item = item;
        this.quantity = quantity;
        this.uom = uom;
        this.unitCost = unitCost;
        this.totalCost = totalCost;
        this.warehouse = warehouse;
    }

    public String getType() {
        return type;
    }

    public String getInvoice() {
        return invoice;
    }

    public String getDate() {
        return date;
    }

    public String getCategory() {
        return category;
    }

    public String getResultDescription() {
        return resultDescription;
    }

    public String getItem() {
        return item;
    }

    public String getQuantity() {
        return quantity;
    }

    public String getUom() {
        return uom;
    }

    public String getUnitCost() {
        return unitCost;
    }

    public String getTotalCost() {
        return totalCost;
    }

    public String getWarehouse() {
        return warehouse;
    }
}