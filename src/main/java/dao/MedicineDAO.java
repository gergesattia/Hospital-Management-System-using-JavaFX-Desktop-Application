package dao;

import models.Medicine;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MedicineDAO {

    public List<Medicine> getAll() {
        List<Medicine> medicines = new ArrayList<>();
        String sql = "SELECT * FROM medicines ORDER BY name ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                medicines.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return medicines;
    }

    public List<Medicine> search(String query) {
        List<Medicine> medicines = new ArrayList<>();
        String sql = "SELECT * FROM medicines WHERE name LIKE ? OR description LIKE ? ORDER BY name ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + query + "%");
            ps.setString(2, "%" + query + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    medicines.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return medicines;
    }

    public void updateStock(int id, int quantityChange) {
        String sql = "UPDATE medicines SET stock_quantity = stock_quantity + ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quantityChange);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deductStockFEFO(String name, int qtyToDeduct) throws SQLException {
        String sql = "SELECT id, stock_quantity FROM medicines WHERE name = ? AND stock_quantity > 0 ORDER BY expiry_date ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                int remaining = qtyToDeduct;
                while (rs.next() && remaining > 0) {
                    int batchId = rs.getInt("id");
                    int batchStock = rs.getInt("stock_quantity");
                    int take = Math.min(batchStock, remaining);
                    
                    try (PreparedStatement psUpd = conn.prepareStatement("UPDATE medicines SET stock_quantity = stock_quantity - ? WHERE id = ?")) {
                        psUpd.setInt(1, take);
                        psUpd.setInt(2, batchId);
                        psUpd.executeUpdate();
                    }
                    remaining -= take;
                }
                
                if (remaining > 0) {
                    throw new SQLException("Insufficient stock for " + name + ". Missing " + remaining + " units.");
                }
            }
        }
    }

    public List<String> getAllNames() {
        List<String> names = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             ResultSet rs = conn.createStatement().executeQuery("SELECT DISTINCT name FROM medicines ORDER BY name ASC")) {
            while (rs.next()) names.add(rs.getString("name"));
        } catch (SQLException e) { e.printStackTrace(); }
        return names;
    }

    public List<Medicine> getAllGrouped() {
        List<Medicine> medicines = new ArrayList<>();
        // Use the price and expiry of the nearest expiry batch
        String sql = "SELECT m1.*, (SELECT SUM(stock_quantity) FROM medicines m2 WHERE m2.name = m1.name) as total_stock " +
                     "FROM medicines m1 " +
                     "WHERE id = (SELECT id FROM medicines m3 WHERE m3.name = m1.name AND stock_quantity > 0 ORDER BY expiry_date ASC LIMIT 1) " +
                     "ORDER BY name ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Medicine m = mapResultSet(rs);
                m.setStockQuantity(rs.getInt("total_stock")); // Override with total stock
                medicines.add(m);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return medicines;
    }

    public List<Medicine> searchGrouped(String query) {
        List<Medicine> medicines = new ArrayList<>();
        String sql = "SELECT m1.*, (SELECT SUM(stock_quantity) FROM medicines m2 WHERE m2.name = m1.name) as total_stock " +
                     "FROM medicines m1 " +
                     "WHERE (name LIKE ? OR description LIKE ?) " +
                     "AND id = (SELECT id FROM medicines m3 WHERE m3.name = m1.name AND stock_quantity > 0 ORDER BY expiry_date ASC LIMIT 1) " +
                     "ORDER BY name ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + query + "%");
            ps.setString(2, "%" + query + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Medicine m = mapResultSet(rs);
                    m.setStockQuantity(rs.getInt("total_stock"));
                    medicines.add(m);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return medicines;
    }

    private Medicine mapResultSet(ResultSet rs) throws SQLException {
        Medicine m = new Medicine();
        m.setId(rs.getInt("id"));
        m.setName(rs.getString("name"));
        m.setDescription(rs.getString("description"));
        m.setStockQuantity(rs.getInt("stock_quantity"));
        m.setMinStockAlert(rs.getInt("min_stock_alert"));
        m.setUnitPrice(rs.getDouble("unit_price"));
        m.setCostPrice(rs.getDouble("cost_price"));
        m.setSupplier(rs.getString("supplier"));
        m.setExpiryDate(rs.getDate("expiry_date") != null ? rs.getDate("expiry_date").toLocalDate() : null);
        return m;
    }
}
