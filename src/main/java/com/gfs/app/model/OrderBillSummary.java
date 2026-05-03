package com.gfs.app.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * One grouped bill — mirrors the PHP $byBill object produced by
 * OrderBoardController::index().
 *
 * Built from a List<OrderLineRow> that all share the same invoice_id.
 */
public class OrderBillSummary {

    private final long          invoiceId;
    private final LocalDateTime date;
    private final String        salesType;
    private final String        tableName;
    private final String        customer;
    private final String        orderedBy;      // first line's employee
    private final LocalDateTime orderedAt;      // earliest created
    private final String        orderStation;   // createdAtHo
    private final String        closedBy;
    private final LocalDateTime closedTime;
    private final String        closeStation;   // closedAt
    private final double        gross;          // SUM(qty * unitPrice)
    private final double        discount;       // SUM(discountAmount)
    private final double        net;            // gross - discount
    private final int           itemCount;
    private final List<OrderLineRow> lines;

    public OrderBillSummary(long invoiceId, LocalDateTime date, String salesType,
                            String tableName, String customer, String orderedBy,
                            LocalDateTime orderedAt, String orderStation,
                            String closedBy, LocalDateTime closedTime, String closeStation,
                            double gross, double discount,
                            int itemCount, List<OrderLineRow> lines) {
        this.invoiceId    = invoiceId;
        this.date         = date;
        this.salesType    = salesType;
        this.tableName    = tableName;
        this.customer     = customer;
        this.orderedBy    = orderedBy;
        this.orderedAt    = orderedAt;
        this.orderStation = orderStation;
        this.closedBy     = closedBy;
        this.closedTime   = closedTime;
        this.closeStation = closeStation;
        this.gross        = gross;
        this.discount     = discount;
        this.net          = gross - discount;
        this.itemCount    = itemCount;
        this.lines        = lines;
    }

    public long          getInvoiceId()    { return invoiceId; }
    public LocalDateTime getDate()         { return date; }
    public String        getSalesType()    { return s(salesType); }
    public String        getTableName()    { return s(tableName); }
    public String        getCustomer()     { return s(customer); }
    public String        getOrderedBy()    { return s(orderedBy); }
    public LocalDateTime getOrderedAt()    { return orderedAt; }
    public String        getOrderStation() { return s(orderStation); }
    public String        getClosedBy()     { return s(closedBy); }
    public LocalDateTime getClosedTime()   { return closedTime; }
    public String        getCloseStation() { return s(closeStation); }
    public double        getGross()        { return gross; }
    public double        getDiscount()     { return discount; }
    public double        getNet()          { return net; }
    public int           getItemCount()    { return itemCount; }
    public List<OrderLineRow> getLines()   { return lines; }

    public String getDateFormatted() {
        return fmt(date, "yyyy-MM-dd HH:mm");
    }

    public String getOrderedAtFormatted() {
        return fmt(orderedAt, "HH:mm:ss");
    }

    public String getClosedTimeFormatted() {
        return fmt(closedTime, "HH:mm:ss");
    }

    private static String s(String v)                    { return v != null ? v : ""; }
    private static String fmt(LocalDateTime dt, String p) {
        return dt != null
            ? dt.format(java.time.format.DateTimeFormatter.ofPattern(p))
            : "";
    }
}