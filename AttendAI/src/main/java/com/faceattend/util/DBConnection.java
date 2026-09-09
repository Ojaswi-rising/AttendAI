package com.faceattend.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Central place to obtain a JDBC connection.
 *
 * Defaults to SQLite (zero setup, good for a college project demo).
 * Switch DB_URL to the MySQL line if you'd rather run against MySQL.
 *
 * TODO (Copilot): read these values from a config/application.properties
 * file instead of hardcoding them, and add connection pooling (HikariCP)
 * if you want this production-grade.
 */
public class DBConnection {

    // ---- SQLite (default) ----
    private static final String DB_URL = "jdbc:sqlite:db/face_attend.db";

    // ---- MySQL (alternative) ----
    // private static final String DB_URL = "jdbc:mysql://localhost:3306/face_attend";
    // private static final String DB_USER = "root";
    // private static final String DB_PASSWORD = "";

    private static Connection connection;

    private DBConnection() {
    }

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL);
        }
        return connection;
    }

    public static void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
