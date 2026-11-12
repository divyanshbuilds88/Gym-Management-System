package com.smartgym.dao.impl;

import com.smartgym.dao.MemberDAO;
import com.smartgym.exception.DAOException;
import com.smartgym.model.Member;
import com.smartgym.util.DatabaseConnection;
import com.smartgym.util.DbUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MemberDAOImpl implements MemberDAO {

    // Member columns + the member's LATEST membership (join with memberships and plans)
    private static final String SELECT_WITH_PLAN =
            "SELECT m.member_id, m.member_code, m.user_id, m.full_name, m.age, m.gender, m.phone, m.email, "
          + "m.address, m.joining_date, p.plan_name, ms.start_date AS ms_start, ms.expiry_date AS ms_expiry, "
          + "CASE WHEN ms.membership_id IS NULL THEN 'NONE' "
          + "     WHEN ms.status = 'CANCELLED' THEN 'CANCELLED' "
          + "     WHEN ms.expiry_date < CURDATE() THEN 'EXPIRED' "
          + "     ELSE ms.status END AS ms_status "
          + "FROM members m "
          + "LEFT JOIN memberships ms ON ms.membership_id = "
          + "   (SELECT MAX(x.membership_id) FROM memberships x WHERE x.member_id = m.member_id) "
          + "LEFT JOIN membership_plans p ON p.plan_id = ms.plan_id ";

    @Override
    public int insert(Member member) throws DAOException {
        try (Connection con = DatabaseConnection.getConnection()) {
            return insert(con, member);
        } catch (SQLException e) {
            throw new DAOException("Could not connect to the database", e);
        }
    }

    @Override
    public int insert(Connection con, Member m) throws DAOException {
        String sql = "INSERT INTO members (member_code, user_id, full_name, age, gender, phone, email, address, joining_date) "
                   + "VALUES (?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, m.getMemberCode());
            DbUtil.setNullableInt(ps, 2, m.getUserId());
            ps.setString(3, m.getFullName());
            ps.setInt(4, m.getAge());
            ps.setString(5, m.getGender());
            ps.setString(6, m.getPhone());
            ps.setString(7, m.getEmail());
            ps.setString(8, m.getAddress());
            ps.setObject(9, m.getJoiningDate());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new DAOException("Member was saved but no ID was returned");
        } catch (SQLException e) {
            throw new DAOException("Could not add member", e);
        }
    }

    @Override
    public boolean update(Member m) throws DAOException {
        String sql = "UPDATE members SET full_name = ?, age = ?, gender = ?, phone = ?, email = ?, "
                   + "address = ?, joining_date = ? WHERE member_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, m.getFullName());
            ps.setInt(2, m.getAge());
            ps.setString(3, m.getGender());
            ps.setString(4, m.getPhone());
            ps.setString(5, m.getEmail());
            ps.setString(6, m.getAddress());
            ps.setObject(7, m.getJoiningDate());
            ps.setInt(8, m.getMemberId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DAOException("Could not update member", e);
        }
    }

    /**
     * Deletes the member AND the member's login account in one TRANSACTION.
     * If anything fails, rollback() undoes both deletes.
     */
    @Override
    public boolean delete(int memberId) throws DAOException {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);                     // start transaction

            Integer userId = null;
            try (PreparedStatement ps = con.prepareStatement("SELECT user_id FROM members WHERE member_id = ?")) {
                ps.setInt(1, memberId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        con.rollback();
                        return false;
                    }
                    userId = DbUtil.getNullableInt(rs, "user_id");
                }
            }

            boolean deleted;
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM members WHERE member_id = ?")) {
                ps.setInt(1, memberId);
                deleted = ps.executeUpdate() > 0;
            }
            if (userId != null) {
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM users WHERE user_id = ?")) {
                    ps.setInt(1, userId);
                    ps.executeUpdate();
                }
            }
            con.commit();                                 // both deletes succeeded
            return deleted;
        } catch (SQLException e) {
            DbUtil.rollbackQuietly(con);
            throw new DAOException("Could not delete member", e);
        } finally {
            DbUtil.closeQuietly(con);                     // finally always runs
        }
    }

    @Override
    public Member findById(int id) throws DAOException {
        return findOne(SELECT_WITH_PLAN + "WHERE m.member_id = ?", String.valueOf(id), true);
    }

    @Override
    public Member findByCode(String memberCode) throws DAOException {
        return findOne(SELECT_WITH_PLAN + "WHERE m.member_code = ?", memberCode, false);
    }

    @Override
    public Member findByUserId(int userId) throws DAOException {
        return findOne(SELECT_WITH_PLAN + "WHERE m.user_id = ?", String.valueOf(userId), true);
    }

    /** Helper: runs a query with one parameter (number or text) and returns the first row. */
    private Member findOne(String sql, String value, boolean numeric) throws DAOException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (numeric) {
                ps.setInt(1, Integer.parseInt(value));
            } else {
                ps.setString(1, value);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DAOException("Could not look up member", e);
        }
    }

    @Override
    public List<Member> findAll() throws DAOException {
        List<Member> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_WITH_PLAN + "ORDER BY m.member_id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DAOException("Could not load members", e);
        }
    }

    @Override
    public List<Member> search(String keyword) throws DAOException {
        String sql = SELECT_WITH_PLAN
                   + "WHERE m.full_name LIKE ? OR m.member_code LIKE ? OR m.phone LIKE ? OR m.email LIKE ? "
                   + "ORDER BY m.member_id";
        List<Member> list = new ArrayList<>();
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
            throw new DAOException("Could not search members", e);
        }
    }

    @Override
    public List<Member> findRecent(int limit) throws DAOException {
        List<Member> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     SELECT_WITH_PLAN + "ORDER BY m.created_at DESC, m.member_id DESC LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DAOException("Could not load recent members", e);
        }
    }

    @Override
    public String generateNextCode() throws DAOException {
        String sql = "SELECT COALESCE(MAX(CAST(SUBSTRING(member_code, 4) AS UNSIGNED)), 0) + 1 FROM members";
        try (Connection con = DatabaseConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            int next = rs.next() ? rs.getInt(1) : 1;
            return String.format("GYM%04d", next);
        } catch (SQLException e) {
            throw new DAOException("Could not generate member code", e);
        }
    }

    @Override
    public boolean updateContact(int memberId, String phone, String email, String address) throws DAOException {
        String sql = "UPDATE members SET phone = ?, email = ?, address = ? WHERE member_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, phone);
            ps.setString(2, email);
            ps.setString(3, address);
            ps.setInt(4, memberId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DAOException("Could not update contact details", e);
        }
    }

    private static Member mapRow(ResultSet rs) throws SQLException {
        Member m = new Member();
        m.setMemberId(rs.getInt("member_id"));
        m.setMemberCode(rs.getString("member_code"));
        m.setUserId(DbUtil.getNullableInt(rs, "user_id"));
        m.setFullName(rs.getString("full_name"));
        m.setAge(rs.getInt("age"));
        m.setGender(rs.getString("gender"));
        m.setPhone(rs.getString("phone"));
        m.setEmail(rs.getString("email"));
        m.setAddress(rs.getString("address"));
        m.setJoiningDate(rs.getObject("joining_date", LocalDate.class));
        m.setPlanName(rs.getString("plan_name"));
        m.setMembershipStart(rs.getObject("ms_start", LocalDate.class));
        m.setMembershipExpiry(rs.getObject("ms_expiry", LocalDate.class));
        m.setMembershipStatus(rs.getString("ms_status"));
        return m;
    }
}
