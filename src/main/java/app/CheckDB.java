package app;

import dao.DatabaseConnection;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class CheckDB {
    public static void main(String[] args) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM users");
            while (rs.next()) {
                System.out.println("User: " + rs.getString("username") + ", Role: " + rs.getString("role"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
