package dao;

import models.Sale;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SaleDAO {

    public List<Sale> getAll() {
        List<Sale> sales = new ArrayList<>();
        String sql = "SELECT s.*, u.full_name as creator_name " +
                     "FROM sales s " +
                     "LEFT JOIN users u ON s.created_by = u.id " +
                     "ORDER BY s.created_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Sale s = new Sale();
                s.setId(rs.getInt("id"));
                s.setPatientName(rs.getString("patient_name"));
                s.setTotalAmount(rs.getDouble("total_amount"));
                s.setDiscount(rs.getDouble("discount"));
                s.setPaymentMethod(rs.getString("payment_method"));
                s.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                s.setCreatorName(rs.getString("creator_name"));
                sales.add(s);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sales;
    }

    public double getDailyRevenue() {
        String sql = "SELECT SUM(total_amount) FROM sales WHERE DATE(created_at) = CURDATE()";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public double getMonthlyRevenue() {
        String sql = "SELECT SUM(total_amount) FROM sales WHERE MONTH(created_at) = MONTH(CURDATE()) AND YEAR(created_at) = YEAR(CURDATE())";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public double getDailyProfit() {
        String sql = "SELECT SUM(si.quantity * (si.price_at_sale - m.cost_price)) " +
                     "FROM sale_items si " +
                     "JOIN medicines m ON si.medicine_id = m.id " +
                     "JOIN sales s ON si.sale_id = s.id " +
                     "WHERE DATE(s.created_at) = CURDATE()";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
