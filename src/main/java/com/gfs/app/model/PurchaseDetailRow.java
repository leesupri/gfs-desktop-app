package com.gfs.app.model;

import java.time.LocalDateTime;

/**
 * One row from PURCHASE_DETAIL_REPORT.jrxml
 * and PURCHASE_DETAIL_REPORT_GROUP_BY_PARTNER.jrxml.
 *
 * The SQL is identical — only ORDER BY differs:
 *   Detail:     ORDER BY Category, ItemName, id
 *   By Partner: ORDER BY Partner, Category, ItemName, id
 *
 * Grouping mode is handled by the service/controller.
 */
public class PurchaseDetailRow {

    private final String        category;
    private final String        itemName;
    private final long          invoiceId;
    private final LocalDateTime date;
    private final double        purchaseQuantity;
    private final String        purchaseUom;
    private final double        purchaseConversion;
    private final double        quantity;       // purchaseQty * conversion
    private final String        uom;
    private final double        unitCost;       // unitPrice / conversion
    private final double        total;          // qty * unitPrice
    private final String        partner;
    private final String        warehouse;
    private final String        createdBy;

    public PurchaseDetailRow(String category, String itemName, long invoiceId,
                             LocalDateTime date, double purchaseQuantity, String purchaseUom,
                             double purchaseConversion, double quantity, String uom,
                             double unitCost, double total, String partner,
                             String warehouse, String createdBy) {
        this.category          = category;
        this.itemName          = itemName;
        this.invoiceId         = invoiceId;
        this.date              = date;
        this.purchaseQuantity  = purchaseQuantity;
        this.purchaseUom       = purchaseUom;
        this.purchaseConversion = purchaseConversion;
        this.quantity          = quantity;
        this.uom               = uom;
        this.unitCost          = unitCost;
        this.total             = total;
        this.partner           = partner;
        this.warehouse         = warehouse;
        this.createdBy         = createdBy;
    }

    public String        getCategory()          { return s(category); }
    public String        getItemName()          { return s(itemName); }
    public long          getInvoiceId()         { return invoiceId; }
    public LocalDateTime getDate()              { return date; }
    public double        getPurchaseQuantity()  { return purchaseQuantity; }
    public String        getPurchaseUom()       { return s(purchaseUom); }
    public double        getPurchaseConversion(){ return purchaseConversion; }
    public double        getQuantity()          { return quantity; }
    public String        getUom()               { return s(uom); }
    public double        getUnitCost()          { return unitCost; }
    public double        getTotal()             { return total; }
    public String        getPartner()           { return s(partner); }
    public String        getWarehouse()         { return s(warehouse); }
    public String        getCreatedBy()         { return s(createdBy); }

    public String getDateFormatted() {
        return date != null
            ? date.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            : "";
    }

    private static String s(String v) { return v != null ? v : ""; }
}