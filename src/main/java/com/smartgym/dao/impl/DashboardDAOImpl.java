package com.smartgym.dao.impl;

import com.smartgym.dao.DashboardDAO;
import com.smartgym.exception.DAOException;
import com.smartgym.util.DatabaseConnection;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dashboard statistics.
 * Shows all three JDBC statement types:
 *   Statement          -> countRows()           (fixed SQL, no user input)
 *   PreparedStatement  -> the two "series" methods
 *   CallableStatement  -> getMonthlyRevenue()   (stored procedure)
 */
public class DashboardDAOImpl implements DashboardDAO {

    @Override
    public int countMembers() throws DAOException {
        return countRows("SELECT COUNT(*) FROM members");
    }

    @Override
    public int countActiveMemberships() throws DAOException {
        return countRows("SELECT COUNT(DISTINCT member_id) FROM memberships "
                       + "WHERE status = 'ACTIVE' AND expiry_date >= CURDATE()");
    }

    /** A member counts as expired when they bought a membership before but have no active one now. */
    @Override
    public int countExpiredMemberships() throws DAOException {
        return countRows("SELECT COUNT(*) FROM members m "
                       + "WHERE EXISTS (SELECT 1 FROM memberships x WHERE x.member_id = m.member_id) "
                       + "AND NOT EXISTS (SELECT 1 FROM memberships a WHERE a.member_id = m.member_id "
                       + "                AND a.status = 'ACTIVE' AND a.expiry_date >= CURDATE())");
    }

    @Override
    public int countTrainers() throws DAOException {
        return countRows("SELECT COUNT(*) FROM trainers");
    }

    @Override
    public int countTodayAttendance() throws DAOException {
        return countRows("SELECT COUNT(*) FROM attendance WHERE attendance_date = CURDATE()");
    }

    private int countRows(String sql) throws DAOException {
        try (Connection con = DatabaseConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new DAOException("Dashboard query failed", e);
        }
    }

    @Override
    public double getMonthlyRevenue(int year, int month) throws DAOException {
        try (Connection con = DatabaseConnection.getConnection();
             CallableStatement cs = con.prepareCall("{CALL sp_monthly_revenue(?, ?, ?)}")) {
            cs.setInt(1, year);
            cs.setInt(2, month);
            cs.registerOutParameter(3, Types.DECIMAL);   // OUT parameter
            cs.execute();
            return cs.getDouble(3);
        } catch (SQLException e) {
            throw new DAOException("Could not run sp_monthly_revenue", e);
        }
    }

    @Override
    public Map<String, Double> getMonthlyRevenueSeries(int months) throws DAOException {
        String sql = "SELECT DATE_FORMAT(payment_date, '%Y-%m') AS ym, SUM(amount) AS total "
                   + "FROM payments WHERE status = 'PAID' "
                   + "AND payment_date >= DATE_SUB(DATE_FORMAT(CURDATE(), '%Y-%m-01'), INTERVAL ? MONTH) "
                   + "GROUP BY ym ORDER BY ym";
        Map<String, Double> result = new LinkedHashMap<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, Math.max(0, months - 1));          // this month + (months-1) earlier ones
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString("ym"), rs.getDouble("total"));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DAOException("Could not load revenue series", e);
        }
    }

    @Override
    public Map<LocalDate, Integer> getAttendanceSeries(int days) throws DAOException {
        String sql = "SELECT attendance_date, COUNT(*) AS visits FROM attendance "
                   + "WHERE attendance_date >= DATE_SUB(CURDATE(), INTERVAL ? DAY) "
                   + "GROUP BY attendance_date ORDER BY attendance_date";
        Map<LocalDate, Integer> result = new LinkedHashMap<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, days);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getObject("attendance_date", LocalDate.class), rs.getInt("visits"));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DAOException("Could not load attendance series", e);
        }
    }
}
