package com.gfs.app.model;

import java.time.LocalDateTime;

/**
 * One row from the v_order_all view.
 * Matches exactly the columns SELECTed in OrderBoardController.php.
 */
public class OrderLineRow {

    private final long          id;
    private final long          invoiceId;
    private final LocalDateTime date;
    private final String        salesType;
    private final String        description;
    private final double        quantity;
    private final double        unitPrice;
    private final double        unitCost;
    private final String        category;
    private final String        department;
    private final String        employee;
    private final double        discountAmount;
    private final LocalDateTime created;
    private final String        closedBy;
    private final LocalDateTime closedTime;
    private final String        closedAt;       // close station
    private final String        createdAtHo;    // order station
    private final String        tableName;
    private final String        customer;

    public OrderLineRow(long id, long invoiceId, LocalDateTime date, String salesType,
                        String description, double quantity, double unitPrice, double unitCost,
                        String category, String department, String employee, double discountAmount,
                        LocalDateTime created, String closedBy, LocalDateTime closedTime,
                        String closedAt, String createdAtHo, String tableName, String customer) {
        this.id             = id;
        this.invoiceId      = invoiceId;
        this.date           = date;
        this.salesType      = salesType;
        this.description    = description;
        this.quantity       = quantity;
        this.unitPrice      = unitPrice;
        this.unitCost       = unitCost;
        this.category       = category;
        this.department     = department;
        this.employee       = employee;
        this.discountAmount = discountAmount;
        this.created        = created;
        this.closedBy       = closedBy;
        this.closedTime     = closedTime;
        this.closedAt       = closedAt;
        this.createdAtHo    = createdAtHo;
        this.tableName      = tableName;
        this.customer       = customer;
    }

    public long          getId()             { return id; }
    public long          getInvoiceId()      { return invoiceId; }
    public LocalDateTime getDate()           { return date; }
    public String        getSalesType()      { return s(salesType); }
    public String        getDescription()    { return s(description); }
    public double        getQuantity()       { return quantity; }
    public double        getUnitPrice()      { return unitPrice; }
    public double        getUnitCost()       { return unitCost; }
    public String        getCategory()       { return s(category); }
    public String        getDepartment()     { return s(department); }
    public String        getEmployee()       { return s(employee); }
    public double        getDiscountAmount() { return discountAmount; }
    public LocalDateTime getCreated()        { return created; }
    public String        getClosedBy()       { return s(closedBy); }
    public LocalDateTime getClosedTime()     { return closedTime; }
    public String        getClosedAt()       { return s(closedAt); }
    public String        getCreatedAtHo()    { return s(createdAtHo); }
    public String        getTableName()      { return s(tableName); }
    public String        getCustomer()       { return s(customer); }

    public double getLineTotal() { return quantity * unitPrice; }

    public String getCreatedFormatted() {
        return fmt(created, "HH:mm:ss");
    }

    public String getDateFormatted() {
        return fmt(date, "yyyy-MM-dd HH:mm");
    }

    private static String s(String v)                    { return v != null ? v : ""; }
    private static String fmt(LocalDateTime dt, String p) {
        return dt != null
            ? dt.format(java.time.format.DateTimeFormatter.ofPattern(p))
            : "";
    }
}