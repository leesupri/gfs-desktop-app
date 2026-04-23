package com.gfs.app.repository;

import com.gfs.app.db.ReportsDatabaseManager;
import com.gfs.app.model.ConsumptionDetailRow;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ConsumptionDetailRepository {

    public List<ConsumptionDetailRow> findAll(LocalDate startDate,
                                              LocalDate endDate,
                                              String invoice,
                                              String item,
                                              String warehouse) {
        // SQL mirrors the Jasper report but with NULLIF safety and dynamic filters
        String sql = """
            SELECT
                cat.name AS category,
                item.name AS item,
                sales.invoice_id AS invoice_id,
                sales.date AS date,
                scl.resultDescription,
                scl.resultQuantity,
                scl.quantity,
                item.recipeUom AS uom,
                scl.unitCost,
                scl.quantity * (scl.unitCost / NULLIF(item.inventoryToRecipeConversion, 0)) AS totalCost,
                warehouse.name AS warehouse
            FROM tbl_sales_consumptions sc
            INNER JOIN tbl_sales_consumption_lines scl ON scl.sales_consumption_id = sc.id
            INNER JOIN tbl_items item ON scl.item_id = item.id
            INNER JOIN tbl_categories cat ON item.category_id = cat.id
            INNER JOIN tbl_warehouses warehouse ON scl.warehouse_id = warehouse.id
            LEFT JOIN tbl_sales sales ON sales.id = sc.salesId
            WHERE sc.date BETWEEN ? AND ?
              AND (? = '' OR sales.invoice_id = ?)
              AND (? = '' OR warehouse.name LIKE ?)
              AND (? = '' OR item.name LIKE ?)
            ORDER BY invoice_id, resultDescription, item.name
        """;

        List<ConsumptionDetailRow> rows = new ArrayList<>();

        try (Connection conn = ReportsDatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Date range as Timestamp (from start of day to end of day)
            Timestamp startTimestamp = Timestamp.valueOf(startDate.atStartOfDay());
            Timestamp endTimestamp = Timestamp.valueOf(endDate.atTime(23, 59, 59));
            stmt.setTimestamp(1, startTimestamp);
            stmt.setTimestamp(2, endTimestamp);

            // Invoice filter (exact match)
            String invoiceParam = (invoice == null || invoice.isBlank()) ? "" : invoice;
            stmt.setString(3, invoiceParam);
            stmt.setString(4, invoiceParam);

            // Warehouse filter (partial match)
            String warehouseParam = (warehouse == null || warehouse.isBlank()) ? "" : warehouse;
            stmt.setString(5, warehouseParam);
            stmt.setString(6, "%" + warehouseParam + "%");

            // Item filter (partial match)
            String itemParam = (item == null || item.isBlank()) ? "" : item;
            stmt.setString(7, itemParam);
            stmt.setString(8, "%" + itemParam + "%");

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rows.add(new ConsumptionDetailRow(
                        rs.getLong("invoice_id"),
                        rs.getString("category"),
                        rs.getString("date"),
                        rs.getString("resultDescription"),
                        rs.getDouble("resultQuantity"),
                        rs.getString("item"),
                        rs.getDouble("quantity"),
                        rs.getString("uom"),
                        rs.getDouble("unitCost"),
                        rs.getDouble("totalCost"),
                        rs.getString("warehouse")
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // In production, throw a custom runtime exception
        }
        return rows;
    }
}