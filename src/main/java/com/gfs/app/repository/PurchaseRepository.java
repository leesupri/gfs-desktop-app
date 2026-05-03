package com.gfs.app.repository;

import com.gfs.app.db.ReportsDatabaseManager;
import com.gfs.app.model.PurchaseDetailRow;
import com.gfs.app.model.PurchaseOrderSummaryRow;
import com.gfs.app.model.PurchaseSummaryRow;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PurchaseRepository {

    // =========================================================================
    // 1. PURCHASE SUMMARY — v_purchase_card, grouped by category
    //    Mirrors PURCHASE_SUMMARY_REPORT.jrxml exactly
    // =========================================================================
    public List<PurchaseSummaryRow> findSummary(LocalDate startDate, LocalDate endDate,
                                                 String categoryFilter, String itemFilter) {
        String sql = """
            SELECT
                category,
                name,
                code,
                SUM(quantity)              AS quantity,
                uom,
                SUM(quantity * unitCost)   AS totalCost
            FROM v_purchase_card
            WHERE date >= ? AND date <= ?
              AND (? = '' OR category LIKE ?)
              AND (? = '' OR name     LIKE ?)
            GROUP BY category, name, code, uom
            ORDER BY category, name, code, uom
        """;

        List<PurchaseSummaryRow> rows = new ArrayList<>();
        try (Connection conn = ReportsDatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.valueOf(startDate.atStartOfDay()));
            stmt.setTimestamp(2, Timestamp.valueOf(endDate.atTime(23, 59, 59)));
            String cat  = safe(categoryFilter);
            String item = safe(itemFilter);
            stmt.setString(3, cat);  stmt.setString(4, "%" + cat  + "%");
            stmt.setString(5, item); stmt.setString(6, "%" + item + "%");

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rows.add(new PurchaseSummaryRow(
                        rs.getString("category"),
                        rs.getString("name"),
                        rs.getString("code"),
                        rs.getDouble("quantity"),
                        rs.getString("uom"),
                        rs.getDouble("totalCost")
                    ));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return rows;
    }

    // =========================================================================
    // 2 & 3. PURCHASE DETAIL — same SQL, different ORDER BY
    //   groupByPartner = false → ORDER BY Category, ItemName, id  (DETAIL report)
    //   groupByPartner = true  → ORDER BY Partner, Category, ItemName, id  (BY PARTNER report)
    //   Mirrors both PURCHASE_DETAIL_REPORT.jrxml and
    //             PURCHASE_DETAIL_REPORT_GROUP_BY_PARTNER.jrxml
    // =========================================================================
    public List<PurchaseDetailRow> findDetail(LocalDate startDate, LocalDate endDate,
                                               String categoryFilter, String partnerFilter,
                                               String warehouseFilter, String itemFilter,
                                               boolean groupByPartner) {
        String orderBy = groupByPartner
            ? "ORDER BY Partner ASC, Category ASC, ItemName ASC, tbl_purchase_invoices.id ASC"
            : "ORDER BY Category ASC, ItemName ASC, tbl_purchase_invoices.id ASC";

        String sql = """
            SELECT
                tbl_categories.name                              AS Category,
                tbl_items.name                                   AS ItemName,
                tbl_purchase_invoices.id,
                tbl_purchase_invoices.date,
                tbl_purchase_invoice_lines.quantity              AS purchaseQuantity,
                tbl_purchase_invoice_lines.purchaseUom,
                tbl_purchase_invoice_lines.purchaseConversion,
                tbl_purchase_invoice_lines.quantity
                    * tbl_purchase_invoice_lines.purchaseConversion  AS quantity,
                tbl_items.uom,
                tbl_purchase_invoice_lines.unitPrice
                    / NULLIF(tbl_purchase_invoice_lines.purchaseConversion, 0) AS unitCost,
                tbl_purchase_invoice_lines.quantity
                    * tbl_purchase_invoice_lines.unitPrice            AS total,
                tbl_partners.name                                AS Partner,
                tbl_warehouses.name                              AS Warehouse,
                tbl_employees.name                               AS CreateBy
            FROM tbl_purchase_invoice_lines
            INNER JOIN tbl_purchase_invoices
                ON tbl_purchase_invoice_lines.purchase_invoice_id = tbl_purchase_invoices.id
            LEFT  JOIN tbl_partners
                ON tbl_purchase_invoices.partner_id = tbl_partners.id
            LEFT  JOIN tbl_warehouses
                ON tbl_purchase_invoices.warehouse_id = tbl_warehouses.id
            INNER JOIN tbl_items
                ON tbl_purchase_invoice_lines.item_id = tbl_items.id
            INNER JOIN tbl_categories
                ON tbl_items.category_id = tbl_categories.id
            LEFT  JOIN tbl_employees
                ON tbl_purchase_invoices.createdBy_id = tbl_employees.id
            WHERE tbl_purchase_invoices.date >= ? AND tbl_purchase_invoices.date <= ?
              AND (? = '' OR tbl_categories.name LIKE ?)
              AND (? = '' OR tbl_partners.name   LIKE ?)
              AND (? = '' OR tbl_warehouses.name LIKE ?)
              AND (? = '' OR tbl_items.name      LIKE ?)
            """ + orderBy;

        List<PurchaseDetailRow> rows = new ArrayList<>();
        try (Connection conn = ReportsDatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.valueOf(startDate.atStartOfDay()));
            stmt.setTimestamp(2, Timestamp.valueOf(endDate.atTime(23, 59, 59)));
            String cat  = safe(categoryFilter);
            String par  = safe(partnerFilter);
            String wh   = safe(warehouseFilter);
            String item = safe(itemFilter);
            stmt.setString(3,  cat);  stmt.setString(4,  "%" + cat  + "%");
            stmt.setString(5,  par);  stmt.setString(6,  "%" + par  + "%");
            stmt.setString(7,  wh);   stmt.setString(8,  "%" + wh   + "%");
            stmt.setString(9,  item); stmt.setString(10, "%" + item + "%");

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("date");
                    rows.add(new PurchaseDetailRow(
                        rs.getString("Category"),
                        rs.getString("ItemName"),
                        rs.getLong("id"),
                        ts != null ? ts.toLocalDateTime() : null,
                        rs.getDouble("purchaseQuantity"),
                        rs.getString("purchaseUom"),
                        rs.getDouble("purchaseConversion"),
                        rs.getDouble("quantity"),
                        rs.getString("uom"),
                        rs.getDouble("unitCost"),
                        rs.getDouble("total"),
                        rs.getString("Partner"),
                        rs.getString("Warehouse"),
                        rs.getString("CreateBy")
                    ));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return rows;
    }

    // =========================================================================
    // 4. PURCHASE ORDER SUMMARY — tbl_purchase_order_lines, no date filter
    //    Mirrors PURCHASE_ORDER_SUMMARY_REPORT.jrxml exactly
    // =========================================================================
    public List<PurchaseOrderSummaryRow> findOrderSummary(String categoryFilter,
                                                           String itemFilter) {
        String sql = """
            SELECT
                tbl_categories.name                              AS Category,
                tbl_items.name                                   AS ItemName,
                tbl_items.code                                   AS ItemCode,
                SUM(tbl_purchase_order_lines.quantity)           AS quantity,
                SUM(tbl_purchase_order_lines.totalReceived)      AS TotalReceive,
                tbl_purchase_order_lines.purchaseUom,
                tbl_purchase_order_lines.unitPrice,
                SUM(tbl_purchase_order_lines.unitPrice
                    * tbl_purchase_order_lines.quantity)          AS total
            FROM tbl_purchase_order_lines
            INNER JOIN tbl_purchase_orders
                ON tbl_purchase_order_lines.purchase_order_id = tbl_purchase_orders.id
            INNER JOIN tbl_items
                ON tbl_purchase_order_lines.item_id = tbl_items.id
            INNER JOIN tbl_categories
                ON tbl_items.category_id = tbl_categories.id
            WHERE (? = '' OR tbl_categories.name LIKE ?)
              AND (? = '' OR tbl_items.name      LIKE ?)
            GROUP BY Category, ItemName, ItemCode,
                     tbl_purchase_order_lines.purchaseUom,
                     tbl_purchase_order_lines.unitPrice
            ORDER BY Category, ItemName, ItemCode
        """;

        List<PurchaseOrderSummaryRow> rows = new ArrayList<>();
        try (Connection conn = ReportsDatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String cat  = safe(categoryFilter);
            String item = safe(itemFilter);
            stmt.setString(1, cat);  stmt.setString(2, "%" + cat  + "%");
            stmt.setString(3, item); stmt.setString(4, "%" + item + "%");

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rows.add(new PurchaseOrderSummaryRow(
                        rs.getString("Category"),
                        rs.getString("ItemName"),
                        rs.getString("ItemCode"),
                        rs.getDouble("quantity"),
                        rs.getDouble("TotalReceive"),
                        rs.getString("purchaseUom"),
                        rs.getDouble("unitPrice"),
                        rs.getDouble("total")
                    ));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return rows;
    }

    // ── Dropdown helpers ──────────────────────────────────────────
    public List<String> findDistinctCategories() {
        return distinct("SELECT DISTINCT category FROM v_purchase_card" +
                        " WHERE category IS NOT NULL ORDER BY category");
    }

    public List<String> findDistinctPartners() {
        return distinct("SELECT DISTINCT name FROM tbl_partners" +
                        " WHERE name IS NOT NULL ORDER BY name");
    }

    public List<String> findDistinctWarehouses() {
        return distinct("SELECT DISTINCT name FROM tbl_warehouses" +
                        " WHERE name IS NOT NULL ORDER BY name");
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