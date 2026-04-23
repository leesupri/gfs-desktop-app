package com.gfs.app.repository;

import com.gfs.app.db.AppDatabaseManager;
import com.gfs.app.model.StaffUser;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AuthRepository {

    public StaffUser findByUsername(String username) {
        String sql = """
                SELECT id, username, name, title, password, is_active
                FROM staff_users
                WHERE username = ?
                LIMIT 1
                """;

        try (Connection conn = AppDatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new StaffUser(
                            rs.getLong("id"),
                            rs.getString("username"),
                            rs.getString("name"),
                            rs.getString("title"),
                            rs.getString("password"),
                            rs.getBoolean("is_active")
                    );
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}