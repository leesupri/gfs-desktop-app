package com.gfs.app.repository;

import com.gfs.app.db.ReportsDatabaseManager;
import com.gfs.app.model.ItemSalesRow;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ItemSalesRepository {

    public List<ItemSalesRow> findAll(LocalDate startDate, LocalDate endDate,
                                      String itemSearch, String departmentFilter, String categoryFilter) {
        String sql = """
            SELECT
                dept.name AS department,
                cat.name AS category,
                item.name AS itemName,
                item.code AS itemCode,
                COALESCE(temp.quantity, 0) AS quantity,
                COALESCE(temp.subtotal, 0) AS subtotal,
                COALESCE(temp.cost, 0) AS cost,
                COALESCE(temp.discount, 0) AS discount,
                COALESCE(temp.serviceCharge, 0) AS serviceCharge,
                COALESCE(temp.tax, 0) AS tax,
                CASE WHEN COALESCE(temp.subtotal, 0) = 0 THEN 0 
                     ELSE (COALESCE(temp.cost, 0) / COALESCE(temp.subtotal, 0)) * 100 
                END AS costPercentage,
                COALESCE(temp.subtotal, 0) - COALESCE(temp.cost, 0) AS profit,
                COALESCE(temp.subtotal, 0) - COALESCE(temp.discount, 0) +
                COALESCE(temp.serviceCharge, 0) + COALESCE(temp.tax, 0) AS total
            FROM tbl_items item
            LEFT JOIN (
                SELECT
                    sl.item_id,
                    SUM(sl.quantity - sl.voidQuantity) AS quantity,
                    SUM((sl.quantity - sl.voidQuantity) * sl.unitPrice) AS subtotal,
                    SUM((sl.quantity - sl.voidQuantity) * sl.unitCost) AS cost,
                    SUM(sl.discountAmount) AS discount,
                    SUM(sl.serviceChargeAmount) AS serviceCharge,
                    SUM(sl.tax1Amount + sl.tax2Amount + sl.tax3Amount) AS tax
                FROM tbl_sales s
                INNER JOIN tbl_invoices inv ON s.invoice_id = inv.id
                INNER JOIN tbl_sales_lines sl ON s.id = sl.sales_id
                WHERE s.date BETWEEN ? AND ?
                  AND s.voidCheck = FALSE
                  AND sl.quantity > 0
                GROUP BY sl.item_id
            ) temp ON item.id = temp.item_id
            LEFT JOIN tbl_categories cat ON item.category_id = cat.id
            LEFT JOIN tbl_departments dept ON cat.department_id = dept.id
            WHERE item.sales = TRUE
              AND COALESCE(temp.quantity, 0) > 0                         
              AND (? = '' OR item.name LIKE CONCAT('%', ?, '%') OR item.code LIKE CONCAT('%', ?, '%'))
              AND (? = '' OR dept.name = ?)
              AND (? = '' OR cat.name = ?)
            ORDER BY dept.name, cat.name, item.name, item.code
        """;

        List<ItemSalesRow> rows = new ArrayList<>();
        try (Connection conn = ReportsDatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            Timestamp startTs = Timestamp.valueOf(startDate.atStartOfDay());
            Timestamp endTs = Timestamp.valueOf(endDate.atTime(23, 59, 59));
            stmt.setTimestamp(1, startTs);
            stmt.setTimestamp(2, endTs);

            String search = (itemSearch == null) ? "" : itemSearch.trim();
            stmt.setString(3, search);
            stmt.setString(4, search);
            stmt.setString(5, search);

            String dept = (departmentFilter == null || departmentFilter.equals("ALL")) ? "" : departmentFilter;
            stmt.setString(6, dept);
            stmt.setString(7, dept);

            String cat = (categoryFilter == null || categoryFilter.equals("ALL")) ? "" : categoryFilter;
            stmt.setString(8, cat);
            stmt.setString(9, cat);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rows.add(new ItemSalesRow(
                        rs.getString("department"),
                        rs.getString("category"),
                        rs.getString("itemName"),
                        rs.getString("itemCode"),
                        rs.getDouble("quantity"),
                        rs.getDouble("subtotal"),
                        rs.getDouble("cost"),
                        rs.getDouble("discount"),
                        rs.getDouble("serviceCharge"),
                        rs.getDouble("tax"),
                        rs.getDouble("profit"),
                        rs.getDouble("total"),
                        rs.getDouble("costPercentage")
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rows;
    }

    // Helper to get distinct departments for filter dropdown
    public List<String> getDepartments() {
        List<String> depts = new ArrayList<>();
        String sql = "SELECT DISTINCT name FROM tbl_departments ORDER BY name";
        try (Connection conn = ReportsDatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) depts.add(rs.getString("name"));
        } catch (Exception e) { e.printStackTrace(); }
        return depts;
    }

    // Helper to get categories for a department (or all if null)
    public List<String> getCategories(String department) {
        List<String> cats = new ArrayList<>();
        String sql = "SELECT DISTINCT c.name FROM tbl_categories c " +
                     "LEFT JOIN tbl_departments d ON c.department_id = d.id " +
                     "WHERE (? = '' OR d.name = ?) ORDER BY c.name";
        try (Connection conn = ReportsDatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            String dept = (department == null || department.equals("ALL")) ? "" : department;
            stmt.setString(1, dept);
            stmt.setString(2, dept);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) cats.add(rs.getString("name"));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return cats;
    }
}