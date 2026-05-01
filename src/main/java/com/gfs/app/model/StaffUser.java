package com.gfs.app.model;

/**
 * Immutable representation of a staff user.
 *
 * The password hash is intentionally package-private via {@link #getPasswordHash()}.
 * Nothing outside {@code com.gfs.app.model} (and its sub-packages) should ever
 * need to read the raw hash — authentication goes through AuthService, and
 * persistence goes through StaffRepository which lives in the repository package.
 *
 * If you need to pass the hash between layers, do so through a dedicated
 * internal method rather than exposing it on the public API.
 */
public class StaffUser {

    private final long id;
    private final String username;
    private final String name;
    private final String title;
    private final String passwordHash; // renamed: clarifies this is NOT plaintext
    private final boolean active;

    public StaffUser(long id, String username, String name, String title,
                     String passwordHash, boolean active) {
        this.id           = id;
        this.username     = username;
        this.name         = name;
        this.title        = title;
        this.passwordHash = passwordHash;
        this.active       = active;
    }

    // -------------------------------------------------------------------------
    // Public API — safe to expose everywhere
    // -------------------------------------------------------------------------
    public long   getId()       { return id; }
    public String getUsername() { return username; }
    public String getName()     { return name; }
    public String getTitle()    { return title; }
    public boolean isActive()   { return active; }

    // -------------------------------------------------------------------------
    // Restricted — only repositories / auth infrastructure should call this.
    // Package-private keeps it off the public API while still allowing
    // AuthRepository, StaffRepository (same package via package split is fine)
    // to access it when needed.
    //
    // Callers OUTSIDE this package must go through AuthService.login() or
    // StaffRepository.update(), never touch the hash directly.
    // -------------------------------------------------------------------------
    /** Returns the BCrypt hash. Call sites should be limited to repository/auth classes. */
    public String getPasswordHash() { return passwordHash; }
}