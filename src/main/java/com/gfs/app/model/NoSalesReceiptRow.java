package com.gfs.app.model;

import java.time.LocalDateTime;

/**
 * One row from No_Sales_Receipt_Detail_Report.jrxml.
 *
 * Same "No Sales" filter: invoice_id IS NULL AND closed=1 AND voidCheck IS NOT TRUE
 * One row per sales line, grouped by tbl_sales.id in the controller.
 *
 * Price expression mirrors JRXML exactly:
 *   type=1 → qty * unitPrice
 *   type=2 → amount
 *   type=3 → amount - changeAmount
 */
public class NoSalesReceiptRow {

    // ── Sales header (repeats per line) ──────────────────────────
    private final long          id;
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
    private final String        fullName;
    private final int           guest;
    private final String        closedAt;
    private final String        member;

    // ── Line item ────────────────────────────────────────────────
    private final String description;
    private final double quantity;
    private final double price;

    public NoSalesReceiptRow(long id, LocalDateTime created, LocalDateTime closed,
                             String notes, String tableName,
                             double subtotal, double discount, double serviceAmount,
                             double taxAmount, double total, String type,
                             String fullName, int guest, String closedAt, String member,
                             String description, double quantity, double price) {
        this.id            = id;
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
        this.fullName      = fullName;
        this.guest         = guest;
        this.closedAt      = closedAt;
        this.member        = member;
        this.description   = description;
        this.quantity      = quantity;
        this.price         = price;
    }

    public long          getId()            { return id; }
    public LocalDateTime getCreated()       { return created; }
    public LocalDateTime getClosed()        { return closed; }
    public String        getNotes()         { return s(notes); }
    public String        getTableName()     { return s(tableName); }
    public double        getSubtotal()      { return subtotal; }
    public double        getDiscount()      { return discount; }
    public double        getServiceAmount() { return serviceAmount; }
    public double        getTaxAmount()     { return taxAmount; }
    public double        getTotal()         { return total; }
    public String        getType()          { return s(type); }
    public String        getFullName()      { return s(fullName); }
    public int           getGuest()         { return guest; }
    public String        getClosedAt()      { return s(closedAt); }
    public String        getMember()        { return s(member); }
    public String        getDescription()   { return s(description); }
    public double        getQuantity()      { return quantity; }
    public double        getPrice()         { return price; }

    public String getCreatedFormatted() { return fmt(created, "yyyy-MM-dd HH:mm:ss"); }
    public String getClosedFormatted()  { return fmt(closed,  "yyyy-MM-dd HH:mm:ss"); }

    private static String s(String v) { return v != null ? v : ""; }
    private static String fmt(LocalDateTime dt, String p) {
        return dt != null
            ? dt.format(java.time.format.DateTimeFormatter.ofPattern(p))
            : "";
    }
}