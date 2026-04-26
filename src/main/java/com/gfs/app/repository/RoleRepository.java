package com.gfs.app.repository;

import com.gfs.app.db.AppDatabaseManager;
import com.gfs.app.model.Role;
import com.gfs.app.model.Permission;
import java.sql.*;
import java.util.*;

public class RoleRepository {

    public List<Role> findAll() {
        List<Role> list = new ArrayList<>();
        String sql = "SELECT id, name FROM roles ORDER BY name";
        try (Connection conn = AppDatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) list.add(new Role(rs.getLong("id"), rs.getString("name")));
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    public boolean save(Role role) {
        boolean update = role.getId() > 0;
        String sql = update ? "UPDATE roles SET name=? WHERE id=?" : "INSERT INTO roles (name) VALUES (?)";
        try (Connection conn = AppDatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (update) {
                stmt.setString(1, role.getName());
                stmt.setLong(2, role.getId());
            } else {
                stmt.setString(1, role.getName());
            }
            return stmt.executeUpdate() > 0;
        } catch (Exception e) { return false; }
    }

    public boolean deleteRole(long id) {
        String sql = "DELETE FROM roles WHERE id=?";
        try (Connection conn = AppDatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) { return false; }
    }

    public List<Permission> getAllPermissions() {
        List<Permission> list = new ArrayList<>();
        String sql = "SELECT id, code, description FROM permissions ORDER BY code";
        try (Connection conn = AppDatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) list.add(new Permission(rs.getLong("id"), rs.getString("code"), rs.getString("description")));
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    public List<Permission> getRolePermissions(long roleId) {
        List<Permission> perms = new ArrayList<>();
        String sql = "SELECT p.id, p.code, p.description FROM permissions p " +
                     "INNER JOIN role_permissions rp ON rp.permission_id = p.id WHERE rp.role_id=?";
        try (Connection conn = AppDatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, roleId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) perms.add(new Permission(rs.getLong("id"), rs.getString("code"), rs.getString("description")));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return perms;
    }

    public void updateRolePermissions(long roleId, List<Long> permissionIds) {
        try (Connection conn = AppDatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            String del = "DELETE FROM role_permissions WHERE role_id=?";
            try (PreparedStatement ps = conn.prepareStatement(del)) { ps.setLong(1, roleId); ps.executeUpdate(); }
            String ins = "INSERT INTO role_permissions (role_id, permission_id) VALUES (?,?)";
            try (PreparedStatement ps = conn.prepareStatement(ins)) {
                for (Long pid : permissionIds) {
                    ps.setLong(1, roleId);
                    ps.setLong(2, pid);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            conn.commit();
        } catch (Exception e) { e.printStackTrace(); }
    }
}