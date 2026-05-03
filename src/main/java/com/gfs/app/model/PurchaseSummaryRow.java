package com.gfs.app.model;

/**
 * One row from the Purchase Summary report.
 *
 * Source: v_purchase_card view
 * SQL:    SELECT category, name, code,
 *                SUM(quantity) AS quantity, uom,
 *                SUM(quantity * unitCost) AS totalCost
 *         FROM v_purchase_card
 *         WHERE date >= ? AND date <= ?
 *         GROUP BY category, name, code, uom
 *         ORDER BY category, name, code, uom
 */
public class PurchaseSummaryRow {

    private final String category;
    private final String name;
    private final String code;
    private final double quantity;
    private final String uom;
    private final double totalCost;

    public PurchaseSummaryRow(String category, String name, String code,
                              double quantity, String uom, double totalCost) {
        this.category  = category;
        this.name      = name;
        this.code      = code;
        this.quantity  = quantity;
        this.uom       = uom;
        this.totalCost = totalCost;
    }

    public String getCategory()  { return s(category); }
    public String getName()      { return s(name); }
    public String getCode()      { return s(code); }
    public double getQuantity()  { return quantity; }
    public String getUom()       { return s(uom); }
    public double getTotalCost() { return totalCost; }

    private static String s(String v) { return v != null ? v : ""; }
}