package com.gfs.app.model;

public class WarehouseConsumptionRow {
    private final String warehouse;
    private final String item;
    private final String uom;
    private final double quantity;
    private final double totalCost;

    public WarehouseConsumptionRow(String warehouse, String item, String uom, double quantity, double totalCost) {
        this.warehouse = warehouse;
        this.item = item;
        this.uom = uom;
        this.quantity = quantity;
        this.totalCost = totalCost;
    }

    public String getWarehouse() { return warehouse; }
    public String getItem() { return item; }
    public String getUom() { return uom; }
    public double getQuantity() { return quantity; }
    public double getTotalCost() { return totalCost; }
}