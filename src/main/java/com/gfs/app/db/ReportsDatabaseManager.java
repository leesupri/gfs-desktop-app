package com.gfs.app.db;

import com.gfs.app.config.ReportsDatabaseConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ReportsDatabaseManager {

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                ReportsDatabaseConfig.URL,
                ReportsDatabaseConfig.USERNAME,
                ReportsDatabaseConfig.PASSWORD
        );
    }

    private ReportsDatabaseManager() {
    }
}