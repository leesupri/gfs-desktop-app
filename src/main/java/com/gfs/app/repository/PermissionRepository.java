package com.gfs.app.repository;

import com.gfs.app.db.AppDatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

public class PermissionRepository {

    public Set<String> findPermissionsByStaffUserId(long staffUserId) {
        String sql = """
                SELECT DISTINCT p.code
                FROM permissions p
                INNER JOIN role_permissions rp ON rp.permission_id = p.id
                INNER JOIN staff_user_roles sur ON sur.role_id = rp.role_id
                WHERE sur.staff_user_id = ?
                """;

        Set<String> permissions = new HashSet<>();

        try (Connection conn = AppDatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, staffUserId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    permissions.add(rs.getString("code"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return permissions;
    }
}