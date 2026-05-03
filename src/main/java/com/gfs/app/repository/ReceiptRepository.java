package com.gfs.app.repository;

import com.gfs.app.db.ReportsDatabaseManager;
import com.gfs.app.model.ReceiptDetailRow;
import com.gfs.app.model.ReceiptSummaryRow;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReceiptRepository {

    // -------------------------------------------------------------------------
    // Summary — mirrors Receipt_Summary_Report.jrxml exactly
    // Reads from v_sales view, filtered by date + optional invoice/type search
    // -------------------------------------------------------------------------
    public List<ReceiptSummaryRow> findSummary(LocalDate startDate, LocalDate endDate,
                                                String invoiceFilter, String typeFilter) {
        String sql = """
            SELECT invoice_id, date, type, pax,
                   subtotal, discountAmount, serviceChargeAmount,
                   tax1Amount, roundingAmount, total
            FROM v_sales
            WHERE date >= ? AND date <= ?
              AND (? = '' OR CAST(invoice_id AS CHAR) LIKE ?)
              AND (? = '' OR type = ?)
            ORDER BY invoice_id
        """;

        List<ReceiptSummaryRow> rows = new ArrayList<>();

        try (Connection conn = ReportsDatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            Timestamp startTs = Timestamp.valueOf(startDate.atStartOfDay());
            Timestamp endTs   = Timestamp.valueOf(endDate.atTime(23, 59, 59));
            stmt.setTimestamp(1, startTs);
            stmt.setTimestamp(2, endTs);

            String inv = (invoiceFilter == null || invoiceFilter.isBlank()) ? "" : invoiceFilter.trim();
            stmt.setString(3, inv);
            stmt.setString(4, "%" + inv + "%");

            String type = (typeFilter == null || typeFilter.equals("ALL")) ? "" : typeFilter;
            stmt.setString(5, type);
            stmt.setString(6, type);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("date");
                    rows.add(new ReceiptSummaryRow(
                        rs.getLong("invoice_id"),
                        ts != null ? ts.toLocalDateTime() : null,
                        rs.getString("type"),
                        rs.getInt("pax"),
                        rs.getDouble("subtotal"),
                        rs.getDouble("discountAmount"),
                        rs.getDouble("serviceChargeAmount"),
                        rs.getDouble("tax1Amount"),
                        rs.getDouble("roundingAmount"),
                        rs.getDouble("total")
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rows;
    }

    // -------------------------------------------------------------------------
    // Detail — mirrors Receipt_Detail_Report.jrxml exactly
    // Filtered by a single invoice_id (drill-down from summary)
    // -------------------------------------------------------------------------
    public List<ReceiptDetailRow> findDetail(long invoiceId) {
        String sql = """
            SELECT
                tbl_sales.invoice_id                                        AS id,
                tbl_sales.created,
                tbl_sales.closedTime                                        AS closed,
                tbl_sales.notes,
                tbl_sales.tableName,
                tbl_sales.subtotal,
                tbl_sales.discountAmount                                    AS discount,
                tbl_sales.serviceChargeAmount                               AS serviceAmount,
                tbl_sales.tax1Amount + tbl_sales.tax2Amount
                    + tbl_sales.tax3Amount                                  AS taxAmount,
                tbl_sales.total,
                tbl_sales.type,
                tbl_sales.printCount                                        AS Print,
                tbl_employees.name                                          AS fullName,
                tbl_sales.pax                                               AS guest,
                tbl_sales.closedAt,
                tbl_sales_lines.description,
                tbl_sales_lines.quantity,
                tbl_customers.name                                          AS member,
                (CASE WHEN tbl_sales_lines.type = 1
                        THEN tbl_sales_lines.quantity * tbl_sales_lines.unitPrice
                      WHEN tbl_sales_lines.type = 2
                        THEN tbl_sales_lines.amount
                      WHEN tbl_sales_lines.type = 3
                        THEN tbl_sales_lines.amount - tbl_sales_lines.changeAmount
                 END)                                                       AS price
            FROM tbl_sales_lines
            INNER JOIN tbl_sales
                ON tbl_sales_lines.sales_id = tbl_sales.id
            LEFT JOIN tbl_employees
                ON tbl_sales.closedBy_id = tbl_employees.id
            LEFT JOIN tbl_customers
                ON tbl_sales.customer_id = tbl_customers.id
            WHERE tbl_sales.invoice_id = ?
              AND tbl_sales.voidCheck IS NOT TRUE
            ORDER BY tbl_sales.invoice_id, tbl_sales_lines.idx
        """;

        List<ReceiptDetailRow> rows = new ArrayList<>();

        try (Connection conn = ReportsDatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, invoiceId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Timestamp created = rs.getTimestamp("created");
                    Timestamp closed  = rs.getTimestamp("closed");
                    rows.add(new ReceiptDetailRow(
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
                        rs.getInt("Print"),
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
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rows;
    }

    /** Returns distinct sale types from v_sales for the filter dropdown. */
    public List<String> findDistinctTypes() {
        List<String> types = new ArrayList<>();
        String sql = "SELECT DISTINCT type FROM v_sales WHERE type IS NOT NULL ORDER BY type";
        try (Connection conn = ReportsDatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) types.add(rs.getString("type"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return types;
    }
}