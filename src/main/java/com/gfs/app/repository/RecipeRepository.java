package com.gfs.app.repository;

import com.gfs.app.db.ReportsDatabaseManager;
import com.gfs.app.model.RecipeRow;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RecipeRepository {

    public List<RecipeRow> findAll(String searchKeyword,
                               String salesFilter, String purchasedFilter, 
                               String stockedFilter, String activeFilter) {
    String sql = """
        SELECT
            recipe.id AS recipeId,
            recipe.active AS recipeActive,
            CASE WHEN recipe.sales = TRUE THEN 'yes' ELSE 'no' END AS sales,
            CASE WHEN recipe.purchased = TRUE THEN 'yes' ELSE 'no' END AS purchased,
            CASE WHEN recipe.stocked = TRUE THEN 'yes' ELSE 'no' END AS stocked,
            recipe.name AS recipeName,
            recipe.production AS production,
            recipe.uom AS uom,
            item.id AS itemId,
            item.name AS itemName,
            tbl_recipes.quantity AS RecQty,
            item.recipeUom AS recipeUom,
            (tbl_recipes.quantity / NULLIF(item.inventoryToRecipeConversion, 0)) AS InvQty,
            item.uom AS InvUom,
            COALESCE(item.averageCost, 0) AS unitCost,
            (tbl_recipes.quantity / NULLIF(item.inventoryToRecipeConversion, 0)) * COALESCE(item.averageCost, 0) AS totalCost,
            tbl_recipes.idx AS idx
        FROM tbl_recipes
            JOIN tbl_items item ON tbl_recipes.item_id = item.id
            JOIN tbl_items recipe ON tbl_recipes.recipe_item_id = recipe.id
        WHERE 1=1
            AND ('' = ? OR recipe.name LIKE CONCAT('%', ?, '%') OR item.name LIKE CONCAT('%', ?, '%'))
            AND (? = 'ALL' OR (? = 'YES' AND recipe.sales = TRUE) OR (? = 'NO' AND recipe.sales = FALSE))
            AND (? = 'ALL' OR (? = 'YES' AND recipe.purchased = TRUE) OR (? = 'NO' AND recipe.purchased = FALSE))
            AND (? = 'ALL' OR (? = 'YES' AND recipe.stocked = TRUE) OR (? = 'NO' AND recipe.stocked = FALSE))
            AND (? = 'ALL' OR (? = 'YES' AND recipe.active = 1) OR (? = 'NO' AND recipe.active = 0))
        ORDER BY recipe.id, tbl_recipes.idx
    """;

    List<RecipeRow> rows = new ArrayList<>();
    try (Connection conn = ReportsDatabaseManager.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {

        int idx = 1;
        // Search keyword (3 placeholders)
        stmt.setString(idx++, searchKeyword);
        stmt.setString(idx++, searchKeyword);
        stmt.setString(idx++, searchKeyword);

        // Sales filter (3 placeholders)
        stmt.setString(idx++, salesFilter);
        stmt.setString(idx++, salesFilter);
        stmt.setString(idx++, salesFilter);

        // Purchased filter (3 placeholders)
        stmt.setString(idx++, purchasedFilter);
        stmt.setString(idx++, purchasedFilter);
        stmt.setString(idx++, purchasedFilter);

        // Stocked filter (3 placeholders)
        stmt.setString(idx++, stockedFilter);
        stmt.setString(idx++, stockedFilter);
        stmt.setString(idx++, stockedFilter);

        // Active filter (3 placeholders)
        stmt.setString(idx++, activeFilter);
        stmt.setString(idx++, activeFilter);
        stmt.setString(idx++, activeFilter);

        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                rows.add(new RecipeRow(
                    rs.getLong("recipeId"),
                    rs.getBoolean("recipeActive"),
                    rs.getString("sales"),
                    rs.getString("purchased"),
                    rs.getString("stocked"),
                    rs.getString("recipeName"),
                    rs.getDouble("production"),
                    rs.getString("uom"),
                    rs.getLong("itemId"),
                    rs.getString("itemName"),
                    rs.getDouble("RecQty"),
                    rs.getString("recipeUom"),
                    rs.getDouble("InvQty"),
                    rs.getString("InvUom"),
                    rs.getDouble("unitCost"),
                    rs.getDouble("totalCost"),
                    rs.getInt("idx")
                ));
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
    return rows;
                               }}