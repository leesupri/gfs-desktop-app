package com.gfs.app;

import com.gfs.app.model.StaffUser;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Holds the authenticated session for the current user.
 *
 * Thread-safety: all state is stored in {@code volatile} fields and the
 * permissions set is wrapped in an unmodifiable view, so concurrent reads
 * from background Task threads are safe without explicit locking.
 *
 * Write operations (login / logout) should only be called from the JavaFX
 * Application Thread, which is the normal usage pattern for this desktop app.
 */
public class SessionManager {

    private static volatile StaffUser currentUser;
    private static volatile Set<String> currentPermissions = Collections.emptySet();

    // -------------------------------------------------------------------------
    // Write operations (call from FX Application Thread only)
    // -------------------------------------------------------------------------

    public static void login(StaffUser user, Set<String> permissions) {
        currentUser        = user;
        currentPermissions = permissions != null
                ? Collections.unmodifiableSet(new HashSet<>(permissions))
                : Collections.emptySet();
    }

    public static void logout() {
        currentUser        = null;
        currentPermissions = Collections.emptySet();
    }

    // -------------------------------------------------------------------------
    // Read operations (safe to call from any thread)
    // -------------------------------------------------------------------------

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static StaffUser getCurrentUser() {
        return currentUser;
    }

    public static String getCurrentUsername() {
        StaffUser u = currentUser;
        return u != null ? u.getUsername() : null;
    }

    public static String getCurrentDisplayName() {
        StaffUser u = currentUser;
        return u != null ? u.getName() : null;
    }

    public static String getCurrentTitle() {
        StaffUser u = currentUser;
        return u != null ? u.getTitle() : null;
    }

    public static boolean hasPermission(String permissionCode) {
        return currentPermissions.contains(permissionCode);
    }

    /** Returns an unmodifiable snapshot of the current permission set. */
    public static Set<String> getCurrentPermissions() {
        return currentPermissions; // already unmodifiable
    }

    private SessionManager() {}
}