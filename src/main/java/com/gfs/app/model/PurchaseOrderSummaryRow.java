package com.gfs.app.model;

/**
 * One row from PURCHASE_ORDER_SUMMARY_REPORT.jrxml.
 *
 * Source: tbl_purchase_order_lines + tbl_purchase_orders + tbl_items + tbl_categories
 * Note:   No date filter — covers all purchase orders in the system.
 *         Grouped by: Category, ItemName, ItemCode, purchaseUom, unitPrice
 */
public class PurchaseOrderSummaryRow {

    private final String category;
    private final String itemName;
    private final String itemCode;
    private final double quantity;
    private final double totalReceived;
    private final String purchaseUom;
    private final double unitPrice;
    private final double total;

    public PurchaseOrderSummaryRow(String category, String itemName, String itemCode,
                                   double quantity, double totalReceived, String purchaseUom,
                                   double unitPrice, double total) {
        this.category      = category;
        this.itemName      = itemName;
        this.itemCode      = itemCode;
        this.quantity      = quantity;
        this.totalReceived = totalReceived;
        this.purchaseUom   = purchaseUom;
        this.unitPrice     = unitPrice;
        this.total         = total;
    }

    public String getCategory()      { return s(category); }
    public String getItemName()      { return s(itemName); }
    public String getItemCode()      { return s(itemCode); }
    public double getQuantity()      { return quantity; }
    public double getTotalReceived() { return totalReceived; }
    public String getPurchaseUom()   { return s(purchaseUom); }
    public double getUnitPrice()     { return unitPrice; }
    public double getTotal()         { return total; }

    private static String s(String v) { return v != null ? v : ""; }
}