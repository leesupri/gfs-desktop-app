package com.gfs.app;

import com.gfs.app.model.StaffUser;

import java.util.HashSet;
import java.util.Set;

public class SessionManager {
    private static StaffUser currentUser;
    private static Set<String> currentPermissions = new HashSet<>();

    public static void login(StaffUser user, Set<String> permissions) {
        currentUser = user;
        currentPermissions = permissions != null ? permissions : new HashSet<>();
    }

    public static void logout() {
        currentUser = null;
        currentPermissions.clear();
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static StaffUser getCurrentUser() {
        return currentUser;
    }

    public static String getCurrentUsername() {
        return currentUser != null ? currentUser.getUsername() : null;
    }

    public static String getCurrentDisplayName() {
        return currentUser != null ? currentUser.getName() : null;
    }

    public static String getCurrentTitle() {
        return currentUser != null ? currentUser.getTitle() : null;
    }

    public static boolean hasPermission(String permissionCode) {
        return currentPermissions.contains(permissionCode);
    }

    public static Set<String> getCurrentPermissions() {
        return currentPermissions;
    }
}