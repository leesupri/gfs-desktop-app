package com.gfs.app.repository;

import com.gfs.app.db.ReportsDatabaseManager;
import com.gfs.app.model.OrderLineRow;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Mirrors the query in OrderBoardController.php exactly:
 *   - Source: v_order_all view
 *   - Filters: date range, invoice, table, station, department, category, q (keyword)
 *   - Order: date DESC, invoice_id DESC, created ASC, id ASC
 *
 * Grouping into OrderBillSummary is done in the service layer (as PHP does it
 * in-memory with ->groupBy()).
 */
public class OrderBoardRepository {

    public List<OrderLineRow> findAll(LocalDate startDate, LocalDate endDate,
                                      String invoice, String table, String station,
                                      String department, String category, String q) {

        // Build dynamic WHERE — mirrors Laravel's ->when() chaining
        StringBuilder sql = new StringBuilder("""
            SELECT
                id, invoice_id, date, salesType,
                description, quantity, unitPrice, unitCost,
                category, department, employee, discountAmount,
                created, closedBy, closedTime, closedAt,
                createdAtHo, tableName, customer
            FROM v_order_all
            WHERE date BETWEEN ? AND ?
        """);

        List<Object> params = new ArrayList<>();
        params.add(Timestamp.valueOf(startDate.atStartOfDay()));
        params.add(Timestamp.valueOf(endDate.atTime(23, 59, 59)));

        if (!invoice.isEmpty()) {
            sql.append(" AND invoice_id LIKE ?");
            params.add("%" + invoice + "%");
        }
        if (!table.isEmpty()) {
            sql.append(" AND tableName LIKE ?");
            params.add("%" + table + "%");
        }
        if (!station.isEmpty()) {
            // station matches either order station OR close station — same as PHP
            sql.append(" AND (createdAtHo LIKE ? OR closedAt LIKE ?)");
            params.add("%" + station + "%");
            params.add("%" + station + "%");
        }
        if (!department.isEmpty()) {
            sql.append(" AND department LIKE ?");
            params.add("%" + department + "%");
        }
        if (!category.isEmpty()) {
            sql.append(" AND category LIKE ?");
            params.add("%" + category + "%");
        }
        if (!q.isEmpty()) {
            // keyword search across description, employee, closedBy, customer
            sql.append("""
                 AND (description LIKE ?
                      OR employee  LIKE ?
                      OR closedBy  LIKE ?
                      OR customer  LIKE ?)
            """);
            String like = "%" + q + "%";
            params.add(like); params.add(like); params.add(like); params.add(like);
        }

        sql.append(" ORDER BY date DESC, invoice_id DESC, created ASC, id ASC");

        List<OrderLineRow> rows = new ArrayList<>();

        try (Connection conn = ReportsDatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Timestamp) stmt.setTimestamp(i + 1, (Timestamp) p);
                else                        stmt.setString(i + 1, (String) p);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Timestamp date      = rs.getTimestamp("date");
                    Timestamp created   = rs.getTimestamp("created");
                    Timestamp closedTs  = rs.getTimestamp("closedTime");
                    rows.add(new OrderLineRow(
                        rs.getLong("id"),
                        rs.getLong("invoice_id"),
                        ts(date),
                        rs.getString("salesType"),
                        rs.getString("description"),
                        rs.getDouble("quantity"),
                        rs.getDouble("unitPrice"),
                        rs.getDouble("unitCost"),
                        rs.getString("category"),
                        rs.getString("department"),
                        rs.getString("employee"),
                        rs.getDouble("discountAmount"),
                        ts(created),
                        rs.getString("closedBy"),
                        ts(closedTs),
                        rs.getString("closedAt"),
                        rs.getString("createdAtHo"),
                        rs.getString("tableName"),
                        rs.getString("customer")
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rows;
    }

    /** Returns distinct values for filter dropdowns. */
    public List<String> findDistinct(String column) {
        List<String> values = new ArrayList<>();
        String sql = "SELECT DISTINCT " + column + " FROM v_order_all" +
                     " WHERE " + column + " IS NOT NULL AND " + column + " <> ''" +
                     " ORDER BY " + column;
        try (Connection conn = ReportsDatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) values.add(rs.getString(1));
        } catch (Exception e) { e.printStackTrace(); }
        return values;
    }

    private static LocalDateTime ts(Timestamp t) {
        return t != null ? t.toLocalDateTime() : null;
    }
}