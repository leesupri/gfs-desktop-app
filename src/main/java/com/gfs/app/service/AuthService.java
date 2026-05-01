package com.gfs.app.service;

import com.gfs.app.model.StaffUser;
import com.gfs.app.repository.AuthRepository;
import com.gfs.app.repository.PermissionRepository;
import com.gfs.app.util.PasswordUtil;

import java.util.Set;

public class AuthService {

    private final AuthRepository authRepository = new AuthRepository();
    private final PermissionRepository permissionRepository = new PermissionRepository();

    public AuthResult login(String username, String password) {
        StaffUser user = authRepository.findByUsername(username);

        if (user == null) {
            return null;
        }

        if (!user.isActive()) {
            return null;
        }

        if (!PasswordUtil.verifyPassword(password, user.getPasswordHash())) {
            return null;
        }

        Set<String> permissions = permissionRepository.findPermissionsByStaffUserId(user.getId());

        return new AuthResult(user, permissions);
    }
}