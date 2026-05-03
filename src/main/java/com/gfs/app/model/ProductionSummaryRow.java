package com.gfs.app.model;

/**
 * One row from the Production Summary report.
 *
 * Source: v_production_card view
 * SQL:    SELECT category, name AS ItemName, code AS ItemCode,
 *                SUM(quantity) AS Quantity, uom, warehouse
 *         FROM v_production_card
 *         WHERE date BETWEEN ? AND ?
 *         GROUP BY category, ItemName, ItemCode, uom, warehouse
 */
public class ProductionSummaryRow {

    private final String category;
    private final String itemName;
    private final String itemCode;
    private final double quantity;
    private final String uom;
    private final String warehouse;

    public ProductionSummaryRow(String category, String itemName, String itemCode,
                                double quantity, String uom, String warehouse) {
        this.category  = category;
        this.itemName  = itemName;
        this.itemCode  = itemCode;
        this.quantity  = quantity;
        this.uom       = uom;
        this.warehouse = warehouse;
    }

    public String getCategory()  { return s(category); }
    public String getItemName()  { return s(itemName); }
    public String getItemCode()  { return s(itemCode); }
    public double getQuantity()  { return quantity; }
    public String getUom()       { return s(uom); }
    public String getWarehouse() { return s(warehouse); }

    private static String s(String v) { return v != null ? v : ""; }
}