package com.gfs.app.model;

/**
 * One row from No_Sales_Item_Report.jrxml.
 *
 * "No Sales" = tbl_sales WHERE invoice_id IS NULL AND closed = 1 AND voidCheck IS NOT TRUE
 * These are internal/staff consumption sales not tied to a formal invoice.
 *
 * SQL groups by: Department, Category, description
 * Computed fields (mirrors JRXML variables):
 *   total        = netSales - disc + service + tax
 *   profitPercent = profit / (netSales - disc)
 */
public class NoSalesItemRow {

    private final String department;
    private final String category;
    private final String description;
    private final double quantity;
    private final double netSales;
    private final double disc;
    private final double profit;
    private final double cost;
    private final double service;
    private final double tax;

    public NoSalesItemRow(String department, String category, String description,
                          double quantity, double netSales, double disc,
                          double profit, double cost, double service, double tax) {
        this.department  = department;
        this.category    = category;
        this.description = description;
        this.quantity    = quantity;
        this.netSales    = netSales;
        this.disc        = disc;
        this.profit      = profit;
        this.cost        = cost;
        this.service     = service;
        this.tax         = tax;
    }

    public String getDepartment()  { return s(department); }
    public String getCategory()    { return s(category); }
    public String getDescription() { return s(description); }
    public double getQuantity()    { return quantity; }
    public double getNetSales()    { return netSales; }
    public double getDisc()        { return disc; }
    public double getProfit()      { return profit; }
    public double getCost()        { return cost; }
    public double getService()     { return service; }
    public double getTax()         { return tax; }

    /** netSales - disc + service + tax  (mirrors JRXML $V{total}) */
    public double getTotal() { return netSales - disc + service + tax; }

    /** profit / (netSales - disc)  (mirrors JRXML $V{profitPercent}) */
    public double getProfitPercent() {
        double base = netSales - disc;
        return base == 0 ? 0 : profit / base;
    }

    private static String s(String v) { return v != null ? v : ""; }
}