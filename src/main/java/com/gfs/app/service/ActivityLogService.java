package com.gfs.app.service;

import com.gfs.app.SessionManager;
import com.gfs.app.model.ActivityLog;
import com.gfs.app.repository.ActivityLogRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Facade for reading and writing activity log entries.
 *
 * Usage — write from anywhere in the app:
 * <pre>
 *   ActivityLogService.log(ActivityLogAction.EXPORT_CSV, "Exported warehouse consumption CSV");
 *   ActivityLogService.log(ActivityLogAction.LOGIN, null);
 * </pre>
 */
public class ActivityLogService {

    private static final ActivityLogRepository repository = new ActivityLogRepository();

    // -------------------------------------------------------------------------
    // Write helpers (static — call from any controller)
    // -------------------------------------------------------------------------

    /**
     * Logs an action for the currently logged-in user.
     * Safe to call from any thread — fire-and-forget on a daemon thread.
     */
    public static void log(String action, String description) {
        long   userId   = SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getId() : 0L;
        String username = SessionManager.getCurrentUsername() != null ? SessionManager.getCurrentUsername() : "system";
        logAsync(userId, username, action, description);
    }

    /**
     * Logs an action for a specific user (e.g. LOGIN before session is set).
     */
    public static void log(long userId, String username, String action, String description) {
        logAsync(userId, username, action, description);
    }

    private static void logAsync(long userId, String username, String action, String description) {
        Thread t = new Thread(() -> repository.insert(userId, username, action, description));
        t.setDaemon(true);
        t.start();
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------
    public List<ActivityLog> getAll(LocalDate startDate, LocalDate endDate,
                                    String usernameFilter, String actionFilter) {
        if (startDate == null) startDate = LocalDate.now();
        if (endDate   == null) endDate   = LocalDate.now();
        return repository.findAll(startDate, endDate, usernameFilter, actionFilter);
    }

    public List<String> getDistinctActions() {
        return repository.findDistinctActions();
    }
}