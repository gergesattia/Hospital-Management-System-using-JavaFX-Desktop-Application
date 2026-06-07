package utils;

import dao.DatabaseConnection;
import java.sql.Connection;
import java.sql.Statement;

public class SystemRefresh {
    public static void main(String[] args) {
        System.out.println("Starting System Refresh...");
        try {
            Connection conn = DatabaseConnection.getConnection();
            try (Statement stmt = conn.createStatement()) {
                // Drop the database
                System.out.println("Dropping medicore_db...");
                stmt.execute("DROP DATABASE IF EXISTS medicore_db");
                
                System.out.println("Recreating medicore_db...");
                stmt.execute("CREATE DATABASE medicore_db");
                
                System.out.println("Database reset successful! The schema will be re-applied with the default admin user the next time the app starts.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Refresh failed.");
        }
    }
}
