package dao;

import models.Patient;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.sql.Statement;

public class PatientDAO {

    public Patient getPatientByNationalId(String nationalId) {
        String sql = "SELECT * FROM patients WHERE national_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nationalId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Patient p = new Patient();
                    p.setId(rs.getInt("id"));
                    p.setFullName(rs.getString("full_name"));
                    p.setPhone(rs.getString("phone"));
                    p.setNationalId(rs.getString("national_id"));
                    p.setGender(rs.getString("gender"));
                    if (rs.getDate("birth_date") != null) {
                        p.setBirthDate(rs.getDate("birth_date").toLocalDate());
                    }
                    p.setAddress(rs.getString("address"));
                    p.setBookedSpecialization(rs.getString("booked_specialization"));
                    p.setPriorityLevel(rs.getInt("priority_level"));
                    p.setTelegramChatId(rs.getObject("telegram_chat_id") != null ? rs.getLong("telegram_chat_id") : null);
                    return p;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public int insertPatient(Patient p) {
        String sql = "INSERT INTO patients (full_name, phone, national_id, gender, birth_date, address, booked_specialization, priority_level, telegram_chat_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getFullName());
            ps.setString(2, p.getPhone());
            ps.setString(3, p.getNationalId());
            ps.setString(4, p.getGender());
            if (p.getBirthDate() != null) {
                ps.setDate(5, Date.valueOf(p.getBirthDate()));
            } else {
                ps.setNull(5, java.sql.Types.DATE);
            }
            ps.setString(6, p.getAddress());
            ps.setString(7, p.getBookedSpecialization());
            ps.setInt(8, p.getPriorityLevel());
            if (p.getTelegramChatId() != null) {
                ps.setLong(9, p.getTelegramChatId());
            } else {
                ps.setNull(9, java.sql.Types.BIGINT);
            }
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
    
    public void updatePatient(Patient p) {
        String sql = "UPDATE patients SET full_name = ?, phone = ?, booked_specialization = ?, priority_level = ?, telegram_chat_id = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getFullName());
            ps.setString(2, p.getPhone());
            ps.setString(3, p.getBookedSpecialization());
            ps.setInt(4, p.getPriorityLevel());
            if (p.getTelegramChatId() != null) {
                ps.setLong(5, p.getTelegramChatId());
            } else {
                ps.setNull(5, java.sql.Types.BIGINT);
            }
            ps.setInt(6, p.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
