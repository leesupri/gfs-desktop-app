package com.gfs.app.repository;

import com.gfs.app.db.ReportsDatabaseManager;
import com.gfs.app.model.WarehouseConsumptionRow;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class WarehouseConsumptionRepository {

    public List<WarehouseConsumptionRow> findAll(LocalDate startDate, LocalDate endDate, String warehouseFilter) {
        String sql = """
            SELECT 
                warehouse.name AS warehouse,
                item.name AS item,
                item.recipeUom AS uom,
                SUM(scl.quantity) AS quantity,
                SUM(scl.quantity * (scl.unitCost / NULLIF(item.inventoryToRecipeConversion, 0))) AS totalCost
            FROM tbl_sales_consumptions sc
            INNER JOIN tbl_sales_consumption_lines scl ON scl.sales_consumption_id = sc.id
            INNER JOIN tbl_items item ON scl.item_id = item.id
            INNER JOIN tbl_warehouses warehouse ON scl.warehouse_id = warehouse.id
            WHERE sc.date BETWEEN ? AND ?
              AND (? = '' OR warehouse.name LIKE ?)
            GROUP BY warehouse.name, item.name
            ORDER BY warehouse.name, item.name
        """;

        List<WarehouseConsumptionRow> rows = new ArrayList<>();

        try (Connection conn = ReportsDatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            Timestamp startTs = Timestamp.valueOf(startDate.atStartOfDay());
            Timestamp endTs = Timestamp.valueOf(endDate.atTime(23, 59, 59));
            stmt.setTimestamp(1, startTs);
            stmt.setTimestamp(2, endTs);

            String filter = (warehouseFilter == null || warehouseFilter.isBlank()) ? "" : warehouseFilter;
            stmt.setString(3, filter);
            stmt.setString(4, "%" + filter + "%");

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rows.add(new WarehouseConsumptionRow(
                        rs.getString("warehouse"),
                        rs.getString("item"),
                        rs.getString("uom"),
                        rs.getDouble("quantity"),
                        rs.getDouble("totalCost")
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rows;
    }
}