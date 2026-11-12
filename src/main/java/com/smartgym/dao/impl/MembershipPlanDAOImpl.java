package com.smartgym.dao.impl;

import com.smartgym.dao.MembershipPlanDAO;
import com.smartgym.exception.DAOException;
import com.smartgym.model.MembershipPlan;
import com.smartgym.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MembershipPlanDAOImpl implements MembershipPlanDAO {

    private static final String SELECT =
            "SELECT plan_id, plan_name, duration_months, fee, description, is_active FROM membership_plans ";

    @Override
    public int insert(MembershipPlan p) throws DAOException {
        String sql = "INSERT INTO membership_plans (plan_name, duration_months, fee, description, is_active) VALUES (?,?,?,?,?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getPlanName());
            ps.setInt(2, p.getDurationMonths());
            ps.setDouble(3, p.getFee());
            ps.setString(4, p.getDescription());
            ps.setBoolean(5, p.isActive());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new DAOException("Plan was saved but no ID was returned");
        } catch (SQLException e) {
            throw new DAOException("Could not add membership plan", e);
        }
    }

    @Override
    public boolean update(MembershipPlan p) throws DAOException {
        String sql = "UPDATE membership_plans SET plan_name = ?, duration_months = ?, fee = ?, description = ?, is_active = ? WHERE plan_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, p.getPlanName());
            ps.setInt(2, p.getDurationMonths());
            ps.setDouble(3, p.getFee());
            ps.setString(4, p.getDescription());
            ps.setBoolean(5, p.isActive());
            ps.setInt(6, p.getPlanId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DAOException("Could not update membership plan", e);
        }
    }

    @Override
    public boolean delete(int id) throws DAOException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM membership_plans WHERE plan_id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DAOException("Could not delete membership plan", e);   // fails if members use this plan
        }
    }

    @Override
    public MembershipPlan findById(int id) throws DAOException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT + "WHERE plan_id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DAOException("Could not look up membership plan", e);
        }
    }

    @Override
    public List<MembershipPlan> findAll() throws DAOException {
        return query(SELECT + "ORDER BY duration_months");
    }

    @Override
    public List<MembershipPlan> findAllActive() throws DAOException {
        return query(SELECT + "WHERE is_active = 1 ORDER BY duration_months");
    }

    private List<MembershipPlan> query(String sql) throws DAOException {
        List<MembershipPlan> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DAOException("Could not load membership plans", e);
        }
    }

    private static MembershipPlan mapRow(ResultSet rs) throws SQLException {
        MembershipPlan p = new MembershipPlan();
        p.setPlanId(rs.getInt("plan_id"));
        p.setPlanName(rs.getString("plan_name"));
        p.setDurationMonths(rs.getInt("duration_months"));
        p.setFee(rs.getDouble("fee"));
        p.setDescription(rs.getString("description"));
        p.setActive(rs.getBoolean("is_active"));
        return p;
    }
}
