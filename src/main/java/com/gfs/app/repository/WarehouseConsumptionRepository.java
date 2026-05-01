package com.gfs.app.repository;

import com.gfs.app.db.ReportsDatabaseManager;
import com.gfs.app.model.WarehouseConsumptionRow;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class WarehouseConsumptionRepository {

    // YYYYMM integer format used by tbl_sales_consumptions.dateIndex (e.g. 202604)
    private static final DateTimeFormatter DATE_INDEX_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");

    public List<WarehouseConsumptionRow> findAll(LocalDate startDate, LocalDate endDate,
                                                  String warehouseFilter, String itemFilter) {
        String sql = """
            SELECT STRAIGHT_JOIN
                warehouse.name AS warehouse,
                item.name      AS item,
                item.recipeUom AS uom,
                SUM(scl.quantity)                                                                 AS quantity,
                SUM(scl.quantity * (scl.unitCost / NULLIF(item.inventoryToRecipeConversion, 0))) AS totalCost
            FROM tbl_sales_consumptions sc
            INNER JOIN tbl_sales_consumption_lines scl ON scl.sales_consumption_id = sc.id
            INNER JOIN tbl_items                  item ON scl.item_id              = item.id
            INNER JOIN tbl_warehouses          warehouse ON scl.warehouse_id      = warehouse.id
            WHERE sc.dateIndex >= ?
              AND sc.dateIndex <= ?
              AND sc.date BETWEEN ? AND ?
              AND (? = '' OR warehouse.name LIKE ?)
              AND (? = '' OR item.name LIKE ?)
            GROUP BY warehouse.name, item.name
            ORDER BY warehouse.name, item.name
        """;

        List<WarehouseConsumptionRow> rows = new ArrayList<>();

        try (Connection conn = ReportsDatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // dateIndex — YYYYMM integer for fast index scan
            long startIndex = Long.parseLong(startDate.format(DATE_INDEX_FORMAT));
            long endIndex   = Long.parseLong(endDate.format(DATE_INDEX_FORMAT));
            stmt.setLong(1, startIndex);
            stmt.setLong(2, endIndex);

            // date — fine-grained datetime boundary
            Timestamp startTs = Timestamp.valueOf(startDate.atStartOfDay());
            Timestamp endTs   = Timestamp.valueOf(endDate.atTime(23, 59, 59));
            stmt.setTimestamp(3, startTs);
            stmt.setTimestamp(4, endTs);

            // warehouse filter (partial match)
            String wh = (warehouseFilter == null || warehouseFilter.isBlank()) ? "" : warehouseFilter.trim();
            stmt.setString(5, wh);
            stmt.setString(6, "%" + wh + "%");

            // item filter (partial match)
            String it = (itemFilter == null || itemFilter.isBlank()) ? "" : itemFilter.trim();
            stmt.setString(7, it);
            stmt.setString(8, "%" + it + "%");

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