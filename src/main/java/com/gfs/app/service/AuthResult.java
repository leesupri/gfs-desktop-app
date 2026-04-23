package com.gfs.app.service;

import com.gfs.app.model.StaffUser;

import java.util.Set;

public class AuthResult {
    private final StaffUser user;
    private final Set<String> permissions;

    public AuthResult(StaffUser user, Set<String> permissions) {
        this.user = user;
        this.permissions = permissions;
    }

    public StaffUser getUser() {
        return user;
    }

    public Set<String> getPermissions() {
        return permissions;
    }
}