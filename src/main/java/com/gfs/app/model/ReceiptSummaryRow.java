package com.gfs.app.model;

import java.time.LocalDateTime;

/**
 * One row from the Receipt Summary report (v_sales view).
 */
public class ReceiptSummaryRow {

    private final long          invoiceId;
    private final LocalDateTime date;
    private final String        type;
    private final int           pax;
    private final double        subtotal;
    private final double        discountAmount;
    private final double        serviceChargeAmount;
    private final double        tax1Amount;
    private final double        roundingAmount;
    private final double        total;

    public ReceiptSummaryRow(long invoiceId, LocalDateTime date, String type, int pax,
                             double subtotal, double discountAmount, double serviceChargeAmount,
                             double tax1Amount, double roundingAmount, double total) {
        this.invoiceId           = invoiceId;
        this.date                = date;
        this.type                = type;
        this.pax                 = pax;
        this.subtotal            = subtotal;
        this.discountAmount      = discountAmount;
        this.serviceChargeAmount = serviceChargeAmount;
        this.tax1Amount          = tax1Amount;
        this.roundingAmount      = roundingAmount;
        this.total               = total;
    }

    public long          getInvoiceId()           { return invoiceId; }
    public LocalDateTime getDate()                { return date; }
    public String        getDateFormatted() {
        return date != null
            ? date.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            : "";
    }
    public String        getType()                { return type  != null ? type  : ""; }
    public int           getPax()                 { return pax; }
    public double        getSubtotal()            { return subtotal; }
    public double        getDiscountAmount()      { return discountAmount; }
    public double        getServiceChargeAmount() { return serviceChargeAmount; }
    public double        getTax1Amount()          { return tax1Amount; }
    public double        getRoundingAmount()      { return roundingAmount; }
    public double        getTotal()               { return total; }
}