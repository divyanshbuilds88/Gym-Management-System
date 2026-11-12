package com.smartgym.dao.impl;

import com.smartgym.dao.UserDAO;
import com.smartgym.exception.DAOException;
import com.smartgym.model.Role;
import com.smartgym.model.User;
import com.smartgym.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class UserDAOImpl implements UserDAO {

    private static final String SELECT =
            "SELECT user_id, username, password_hash, salt, role, full_name, email, phone, is_active FROM users ";

    @Override
    public int insert(User user) throws DAOException {
        try (Connection con = DatabaseConnection.getConnection()) {
            return insert(con, user);
        } catch (SQLException e) {
            throw new DAOException("Could not connect to the database", e);
        }
    }

    @Override
    public int insert(Connection con, User user) throws DAOException {
        String sql = "INSERT INTO users (username, password_hash, salt, role, full_name, email, phone, is_active) "
                   + "VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getSalt());
            ps.setString(4, user.getRole().name());
            ps.setString(5, user.getFullName());
            ps.setString(6, user.getEmail());
            ps.setString(7, user.getPhone());
            ps.setBoolean(8, user.isActive());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new DAOException("User was saved but no ID was returned");
        } catch (SQLException e) {
            throw new DAOException("Could not save user", e);
        }
    }

    @Override
    public User findByUsername(String username) throws DAOException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT + "WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DAOException("Could not look up user", e);
        }
    }

    @Override
    public User findById(int userId) throws DAOException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT + "WHERE user_id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DAOException("Could not look up user", e);
        }
    }

    @Override
    public boolean usernameExists(String username) throws DAOException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT 1 FROM users WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DAOException("Could not check username", e);
        }
    }

    @Override
    public boolean updateContact(int userId, String fullName, String email, String phone) throws DAOException {
        String sql = "UPDATE users SET full_name = ?, email = ?, phone = ? WHERE user_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, fullName);
            ps.setString(2, email);
            ps.setString(3, phone);
            ps.setInt(4, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DAOException("Could not update user contact details", e);
        }
    }

    @Override
    public boolean updatePassword(int userId, String passwordHash, String salt) throws DAOException {
        String sql = "UPDATE users SET password_hash = ?, salt = ? WHERE user_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setString(2, salt);
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DAOException("Could not change password", e);
        }
    }

    @Override
    public boolean delete(int userId) throws DAOException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM users WHERE user_id = ?")) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DAOException("Could not delete user", e);
        }
    }

    private static User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("user_id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setSalt(rs.getString("salt"));
        u.setRole(Role.fromString(rs.getString("role")));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setPhone(rs.getString("phone"));
        u.setActive(rs.getBoolean("is_active"));
        return u;
    }
}
