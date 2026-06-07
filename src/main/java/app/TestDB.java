package app;

import dao.DatabaseConnection;
import java.sql.Connection;
import java.sql.ResultSet;

public class TestDB {
    public static void main(String[] args) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            System.out.println("--- Users ---");
            ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM users");
            while(rs.next()) System.out.println(rs.getInt("id") + " " + rs.getString("username") + " " + rs.getString("role"));
            
            System.out.println("--- Specializations ---");
            rs = conn.createStatement().executeQuery("SELECT * FROM specializations");
            while(rs.next()) System.out.println(rs.getInt("id") + " " + rs.getString("name"));
            
            System.out.println("--- Doctors ---");
            rs = conn.createStatement().executeQuery("SELECT * FROM doctors");
            while(rs.next()) System.out.println("User ID: " + rs.getInt("user_id") + " Spec ID: " + rs.getInt("specialization_id"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
