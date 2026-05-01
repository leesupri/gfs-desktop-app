package com.gfs.app.config;
 
/**
 * Reports database connection parameters.
 * Credentials are loaded from db.properties — never hardcoded here.
 */
public class ReportsDatabaseConfig {
 
    public static String getUrl()      { return DatabaseConfig.getReportsUrl(); }
    public static String getUsername() { return DatabaseConfig.getReportsUsername(); }
    public static String getPassword() { return DatabaseConfig.getReportsPassword(); }
 
    private ReportsDatabaseConfig() {}
}
 