package com.gfs.app.model;

import java.time.LocalDateTime;

/**
 * One line-item row from the Receipt Detail report.
 * Header fields (invoice, table, staff etc.) repeat per line — the
 * controller groups them by invoice_id exactly like the JRXML group band.
 */
public class ReceiptDetailRow {

    // ── Invoice header (repeated per line) ───────────────────────
    private final long          invoiceId;
    private final LocalDateTime created;
    private final LocalDateTime closed;
    private final String        notes;
    private final String        tableName;
    private final double        subtotal;
    private final double        discount;
    private final double        serviceAmount;
    private final double        taxAmount;
    private final double        total;
    private final String        type;
    private final int           printCount;
    private final String        fullName;
    private final int           guest;
    private final String        closedAt;
    private final String        member;

    // ── Line item ────────────────────────────────────────────────
    private final String description;
    private final double quantity;
    private final double price;

    public ReceiptDetailRow(long invoiceId, LocalDateTime created, LocalDateTime closed,
                            String notes, String tableName,
                            double subtotal, double discount, double serviceAmount,
                            double taxAmount, double total, String type, int printCount,
                            String fullName, int guest, String closedAt, String member,
                            String description, double quantity, double price) {
        this.invoiceId     = invoiceId;
        this.created       = created;
        this.closed        = closed;
        this.notes         = notes;
        this.tableName     = tableName;
        this.subtotal      = subtotal;
        this.discount      = discount;
        this.serviceAmount = serviceAmount;
        this.taxAmount     = taxAmount;
        this.total         = total;
        this.type          = type;
        this.printCount    = printCount;
        this.fullName      = fullName;
        this.guest         = guest;
        this.closedAt      = closedAt;
        this.member        = member;
        this.description   = description;
        this.quantity      = quantity;
        this.price         = price;
    }

    public long          getInvoiceId()     { return invoiceId; }
    public LocalDateTime getCreated()       { return created; }
    public LocalDateTime getClosed()        { return closed; }
    public String        getNotes()         { return notes         != null ? notes         : ""; }
    public String        getTableName()     { return tableName     != null ? tableName     : ""; }
    public double        getSubtotal()      { return subtotal; }
    public double        getDiscount()      { return discount; }
    public double        getServiceAmount() { return serviceAmount; }
    public double        getTaxAmount()     { return taxAmount; }
    public double        getTotal()         { return total; }
    public String        getType()          { return type          != null ? type          : ""; }
    public int           getPrintCount()    { return printCount; }
    public String        getFullName()      { return fullName      != null ? fullName      : ""; }
    public int           getGuest()         { return guest; }
    public String        getClosedAt()      { return closedAt      != null ? closedAt      : ""; }
    public String        getMember()        { return member        != null ? member        : ""; }
    public String        getDescription()   { return description   != null ? description   : ""; }
    public double        getQuantity()      { return quantity; }
    public double        getPrice()         { return price; }

    public String getCreatedFormatted() {
        return created != null
            ? created.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            : "";
    }

    public String getClosedFormatted() {
        return closed != null
            ? closed.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            : "";
    }
}