package dao;

import models.Appointment;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AppointmentDAO {

    public int getBestDoctorIdForSpecialization(String specName) {
        String sql = "SELECT d.id, COUNT(a.id) as queue_len " +
                     "FROM doctors d " +
                     "JOIN specializations s ON d.specialization_id = s.id " +
                     "LEFT JOIN appointments a ON d.id = a.doctor_id AND a.status='waiting' " +
                     "WHERE s.name = ? " +
                     "GROUP BY d.id " +
                     "ORDER BY queue_len ASC " +
                     "LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, specName);
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
    
    public int getQueueNumber(int doctorId, LocalDate apptDate) {
        String sql = "SELECT COUNT(*) FROM appointments WHERE doctor_id = ? AND DATE(appointment_date) = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, doctorId);
            ps.setString(2, apptDate.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) + 1;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 1;
    }
    
    public void insertAppointment(Appointment appt) {
        String sql = "INSERT INTO appointments (patient_id, doctor_id, priority, status, appointment_date, queue_number) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, appt.getPatientId());
            ps.setInt(2, appt.getDoctorId());
            ps.setInt(3, appt.getPriority());
            ps.setString(4, appt.getStatus());
            ps.setTimestamp(5, Timestamp.valueOf(appt.getAppointmentDate()));
            ps.setInt(6, appt.getQueueNumber());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public List<Appointment> getDoctorQueueToday(int doctorId) {
        List<Appointment> list = new ArrayList<>();
        String sql = "SELECT a.id, p.full_name, p.phone, a.priority, a.queue_number, a.status, a.appointment_date " +
                     "FROM appointments a " +
                     "JOIN patients p ON a.patient_id = p.id " +
                     "WHERE a.doctor_id = ? AND DATE(a.appointment_date) = CURDATE() " +
                     "AND a.status IN ('waiting', 'in_progress') " +
                     "ORDER BY a.priority DESC, a.appointment_date ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, doctorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Appointment a = new Appointment();
                    a.setId(rs.getInt("id"));
                    a.setPatientName(rs.getString("full_name"));
                    a.setPatientPhone(rs.getString("phone"));
                    a.setPriority(rs.getInt("priority"));
                    a.setQueueNumber(rs.getInt("queue_number"));
                    a.setStatus(rs.getString("status"));
                    a.setAppointmentDate(rs.getTimestamp("appointment_date").toLocalDateTime());
                    list.add(a);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
    
    public void updateStatus(int appointmentId, String status) {
        String sql = "UPDATE appointments SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, appointmentId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
