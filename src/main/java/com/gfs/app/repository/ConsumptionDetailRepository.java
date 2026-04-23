package com.gfs.app.repository;

import com.gfs.app.db.ReportsDatabaseManager;
import com.gfs.app.model.ConsumptionDetailRow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ConsumptionDetailRepository {

    public List<ConsumptionDetailRow> findAll(String startDate,
                                              String endDate,
                                              String invoice,
                                              String item,
                                              String warehouse) {
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
            scl.quantity * (scl.unitCost / NULLIF(item.inventoryToRecipeConversion,0)) AS totalCost,
            warehouse.name AS warehouse
        FROM tbl_sales_consumptions sc
        JOIN tbl_sales_consumption_lines scl ON scl.sales_consumption_id = sc.id
        JOIN tbl_items item ON scl.item_id = item.id
        JOIN tbl_categories cat ON item.category_id = cat.id
        JOIN tbl_warehouses warehouse ON scl.warehouse_id = warehouse.id
        LEFT JOIN tbl_sales sales ON sales.id = sc.salesId
        WHERE sc.dateIndex BETWEEN ? AND ?
        AND sc.date BETWEEN ? AND ?
        AND (? = '' OR sales.invoice_id = ?)
        AND (? = '' OR warehouse.name LIKE ?)
        AND (? = '' OR item.name LIKE ?)
        ORDER BY sales.invoice_id, scl.resultDescription, item.name
        """;

        List<ConsumptionDetailRow> rows = new ArrayList<>();

        try (Connection conn = ReportsDatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            int i = 1;
            int dateIndexFrom = Integer.parseInt(startDate.replace("-", "").substring(0, 6));
            int dateIndexTo   = Integer.parseInt(endDate.replace("-", "").substring(0, 6));

            // dateIndex
stmt.setInt(i++, dateIndexFrom);
stmt.setInt(i++, dateIndexTo);

// date
stmt.setString(i++, startDate + " 00:00:00");
stmt.setString(i++, endDate + " 23:59:59");

// invoice
stmt.setString(i++, invoice);
stmt.setString(i++, invoice);

// warehouse
stmt.setString(i++, warehouse);
stmt.setString(i++, "%" + warehouse + "%");

// item
stmt.setString(i++, item);
stmt.setString(i++, "%" + item + "%");
            
            
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
        }

        return rows;
    }
}