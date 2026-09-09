package com.faceattend.dao;

import com.faceattend.model.User;
import com.faceattend.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles all CRUD operations for enrolled users.
 *
 * TODO (Copilot): implement updateUser(User) and add pagination to getAllUsers()
 * once the user list grows large.
 */
public class UserDAO {

    public void addUser(User user) throws SQLException {
        String sql = "INSERT INTO users (user_code, name, class_or_dept, photo_sample_path) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUserCode());
            ps.setString(2, user.getName());
            ps.setString(3, user.getClassOrDept());
            ps.setString(4, user.getPhotoSamplePath());
            ps.executeUpdate();
        }
    }

    public List<User> getAllUsers() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                users.add(new User(
                        rs.getInt("id"),
                        rs.getString("user_code"),
                        rs.getString("name"),
                        rs.getString("class_or_dept"),
                        rs.getString("photo_sample_path")
                ));
            }
        }
        return users;
    }

    public void deleteUser(int id) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
