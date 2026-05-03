package com.gfs.app.repository;

import com.gfs.app.db.ReportsDatabaseManager;
import com.gfs.app.model.ProductionDetailRow;
import com.gfs.app.model.ProductionSummaryRow;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ProductionRepository {

    // -------------------------------------------------------------------------
    // Summary — mirrors PRODUCTION_SUMMARY_REPORT.jrxml exactly
    // Source: v_production_card view
    // Grouped: category, ItemName, ItemCode, uom, warehouse
    // -------------------------------------------------------------------------
    public List<ProductionSummaryRow> findSummary(LocalDate startDate, LocalDate endDate,
                                                   String categoryFilter, String warehouseFilter,
                                                   String itemFilter) {
        String sql = """
            SELECT
                category,
                name      AS ItemName,
                code      AS ItemCode,
                SUM(quantity) AS Quantity,
                uom,
                warehouse
            FROM v_production_card
            WHERE date >= ? AND date <= ?
              AND (? = '' OR category  LIKE ?)
              AND (? = '' OR warehouse LIKE ?)
              AND (? = '' OR name      LIKE ?)
            GROUP BY category, ItemName, ItemCode, uom, warehouse
            ORDER BY category, ItemName
        """;

        List<ProductionSummaryRow> rows = new ArrayList<>();

        try (Connection conn = ReportsDatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.valueOf(startDate.atStartOfDay()));
            stmt.setTimestamp(2, Timestamp.valueOf(endDate.atTime(23, 59, 59)));

            String cat  = safe(categoryFilter);
            String wh   = safe(warehouseFilter);
            String item = safe(itemFilter);
            stmt.setString(3, cat);  stmt.setString(4, "%" + cat  + "%");
            stmt.setString(5, wh);   stmt.setString(6, "%" + wh   + "%");
            stmt.setString(7, item); stmt.setString(8, "%" + item + "%");

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rows.add(new ProductionSummaryRow(
                        rs.getString("category"),
                        rs.getString("ItemName"),
                        rs.getString("ItemCode"),
                        rs.getDouble("Quantity"),
                        rs.getString("uom"),
                        rs.getString("warehouse")
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rows;
    }

    // -------------------------------------------------------------------------
    // Detail — mirrors PRODUCTION.jrxml exactly
    // Filtered by tbl_productions.id (drill-down from summary or a separate
    // "production records" list)
    //
    // Two-level join:
    //   tbl_production_lines (itemProduct)  — product output lines
    //   tbl_production_lines (itemRecipe)   — ingredient lines (parent_id link)
    // -------------------------------------------------------------------------
    public List<ProductionDetailRow> findDetail(long productionId) {
        String sql = """
            SELECT
                tbl_productions.id,
                tbl_productions.date,
                tbl_warehouses.name              AS Warehouse,
                tbl_items.name                   AS ProductName,
                tbl_categories.name              AS Category,
                itemProduct.quantity             AS ProductQty,
                tbl_items.recipeUom              AS ProductUom,
                itemProduct.description          AS productDescription,
                aa.name                          AS recipeName,
                bb.name                          AS recipeCategory,
                itemRecipe.quantity              AS recipeQty,
                aa.recipeUom                     AS recipeUom,
                itemRecipe.description           AS recipeDescription,
                tbl_productions.notes
            FROM tbl_production_lines AS itemProduct
            INNER JOIN tbl_productions
                ON (itemProduct.production_id = tbl_productions.id)
            INNER JOIN tbl_warehouses
                ON (tbl_productions.warehouse_id = tbl_warehouses.id)
            INNER JOIN tbl_items
                ON (itemProduct.item_id = tbl_items.id)
            INNER JOIN tbl_categories
                ON (tbl_items.category_id = tbl_categories.id)
            INNER JOIN tbl_production_lines AS itemRecipe
                ON (itemProduct.id = itemRecipe.parent_id)
            INNER JOIN tbl_items AS aa
                ON (itemRecipe.item_id = aa.id)
            INNER JOIN tbl_categories AS bb
                ON (aa.category_id = bb.id)
            WHERE tbl_productions.id = ?
            ORDER BY ProductName, recipeName
        """;

        List<ProductionDetailRow> rows = new ArrayList<>();

        try (Connection conn = ReportsDatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, productionId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("date");
                    rows.add(new ProductionDetailRow(
                        rs.getLong("id"),
                        ts != null ? ts.toLocalDateTime() : null,
                        rs.getString("Warehouse"),
                        rs.getString("ProductName"),
                        rs.getString("Category"),
                        rs.getFloat("ProductQty"),
                        rs.getString("ProductUom"),
                        rs.getString("productDescription"),
                        rs.getString("recipeName"),
                        rs.getString("recipeCategory"),
                        rs.getFloat("recipeQty"),
                        rs.getString("recipeUom"),
                        rs.getString("recipeDescription"),
                        rs.getString("notes")
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rows;
    }

    // -------------------------------------------------------------------------
    // Production ID list — for the summary "click to view detail" feature.
    // Returns distinct production IDs + date + warehouse in the date range.
    // -------------------------------------------------------------------------
    public List<ProductionHeaderRow> findHeaders(LocalDate startDate, LocalDate endDate,
                                                  String warehouseFilter) {
        String sql = """
            SELECT
                p.id,
                p.date,
                w.name AS warehouse,
                p.notes
            FROM tbl_productions p
            INNER JOIN tbl_warehouses w ON w.id = p.warehouse_id
            WHERE p.date BETWEEN ? AND ?
              AND (? = '' OR w.name LIKE ?)
            ORDER BY p.date DESC, p.id DESC
        """;

        List<ProductionHeaderRow> rows = new ArrayList<>();

        try (Connection conn = ReportsDatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.valueOf(startDate.atStartOfDay()));
            stmt.setTimestamp(2, Timestamp.valueOf(endDate.atTime(23, 59, 59)));
            String wh = safe(warehouseFilter);
            stmt.setString(3, wh);
            stmt.setString(4, "%" + wh + "%");

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("date");
                    rows.add(new ProductionHeaderRow(
                        rs.getLong("id"),
                        ts != null ? ts.toLocalDateTime() : null,
                        rs.getString("warehouse"),
                        rs.getString("notes")
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rows;
    }

    public List<String> findDistinctWarehouses() {
        List<String> list = new ArrayList<>();
        String sql = "SELECT DISTINCT warehouse FROM v_production_card" +
                     " WHERE warehouse IS NOT NULL ORDER BY warehouse";
        try (Connection conn = ReportsDatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) list.add(rs.getString(1));
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    public List<String> findDistinctCategories() {
        List<String> list = new ArrayList<>();
        String sql = "SELECT DISTINCT category FROM v_production_card" +
                     " WHERE category IS NOT NULL ORDER BY category";
        try (Connection conn = ReportsDatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) list.add(rs.getString(1));
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    private static String safe(String s) { return s != null ? s.trim() : ""; }

    // ── Simple header DTO used by findHeaders() ───────────────────
    public record ProductionHeaderRow(
        long id,
        java.time.LocalDateTime date,
        String warehouse,
        String notes
    ) {
        public String dateFormatted() {
            return date != null
                ? date.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                : "";
        }
    }
}