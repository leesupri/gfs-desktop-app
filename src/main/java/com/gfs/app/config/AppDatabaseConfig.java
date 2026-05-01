package com.gfs.app.config;
 
/**
 * App database connection parameters.
 * Credentials are loaded from db.properties — never hardcoded here.
 */
public class AppDatabaseConfig {
 
    public static String getUrl()      { return DatabaseConfig.getAppUrl(); }
    public static String getUsername() { return DatabaseConfig.getAppUsername(); }
    public static String getPassword() { return DatabaseConfig.getAppPassword(); }
 
    private AppDatabaseConfig() {}
}
 