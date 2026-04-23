package com.gfs.app.repository;

import com.gfs.app.db.ReportsDatabaseManager;
import com.gfs.app.model.MarketListRow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class MarketListRepository {

    public List<MarketListRow> findAll(String keyword,
                                       String category,
                                       String activeFilter,
                                       String salesFilter,
                                       String stockedFilter,
                                       String purchasedFilter) {
        String sql = """
                SELECT
                    i.id,
                    i.code,
                    i.name,
                    c.name AS category,
                    i.uom,
                    i.purchaseUom,
                    i.recipeUom,
                    COALESCE(i.purchasePrice, 0) AS purchasePrice,
                    COALESCE(i.averageCost, 0) AS averageCost,
                    CAST(COALESCE(i.active, 0) AS UNSIGNED) AS active_flag,
                    CAST(COALESCE(i.sales, 0) AS UNSIGNED) AS sales_flag,
                    CAST(COALESCE(i.stocked, 0) AS UNSIGNED) AS stocked_flag,
                    CAST(COALESCE(i.purchased, 0) AS UNSIGNED) AS purchased_flag
                FROM tbl_items i
                LEFT JOIN tbl_categories c ON c.id = i.category_id
                WHERE
                    (
                        ? IS NULL OR ? = ''
                        OR i.name LIKE CONCAT('%', ?, '%')
                        OR i.code LIKE CONCAT('%', ?, '%')
                        OR COALESCE(c.name, '') LIKE CONCAT('%', ?, '%')
                    )
                    AND (
                        ? = 'ALL'
                        OR COALESCE(c.name, '') = ?
                    )
                    AND (
                        ? = 'ALL'
                        OR (? = 'ACTIVE' AND CAST(COALESCE(i.active, 0) AS UNSIGNED) = 1)
                        OR (? = 'INACTIVE' AND CAST(COALESCE(i.active, 0) AS UNSIGNED) = 0)
                    )
                    AND (
                        ? = 'ALL'
                        OR (? = 'YES' AND CAST(COALESCE(i.sales, 0) AS UNSIGNED) = 1)
                        OR (? = 'NO' AND CAST(COALESCE(i.sales, 0) AS UNSIGNED) = 0)
                    )
                    AND (
                        ? = 'ALL'
                        OR (? = 'YES' AND CAST(COALESCE(i.stocked, 0) AS UNSIGNED) = 1)
                        OR (? = 'NO' AND CAST(COALESCE(i.stocked, 0) AS UNSIGNED) = 0)
                    )
                    AND (
                        ? = 'ALL'
                        OR (? = 'YES' AND CAST(COALESCE(i.purchased, 0) AS UNSIGNED) = 1)
                        OR (? = 'NO' AND CAST(COALESCE(i.purchased, 0) AS UNSIGNED) = 0)
                    )
                ORDER BY COALESCE(c.name, ''), i.name
                """;

        List<MarketListRow> rows = new ArrayList<>();

        try (Connection conn = ReportsDatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            int i = 1;

            stmt.setString(i++, keyword);
            stmt.setString(i++, keyword);
            stmt.setString(i++, keyword);
            stmt.setString(i++, keyword);
            stmt.setString(i++, keyword);

            stmt.setString(i++, category);
            stmt.setString(i++, category);

            stmt.setString(i++, activeFilter);
            stmt.setString(i++, activeFilter);
            stmt.setString(i++, activeFilter);

            stmt.setString(i++, salesFilter);
            stmt.setString(i++, salesFilter);
            stmt.setString(i++, salesFilter);

            stmt.setString(i++, stockedFilter);
            stmt.setString(i++, stockedFilter);
            stmt.setString(i++, stockedFilter);

            stmt.setString(i++, purchasedFilter);
            stmt.setString(i++, purchasedFilter);
            stmt.setString(i++, purchasedFilter);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rows.add(new MarketListRow(
                            rs.getLong("id"),
                            rs.getString("code"),
                            rs.getString("name"),
                            rs.getString("category"),
                            rs.getString("uom"),
                            rs.getString("purchaseUom"),
                            rs.getString("recipeUom"),
                            rs.getDouble("purchasePrice"),
                            rs.getDouble("averageCost"),
                            rs.getInt("active_flag") == 1,
                            rs.getInt("sales_flag") == 1,
                            rs.getInt("stocked_flag") == 1,
                            rs.getInt("purchased_flag") == 1
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return rows;
    }

    public List<String> getCategories() {
        String sql = """
                SELECT DISTINCT COALESCE(name, '') AS name
                FROM tbl_categories
                WHERE COALESCE(name, '') <> ''
                ORDER BY name
                """;

        List<String> categories = new ArrayList<>();

        try (Connection conn = ReportsDatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                categories.add(rs.getString("name"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return categories;
    }
}