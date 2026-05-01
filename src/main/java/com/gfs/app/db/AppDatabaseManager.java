package com.gfs.app.db;
 
import com.gfs.app.config.AppDatabaseConfig;
 
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
 
public class AppDatabaseManager {
 
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                AppDatabaseConfig.getUrl(),
                AppDatabaseConfig.getUsername(),
                AppDatabaseConfig.getPassword()
        );
    }
 
    private AppDatabaseManager() {}
}
 