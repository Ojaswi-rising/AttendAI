package com.faceattend.dao;

import com.faceattend.model.AttendanceRecord;
import com.faceattend.util.DBConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles reading/writing attendance records.
 *
 * TODO (Copilot): implement getRecordsByDateRange(LocalDate from, LocalDate to)
 * and getRecordsByClass(String classOrDept) for the Reports module.
 */
public class AttendanceDAO {

    /**
     * Marks attendance, but only if this user hasn't already been marked today
     * (prevents duplicate entries on the same day).
     */
    public boolean markAttendanceIfNotAlready(AttendanceRecord record) throws SQLException {
        if (alreadyMarkedToday(record.getUserId(), record.getDate())) {
            return false;
        }
        String sql = "INSERT INTO attendance (user_id, date, time, confidence, status) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, record.getUserId());
            ps.setString(2, record.getDate().toString());
            ps.setString(3, record.getTime().toString());
            ps.setDouble(4, record.getConfidence());
            ps.setString(5, record.getStatus());
            ps.executeUpdate();
        }
        return true;
    }

    private boolean alreadyMarkedToday(int userId, LocalDate date) throws SQLException {
        String sql = "SELECT COUNT(*) FROM attendance WHERE user_id = ? AND date = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, date.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public List<AttendanceRecord> getRecordsForDate(LocalDate date) throws SQLException {
        List<AttendanceRecord> records = new ArrayList<>();
        String sql = "SELECT a.*, u.name FROM attendance a JOIN users u ON a.user_id = u.id WHERE a.date = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, date.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AttendanceRecord r = new AttendanceRecord();
                    r.setId(rs.getInt("id"));
                    r.setUserId(rs.getInt("user_id"));
                    r.setUserName(rs.getString("name"));
                    r.setDate(LocalDate.parse(rs.getString("date")));
                    r.setConfidence(rs.getDouble("confidence"));
                    r.setStatus(rs.getString("status"));
                    records.add(r);
                }
            }
        }
        return records;
    }
}
