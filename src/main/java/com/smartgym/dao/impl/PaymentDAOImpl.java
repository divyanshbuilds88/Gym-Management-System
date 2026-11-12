package com.smartgym.dao.impl;

import com.smartgym.dao.PaymentDAO;
import com.smartgym.exception.DAOException;
import com.smartgym.model.Payment;
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

public class PaymentDAOImpl implements PaymentDAO {

    private static final String SELECT =
            "SELECT p.payment_id, p.receipt_no, p.member_id, p.membership_id, p.amount, p.payment_date, "
          + "p.method, p.status, p.notes, m.full_name, m.member_code "
          + "FROM payments p JOIN members m ON m.member_id = p.member_id ";

    private static final String NEXT_RECEIPT_SQL =
            "SELECT COALESCE(MAX(CAST(SUBSTRING(receipt_no, 5) AS UNSIGNED)), 0) + 1 FROM payments";

    @Override
    public int insert(Payment payment) throws DAOException {
        try (Connection con = DatabaseConnection.getConnection()) {
            return insert(con, payment);
        } catch (SQLException e) {
            throw new DAOException("Could not connect to the database", e);
        }
    }

    @Override
    public int insert(Connection con, Payment p) throws DAOException {
        String sql = "INSERT INTO payments (receipt_no, member_id, membership_id, amount, payment_date, method, status, notes) "
                   + "VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getReceiptNo());
            ps.setInt(2, p.getMemberId());
            DbUtil.setNullableInt(ps, 3, p.getMembershipId());
            ps.setDouble(4, p.getAmount());
            ps.setObject(5, p.getPaymentDate());
            ps.setString(6, p.getMethod());
            ps.setString(7, p.getStatus());
            ps.setString(8, p.getNotes());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new DAOException("Payment was saved but no ID was returned");
        } catch (SQLException e) {
            throw new DAOException("Could not save payment", e);
        }
    }

    @Override
    public Payment findById(int paymentId) throws DAOException {
        List<Payment> list = query(SELECT + "WHERE p.payment_id = ?", paymentId, null);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public List<Payment> findAll() throws DAOException {
        return query(SELECT + "ORDER BY p.payment_date DESC, p.payment_id DESC", null, null);
    }

    @Override
    public List<Payment> findByMember(int memberId) throws DAOException {
        return query(SELECT + "WHERE p.member_id = ? ORDER BY p.payment_date DESC, p.payment_id DESC", memberId, null);
    }

    @Override
    public List<Payment> findByStatus(String status) throws DAOException {
        return query(SELECT + "WHERE p.status = ? ORDER BY p.payment_date DESC, p.payment_id DESC", null, status);
    }

    /** Runs the SQL with either one int parameter or one text parameter (or none). */
    private List<Payment> query(String sql, Integer intParam, String textParam) throws DAOException {
        List<Payment> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (intParam != null) {
                ps.setInt(1, intParam);
            } else if (textParam != null) {
                ps.setString(1, textParam);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DAOException("Could not load payments", e);
        }
    }

    @Override
    public boolean updateStatus(int paymentId, String status) throws DAOException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE payments SET status = ? WHERE payment_id = ?")) {
            ps.setString(1, status);
            ps.setInt(2, paymentId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DAOException("Could not update payment status", e);
        }
    }

    @Override
    public String generateReceiptNo(Connection con) throws DAOException {
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(NEXT_RECEIPT_SQL)) {
            int next = rs.next() ? rs.getInt(1) : 1;
            return String.format("RCP-%04d", next);
        } catch (SQLException e) {
            throw new DAOException("Could not generate receipt number", e);
        }
    }

    @Override
    public String generateReceiptNo() throws DAOException {
        try (Connection con = DatabaseConnection.getConnection()) {
            return generateReceiptNo(con);
        } catch (SQLException e) {
            throw new DAOException("Could not connect to the database", e);
        }
    }

    private static Payment mapRow(ResultSet rs) throws SQLException {
        Payment p = new Payment();
        p.setPaymentId(rs.getInt("payment_id"));
        p.setReceiptNo(rs.getString("receipt_no"));
        p.setMemberId(rs.getInt("member_id"));
        p.setMembershipId(DbUtil.getNullableInt(rs, "membership_id"));
        p.setAmount(rs.getDouble("amount"));
        p.setPaymentDate(rs.getObject("payment_date", LocalDate.class));
        p.setMethod(rs.getString("method"));
        p.setStatus(rs.getString("status"));
        p.setNotes(rs.getString("notes"));
        p.setMemberName(rs.getString("full_name"));
        p.setMemberCode(rs.getString("member_code"));
        return p;
    }
}
