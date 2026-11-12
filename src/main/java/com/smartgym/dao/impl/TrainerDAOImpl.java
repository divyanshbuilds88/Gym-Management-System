package com.smartgym.dao.impl;

import com.smartgym.dao.TrainerDAO;
import com.smartgym.exception.DAOException;
import com.smartgym.model.Trainer;
import com.smartgym.util.DatabaseConnection;
import com.smartgym.util.DbUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class TrainerDAOImpl implements TrainerDAO {

    private static final String SELECT =
            "SELECT trainer_id, user_id, full_name, specialization, phone, email, availability, salary, experience_years "
          + "FROM trainers ";

    @Override
    public int insert(Trainer trainer) throws DAOException {
        try (Connection con = DatabaseConnection.getConnection()) {
            return insert(con, trainer);
        } catch (SQLException e) {
            throw new DAOException("Could not connect to the database", e);
        }
    }

    @Override
    public int insert(Connection con, Trainer t) throws DAOException {
        String sql = "INSERT INTO trainers (user_id, full_name, specialization, phone, email, availability, salary, experience_years) "
                   + "VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            DbUtil.setNullableInt(ps, 1, t.getUserId());
            ps.setString(2, t.getFullName());
            ps.setString(3, t.getSpecialization());
            ps.setString(4, t.getPhone());
            ps.setString(5, t.getEmail());
            ps.setString(6, t.getAvailability());
            ps.setDouble(7, t.getSalary());
            ps.setInt(8, t.getExperienceYears());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new DAOException("Trainer was saved but no ID was returned");
        } catch (SQLException e) {
            throw new DAOException("Could not add trainer", e);
        }
    }

    @Override
    public boolean update(Trainer t) throws DAOException {
        String sql = "UPDATE trainers SET full_name = ?, specialization = ?, phone = ?, email = ?, "
                   + "availability = ?, salary = ?, experience_years = ? WHERE trainer_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, t.getFullName());
            ps.setString(2, t.getSpecialization());
            ps.setString(3, t.getPhone());
            ps.setString(4, t.getEmail());
            ps.setString(5, t.getAvailability());
            ps.setDouble(6, t.getSalary());
            ps.setInt(7, t.getExperienceYears());
            ps.setInt(8, t.getTrainerId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DAOException("Could not update trainer", e);
        }
    }

    /** Deletes the trainer and the trainer's login in one transaction. */
    @Override
    public boolean delete(int trainerId) throws DAOException {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);

            Integer userId = null;
            try (PreparedStatement ps = con.prepareStatement("SELECT user_id FROM trainers WHERE trainer_id = ?")) {
                ps.setInt(1, trainerId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        con.rollback();
                        return false;
                    }
                    userId = DbUtil.getNullableInt(rs, "user_id");
                }
            }
            boolean deleted;
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM trainers WHERE trainer_id = ?")) {
                ps.setInt(1, trainerId);
                deleted = ps.executeUpdate() > 0;
            }
            if (userId != null) {
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM users WHERE user_id = ?")) {
                    ps.setInt(1, userId);
                    ps.executeUpdate();
                }
            }
            con.commit();
            return deleted;
        } catch (SQLException e) {
            DbUtil.rollbackQuietly(con);
            throw new DAOException("Could not delete trainer", e);
        } finally {
            DbUtil.closeQuietly(con);
        }
    }

    @Override
    public Trainer findById(int id) throws DAOException {
        return findOneByInt(SELECT + "WHERE trainer_id = ?", id);
    }

    @Override
    public Trainer findByUserId(int userId) throws DAOException {
        return findOneByInt(SELECT + "WHERE user_id = ?", userId);
    }

    private Trainer findOneByInt(String sql, int value) throws DAOException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DAOException("Could not look up trainer", e);
        }
    }

    @Override
    public List<Trainer> findAll() throws DAOException {
        List<Trainer> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT + "ORDER BY trainer_id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DAOException("Could not load trainers", e);
        }
    }

    @Override
    public List<Trainer> search(String keyword) throws DAOException {
        String sql = SELECT + "WHERE full_name LIKE ? OR specialization LIKE ? OR phone LIKE ? OR email LIKE ? ORDER BY trainer_id";
        List<Trainer> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            String like = DbUtil.like(keyword);
            for (int i = 1; i <= 4; i++) {
                ps.setString(i, like);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DAOException("Could not search trainers", e);
        }
    }

    @Override
    public boolean updateContact(int trainerId, String phone, String email) throws DAOException {
        String sql = "UPDATE trainers SET phone = ?, email = ? WHERE trainer_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, phone);
            ps.setString(2, email);
            ps.setInt(3, trainerId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DAOException("Could not update trainer contact details", e);
        }
    }

    private static Trainer mapRow(ResultSet rs) throws SQLException {
        Trainer t = new Trainer();
        t.setTrainerId(rs.getInt("trainer_id"));
        t.setUserId(DbUtil.getNullableInt(rs, "user_id"));
        t.setFullName(rs.getString("full_name"));
        t.setSpecialization(rs.getString("specialization"));
        t.setPhone(rs.getString("phone"));
        t.setEmail(rs.getString("email"));
        t.setAvailability(rs.getString("availability"));
        t.setSalary(rs.getDouble("salary"));
        t.setExperienceYears(rs.getInt("experience_years"));
        return t;
    }
}
