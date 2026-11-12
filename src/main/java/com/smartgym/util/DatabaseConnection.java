package com.smartgym.util;

import com.smartgym.config.DbConfig;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * The single place where JDBC connections are created.
 *
 * JDBC process (5 steps) used here:
 *   1. Load the driver            -> Class.forName(...)
 *   2. Open a connection          -> DriverManager.getConnection(...)
 *   3. Create a statement         -> done in the DAO classes
 *   4. Execute query / update     -> done in the DAO classes
 *   5. Close the resources        -> try-with-resources
 *
 * Run this class directly (main method) to test your MySQL configuration.
 */
public final class DatabaseConnection {

    private static final String DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";

    private DatabaseConnection() { }

    /** Opens a NEW connection. The caller must close it (use try-with-resources). */
    public static Connection getConnection() throws SQLException {
        String url = DbConfig.getUrl();
        if (url.isEmpty()) {
            throw new SQLException("Database URL is missing. Check src/main/resources/db.properties");
        }
        try {
            Class.forName(DRIVER_CLASS);   // step 1: load the JDBC driver (Type 4)
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL driver not found. Reload the Maven project (pom.xml).", e);
        }
        return DriverManager.getConnection(url, DbConfig.getUser(), DbConfig.getPassword());
    }

    /** Returns true if a connection can be opened. */
    public static boolean testConnection() {
        try (Connection con = getConnection()) {
            return con != null && !con.isClosed();
        } catch (SQLException e) {
            System.out.println("Connection FAILED: " + e.getMessage());
            return false;
        }
    }

    /** Console test: java com.smartgym.util.DatabaseConnection */
    public static void main(String[] args) {
        System.out.println("=== DatabaseConnection test ===");
        System.out.println("URL  : " + DbConfig.getUrl());
        System.out.println("User : " + DbConfig.getUser());

        try (Connection con = getConnection()) {
            DatabaseMetaData meta = con.getMetaData();
            System.out.println("Connected to : " + meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion());
            System.out.println("Driver       : " + meta.getDriverName() + " " + meta.getDriverVersion());

            // A plain Statement is fine here because the SQL has NO user input
            try (Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM members")) {
                if (rs.next()) {
                    System.out.println("Members in smart_gym_db : " + rs.getInt(1));
                }
            }
            System.out.println("RESULT: SUCCESS - database is ready.");
        } catch (SQLException e) {
            System.out.println("RESULT: FAILED");
            System.out.println("Reason: " + e.getMessage());
            System.out.println("Hint  : check db.user / db.password in db.properties and that MySQL is running.");
        }
    }
}
