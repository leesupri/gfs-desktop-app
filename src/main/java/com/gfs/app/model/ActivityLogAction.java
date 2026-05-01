package com.gfs.app.model;

/**
 * Standard action codes written to activity_log.action.
 * Use these constants everywhere — never magic strings.
 */
public final class ActivityLogAction {

    // Auth
    public static final String LOGIN          = "LOGIN";
    public static final String LOGOUT         = "LOGOUT";
    public static final String LOGIN_FAILED   = "LOGIN_FAILED";

    // Exports
    public static final String EXPORT_CSV     = "EXPORT_CSV";

    // Staff management
    public static final String STAFF_CREATE   = "STAFF_CREATE";
    public static final String STAFF_UPDATE   = "STAFF_UPDATE";
    public static final String STAFF_DELETE   = "STAFF_DELETE";

    // Security / roles
    public static final String ROLE_CREATE    = "ROLE_CREATE";
    public static final String ROLE_UPDATE    = "ROLE_UPDATE";
    public static final String ROLE_DELETE    = "ROLE_DELETE";
    public static final String PERMS_UPDATE   = "PERMS_UPDATE";

    // Page views (optional — enable if needed)
    public static final String PAGE_VIEW      = "PAGE_VIEW";

    private ActivityLogAction() {}
}