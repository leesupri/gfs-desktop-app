package com.gfs.app.repository;

import com.gfs.app.db.AppDatabaseManager;
import com.gfs.app.model.StaffUser;
import com.gfs.app.model.Role;
import com.gfs.app.util.PasswordUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StaffRepository {

    // Read all staff users
    public List<StaffUser> findAll() {
        List<StaffUser> list = new ArrayList<>();
        String sql = "SELECT id, username, name, title, password, is_active FROM staff_users ORDER BY name";
        try (Connection conn = AppDatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(new StaffUser(
                    rs.getLong("id"),
                    rs.getString("username"),
                    rs.getString("name"),
                    rs.getString("title"),
                    rs.getString("password"),
                    rs.getBoolean("is_active")
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    // Find a single user by ID (used for editing)
    public StaffUser findById(long id) {
        String sql = "SELECT id, username, name, title, password, is_active FROM staff_users WHERE id = ?";
        try (Connection conn = AppDatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
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
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    // Insert a new staff user. Returns the generated ID (or 0 if failed)
    public long insert(StaffUser user, String plainPassword) {
        String sql = "INSERT INTO staff_users (username, name, title, password, is_active) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = AppDatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getName());
            stmt.setString(3, user.getTitle());
            String hash = (plainPassword != null && !plainPassword.isEmpty()) 
                            ? PasswordUtil.hashPassword(plainPassword) 
                            : user.getPasswordHash(); // should already be hashed
            stmt.setString(4, hash);
            stmt.setBoolean(5, user.isActive());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    // Update an existing staff user. Password is updated only if plainPassword is non‑empty.
    public boolean update(StaffUser user, String plainPassword) {
        String sql;
        boolean updatePassword = (plainPassword != null && !plainPassword.isEmpty());
        if (updatePassword) {
            sql = "UPDATE staff_users SET username=?, name=?, title=?, password=?, is_active=? WHERE id=?";
        } else {
            sql = "UPDATE staff_users SET username=?, name=?, title=?, is_active=? WHERE id=?";
        }
        try (Connection conn = AppDatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            int idx = 1;
            stmt.setString(idx++, user.getUsername());
            stmt.setString(idx++, user.getName());
            stmt.setString(idx++, user.getTitle());
            if (updatePassword) {
                stmt.setString(idx++, PasswordUtil.hashPassword(plainPassword));
            }
            stmt.setBoolean(idx++, user.isActive());
            stmt.setLong(idx++, user.getId());
            return stmt.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    // Delete a user by ID
    public boolean delete(long id) {
        String sql = "DELETE FROM staff_users WHERE id=?";
        try (Connection conn = AppDatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) { return false; }
    }

    // Get roles assigned to a user
    public List<Role> getUserRoles(long userId) {
        List<Role> roles = new ArrayList<>();
        String sql = "SELECT r.id, r.name FROM roles r " +
                     "INNER JOIN staff_user_roles sur ON sur.role_id = r.id " +
                     "WHERE sur.staff_user_id = ?";
        try (Connection conn = AppDatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    roles.add(new Role(rs.getLong("id"), rs.getString("name")));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return roles;
    }

    // Replace all roles for a user
    public void updateUserRoles(long userId, List<Long> roleIds) {
        try (Connection conn = AppDatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            // delete existing
            String del = "DELETE FROM staff_user_roles WHERE staff_user_id=?";
            try (PreparedStatement ps = conn.prepareStatement(del)) {
                ps.setLong(1, userId);
                ps.executeUpdate();
            }
            // insert new
            String ins = "INSERT INTO staff_user_roles (staff_user_id, role_id) VALUES (?,?)";
            try (PreparedStatement ps = conn.prepareStatement(ins)) {
                for (Long rid : roleIds) {
                    ps.setLong(1, userId);
                    ps.setLong(2, rid);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            conn.commit();
        } catch (Exception e) { e.printStackTrace(); }
    }
}