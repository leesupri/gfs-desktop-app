package com.gfs.app.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads database credentials from db.properties at runtime.
 *
 * Resolution order:
 *   1. db.properties on the classpath (src/main/resources/db.properties — git-ignored)
 *   2. db.properties next to the running JAR  (System.getProperty("user.dir"))
 *
 * If neither is found the application fails fast with a clear error message
 * rather than silently using hardcoded values.
 */
public class DatabaseConfig {

    private static final Properties PROPS = new Properties();

    static {
        // 1. Try classpath first (works for both mvn javafx:run and packaged JAR)
        try (InputStream is = DatabaseConfig.class
                .getResourceAsStream("/db.properties")) {
            if (is != null) {
                PROPS.load(is);
            } else {
                // 2. Try the working directory (useful when running the fat-JAR directly)
                java.nio.file.Path external = java.nio.file.Paths.get(
                        System.getProperty("user.dir"), "db.properties");
                if (java.nio.file.Files.exists(external)) {
                    try (InputStream fis = java.nio.file.Files.newInputStream(external)) {
                        PROPS.load(fis);
                    }
                } else {
                    throw new ExceptionInInitializerError(
                        "db.properties not found on classpath or in working directory.\n" +
                        "Copy db.properties.template to src/main/resources/db.properties " +
                        "and fill in your credentials."
                    );
                }
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    // -------------------------------------------------------------------------
    // App DB (gfs_desktop_app)
    // -------------------------------------------------------------------------
    public static String getAppUrl()      { return require("app.db.url"); }
    public static String getAppUsername() { return require("app.db.username"); }
    public static String getAppPassword() { return require("app.db.password"); }

    // -------------------------------------------------------------------------
    // Reports DB (db_gundaling)
    // -------------------------------------------------------------------------
    public static String getReportsUrl()      { return require("reports.db.url"); }
    public static String getReportsUsername() { return require("reports.db.username"); }
    public static String getReportsPassword() { return require("reports.db.password"); }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------
    private static String require(String key) {
        String value = PROPS.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                "Missing required property '" + key + "' in db.properties");
        }
        return value;
    }

    private DatabaseConfig() {}
}