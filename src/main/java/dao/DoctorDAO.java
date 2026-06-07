package dao;

import models.Doctor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DoctorDAO {
    
    public int getDoctorIdByUserId(int userId) {
        String sql = "SELECT id FROM doctors WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public List<Doctor> getAllActiveDoctors() {
        List<Doctor> doctors = new ArrayList<>();
        String sql = "SELECT u.id, u.full_name, s.name as spec, d.room_number, d.consultation_fee, u.phone, d.id as doctor_id " +
                     "FROM users u " +
                     "JOIN doctors d ON u.id = d.user_id " +
                     "LEFT JOIN specializations s ON d.specialization_id = s.id " +
                     "WHERE u.role = 'doctor' AND u.is_active = 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Doctor doc = new Doctor();
                doc.setId(rs.getInt("id"));
                doc.setFullName(rs.getString("full_name"));
                doc.setSpecializationName(rs.getString("spec"));
                doc.setRoomNumber(rs.getString("room_number"));
                doc.setConsultationFee(rs.getDouble("consultation_fee"));
                doc.setPhone(rs.getString("phone"));
                doc.setDoctorId(rs.getInt("doctor_id"));
                doctors.add(doc);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return doctors;
    }

    public void insertDoctor(Doctor doc) {
        String sql = "INSERT INTO doctors (user_id, specialization_id, room_number, consultation_fee) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, doc.getId()); // This is the user_id generated before
            ps.setInt(2, doc.getSpecializationId());
            ps.setString(3, doc.getRoomNumber());
            ps.setDouble(4, doc.getConsultationFee());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
