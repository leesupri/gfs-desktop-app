package com.gfs.app.repository;

import com.gfs.app.db.AppDatabaseManager;
import com.gfs.app.model.ActivityLog;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogRepository {

    // -------------------------------------------------------------------------
    // Write — called from ActivityLogService
    // -------------------------------------------------------------------------
    public void insert(long userId, String username, String action, String description) {
        String sql = """
            INSERT INTO activity_log (user_id, username, action, description, created_at)
            VALUES (?, ?, ?, ?, NOW())
        """;
        try (Connection conn = AppDatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setString(2, username);
            stmt.setString(3, action);
            stmt.setString(4, description);
            stmt.executeUpdate();
        } catch (Exception e) {
            // Log write must never crash the calling feature
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------------------------
    // Read — called from ActivityLogService
    // -------------------------------------------------------------------------
    public List<ActivityLog> findAll(LocalDate startDate, LocalDate endDate,
                                     String usernameFilter, String actionFilter) {
        String sql = """
            SELECT id, user_id, username, action, description, ip, created_at
            FROM activity_log
            WHERE created_at BETWEEN ? AND ?
              AND (? = '' OR username LIKE ?)
              AND (? = '' OR action   = ?)
            ORDER BY created_at DESC
            LIMIT 2000
        """;

        List<ActivityLog> rows = new ArrayList<>();

        try (Connection conn = AppDatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            Timestamp startTs = Timestamp.valueOf(startDate.atStartOfDay());
            Timestamp endTs   = Timestamp.valueOf(endDate.atTime(23, 59, 59));
            stmt.setTimestamp(1, startTs);
            stmt.setTimestamp(2, endTs);

            String user = (usernameFilter == null || usernameFilter.isBlank()) ? "" : usernameFilter.trim();
            stmt.setString(3, user);
            stmt.setString(4, "%" + user + "%");

            String action = (actionFilter == null || actionFilter.equals("ALL")) ? "" : actionFilter;
            stmt.setString(5, action);
            stmt.setString(6, action);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("created_at");
                    rows.add(new ActivityLog(
                        rs.getLong("id"),
                        rs.getLong("user_id"),
                        rs.getString("username"),
                        rs.getString("action"),
                        rs.getString("description"),
                        rs.getString("ip"),
                        ts != null ? ts.toLocalDateTime() : LocalDateTime.now()
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rows;
    }

    /** Returns the distinct action codes present in the table — for the filter dropdown. */
    public List<String> findDistinctActions() {
        List<String> actions = new ArrayList<>();
        String sql = "SELECT DISTINCT action FROM activity_log ORDER BY action";
        try (Connection conn = AppDatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) actions.add(rs.getString("action"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return actions;
    }
}