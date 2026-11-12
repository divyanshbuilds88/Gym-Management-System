package com.smartgym.dao.impl;

import com.smartgym.dao.MembershipDAO;
import com.smartgym.exception.DAOException;
import com.smartgym.model.Membership;
import com.smartgym.util.DatabaseConnection;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MembershipDAOImpl implements MembershipDAO {

    // cur_status shows EXPIRED automatically when the expiry date has passed
    private static final String SELECT =
            "SELECT ms.membership_id, ms.member_id, ms.plan_id, ms.start_date, ms.expiry_date, "
          + "CASE WHEN ms.status = 'ACTIVE' AND ms.expiry_date < CURDATE() THEN 'EXPIRED' ELSE ms.status END AS cur_status, "
          + "m.full_name, m.member_code, p.plan_name "
          + "FROM memberships ms "
          + "JOIN members m ON m.member_id = ms.member_id "
          + "JOIN membership_plans p ON p.plan_id = ms.plan_id ";

    @Override
    public int insert(Connection con, Membership ms) throws DAOException {
        String sql = "INSERT INTO memberships (member_id, plan_id, start_date, expiry_date, status) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, ms.getMemberId());
            ps.setInt(2, ms.getPlanId());
            ps.setObject(3, ms.getStartDate());
            ps.setObject(4, ms.getExpiryDate());
            ps.setString(5, ms.getStatus());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new DAOException("Membership was saved but no ID was returned");
        } catch (SQLException e) {
            throw new DAOException("Could not save membership", e);
        }
    }

    @Override
    public Membership findById(int membershipId) throws DAOException {
        List<Membership> list = query(SELECT + "WHERE ms.membership_id = ?", membershipId);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public List<Membership> findAll() throws DAOException {
        return query(SELECT + "ORDER BY ms.membership_id DESC", null);
    }

    @Override
    public List<Membership> findByMember(int memberId) throws DAOException {
        return query(SELECT + "WHERE ms.member_id = ? ORDER BY ms.membership_id DESC", memberId);
    }

    @Override
    public Membership findLatestByMember(int memberId) throws DAOException {
        List<Membership> list = query(SELECT + "WHERE ms.member_id = ? ORDER BY ms.membership_id DESC LIMIT 1", memberId);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public List<Membership> findExpiringWithin(int days) throws DAOException {
        String sql = SELECT + "WHERE ms.status = 'ACTIVE' AND ms.expiry_date >= CURDATE() "
                   + "AND ms.expiry_date <= DATE_ADD(CURDATE(), INTERVAL ? DAY) ORDER BY ms.expiry_date";
        return query(sql, days);
    }

    /** Runs the SQL; 'param' is the single ? value, or null when the SQL has no parameter. */
    private List<Membership> query(String sql, Integer param) throws DAOException {
        List<Membership> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (param != null) {
                ps.setInt(1, param);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DAOException("Could not load memberships", e);
        }
    }

    @Override
    public boolean updateStatus(int membershipId, String status) throws DAOException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE memberships SET status = ? WHERE membership_id = ?")) {
            ps.setString(1, status);
            ps.setInt(2, membershipId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DAOException("Could not update membership status", e);
        }
    }

    /** CallableStatement: calls the stored procedure with one OUT parameter. */
    @Override
    public int expireOverdueMemberships() throws DAOException {
        try (Connection con = DatabaseConnection.getConnection();
             CallableStatement cs = con.prepareCall("{CALL sp_expire_memberships(?)}")) {
            cs.registerOutParameter(1, Types.INTEGER);
            cs.execute();
            return cs.getInt(1);
        } catch (SQLException e) {
            throw new DAOException("Could not run sp_expire_memberships", e);
        }
    }

    private static Membership mapRow(ResultSet rs) throws SQLException {
        Membership ms = new Membership();
        ms.setMembershipId(rs.getInt("membership_id"));
        ms.setMemberId(rs.getInt("member_id"));
        ms.setPlanId(rs.getInt("plan_id"));
        ms.setStartDate(rs.getObject("start_date", LocalDate.class));
        ms.setExpiryDate(rs.getObject("expiry_date", LocalDate.class));
        ms.setStatus(rs.getString("cur_status"));
        ms.setMemberName(rs.getString("full_name"));
        ms.setMemberCode(rs.getString("member_code"));
        ms.setPlanName(rs.getString("plan_name"));
        return ms;
    }
}
