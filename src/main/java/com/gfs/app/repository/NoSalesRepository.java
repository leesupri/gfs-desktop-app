package com.gfs.app.repository;

import com.gfs.app.db.ReportsDatabaseManager;
import com.gfs.app.model.NoSalesItemRow;
import com.gfs.app.model.NoSalesReceiptRow;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class NoSalesRepository {

    // =========================================================================
    // Item Summary — mirrors No_Sales_Item_Report.jrxml exactly
    //
    // Key filter: invoice_id IS NULL AND closed=1 AND voidCheck IS NOT TRUE
    //             AND (noReport <> 1 OR unitPrice > 0)
    // =========================================================================
    public List<NoSalesItemRow> findItemSummary(LocalDate startDate, LocalDate endDate,
                                                 String departmentFilter, String categoryFilter,
                                                 String descriptionFilter) {
        String sql = """
            SELECT
                tbl_departments.name                                              AS Department,
                tbl_categories.name                                               AS Category,
                tbl_sales_lines.description,
                SUM(tbl_sales_lines.quantity)                                     AS Quantity,
                SUM(tbl_sales_lines.quantity * tbl_sales_lines.unitPrice)         AS netSales,
                SUM(tbl_sales_lines.discountAmount)                               AS disc,
                SUM((tbl_sales_lines.unitPrice - tbl_sales_lines.unitCost)
                    * tbl_sales_lines.quantity)
                    - SUM(tbl_sales_lines.discountAmount)                         AS profit,
                SUM(tbl_sales_lines.quantity * tbl_sales_lines.unitCost)          AS cost,
                SUM(tbl_sales_lines.serviceChargeAmount)                          AS service,
                SUM(tbl_sales_lines.tax1Amount)                                   AS tax
            FROM tbl_sales_lines
            INNER JOIN tbl_sales
                ON tbl_sales_lines.sales_id = tbl_sales.id
            INNER JOIN tbl_items
                ON tbl_sales_lines.item_id = tbl_items.id
            INNER JOIN tbl_categories
                ON tbl_items.category_id = tbl_categories.id
            INNER JOIN tbl_departments
                ON tbl_categories.department_id = tbl_departments.id
            WHERE tbl_sales.date >= ? AND tbl_sales.date <= ?
              AND tbl_sales.invoice_id IS NULL
              AND tbl_sales.voidCheck IS NOT TRUE
              AND tbl_sales.closed = 1
              AND (tbl_items.noReport <> 1 OR tbl_sales_lines.unitPrice > 0)
              AND (? = '' OR tbl_departments.name LIKE ?)
              AND (? = '' OR tbl_categories.name  LIKE ?)
              AND (? = '' OR tbl_sales_lines.description LIKE ?)
            GROUP BY tbl_departments.name, tbl_categories.name, tbl_sales_lines.description
            ORDER BY tbl_departments.name, tbl_categories.name, tbl_sales_lines.description
        """;

        List<NoSalesItemRow> rows = new ArrayList<>();
        try (Connection conn = ReportsDatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.valueOf(startDate.atStartOfDay()));
            stmt.setTimestamp(2, Timestamp.valueOf(endDate.atTime(23, 59, 59)));
            String dept = safe(departmentFilter);
            String cat  = safe(categoryFilter);
            String desc = safe(descriptionFilter);
            stmt.setString(3, dept); stmt.setString(4, "%" + dept + "%");
            stmt.setString(5, cat);  stmt.setString(6, "%" + cat  + "%");
            stmt.setString(7, desc); stmt.setString(8, "%" + desc + "%");

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rows.add(new NoSalesItemRow(
                        rs.getString("Department"),
                        rs.getString("Category"),
                        rs.getString("description"),
                        rs.getDouble("Quantity"),
                        rs.getDouble("netSales"),
                        rs.getDouble("disc"),
                        rs.getDouble("profit"),
                        rs.getDouble("cost"),
                        rs.getDouble("service"),
                        rs.getDouble("tax")
                    ));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return rows;
    }

    // =========================================================================
    // Receipt Detail — mirrors No_Sales_Receipt_Detail_Report.jrxml exactly
    //
    // Same "No Sales" filter. One row per sales line.
    // ORDER BY tbl_sales.id, tbl_sales_lines.idx
    // =========================================================================
    public List<NoSalesReceiptRow> findReceiptDetail(LocalDate startDate, LocalDate endDate,
                                                      String tableFilter, String staffFilter) {
        String sql = """
            SELECT
                tbl_sales.id,
                tbl_sales.created,
                tbl_sales.closedTime                                          AS closed,
                tbl_sales.notes,
                tbl_sales.tableName,
                tbl_sales.subtotal,
                tbl_sales.discountAmount                                      AS discount,
                tbl_sales.serviceChargeAmount                                 AS serviceAmount,
                tbl_sales.tax1Amount + tbl_sales.tax2Amount
                    + tbl_sales.tax3Amount                                    AS taxAmount,
                tbl_sales.total,
                tbl_sales.type,
                tbl_employees.name                                            AS fullName,
                tbl_sales.pax                                                 AS guest,
                tbl_sales.closedAt,
                tbl_sales_lines.description,
                tbl_sales_lines.quantity,
                tbl_customers.name                                            AS member,
                CASE
                    WHEN tbl_sales_lines.type = 1
                        THEN tbl_sales_lines.quantity * tbl_sales_lines.unitPrice
                    WHEN tbl_sales_lines.type = 2
                        THEN tbl_sales_lines.amount
                    WHEN tbl_sales_lines.type = 3
                        THEN tbl_sales_lines.amount - tbl_sales_lines.changeAmount
                END                                                           AS price
            FROM tbl_sales_lines
            INNER JOIN tbl_sales
                ON tbl_sales_lines.sales_id = tbl_sales.id
            LEFT JOIN tbl_employees
                ON tbl_sales.closedBy_id = tbl_employees.id
            LEFT JOIN tbl_customers
                ON tbl_sales.customer_id = tbl_customers.id
            WHERE tbl_sales.date >= ? AND tbl_sales.date <= ?
              AND tbl_sales.invoice_id IS NULL
              AND tbl_sales.voidCheck IS NOT TRUE
              AND tbl_sales.closed = 1
              AND (? = '' OR tbl_sales.tableName    LIKE ?)
              AND (? = '' OR tbl_employees.name     LIKE ?)
            ORDER BY tbl_sales.id, tbl_sales_lines.idx
        """;

        List<NoSalesReceiptRow> rows = new ArrayList<>();
        try (Connection conn = ReportsDatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.valueOf(startDate.atStartOfDay()));
            stmt.setTimestamp(2, Timestamp.valueOf(endDate.atTime(23, 59, 59)));
            String tbl  = safe(tableFilter);
            String stf  = safe(staffFilter);
            stmt.setString(3, tbl); stmt.setString(4, "%" + tbl + "%");
            stmt.setString(5, stf); stmt.setString(6, "%" + stf + "%");

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Timestamp created = rs.getTimestamp("created");
                    Timestamp closed  = rs.getTimestamp("closed");
                    rows.add(new NoSalesReceiptRow(
                        rs.getLong("id"),
                        created != null ? created.toLocalDateTime() : null,
                        closed  != null ? closed.toLocalDateTime()  : null,
                        rs.getString("notes"),
                        rs.getString("tableName"),
                        rs.getDouble("subtotal"),
                        rs.getDouble("discount"),
                        rs.getDouble("serviceAmount"),
                        rs.getDouble("taxAmount"),
                        rs.getDouble("total"),
                        rs.getString("type"),
                        rs.getString("fullName"),
                        rs.getInt("guest"),
                        rs.getString("closedAt"),
                        rs.getString("member"),
                        rs.getString("description"),
                        rs.getDouble("quantity"),
                        rs.getDouble("price")
                    ));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return rows;
    }

    // ── Dropdown helpers ──────────────────────────────────────────
    public List<String> findDistinctDepartments() {
        return distinct("""
            SELECT DISTINCT d.name FROM tbl_departments d
            INNER JOIN tbl_categories c ON c.department_id = d.id
            INNER JOIN tbl_items i ON i.category_id = c.id
            INNER JOIN tbl_sales_lines sl ON sl.item_id = i.id
            INNER JOIN tbl_sales s ON sl.sales_id = s.id
            WHERE s.invoice_id IS NULL AND s.closed = 1
            ORDER BY d.name
        """);
    }

    public List<String> findDistinctCategories() {
        return distinct("""
            SELECT DISTINCT c.name FROM tbl_categories c
            INNER JOIN tbl_items i ON i.category_id = c.id
            INNER JOIN tbl_sales_lines sl ON sl.item_id = i.id
            INNER JOIN tbl_sales s ON sl.sales_id = s.id
            WHERE s.invoice_id IS NULL AND s.closed = 1
            ORDER BY c.name
        """);
    }

    private List<String> distinct(String sql) {
        List<String> list = new ArrayList<>();
        try (Connection conn = ReportsDatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) list.add(rs.getString(1));
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    private static String safe(String s) { return s != null ? s.trim() : ""; }
}