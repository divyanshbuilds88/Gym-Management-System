package com.smartgym.dao.impl;

import com.smartgym.dao.AttendanceDAO;
import com.smartgym.exception.DAOException;
import com.smartgym.model.Attendance;
import com.smartgym.util.DatabaseConnection;
import com.smartgym.util.DbUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AttendanceDAOImpl implements AttendanceDAO {

    private static final String SELECT =
            "SELECT a.attendance_id, a.member_id, a.attendance_date, a.check_in_time, a.check_out_time, "
          + "m.full_name, m.member_code "
          + "FROM attendance a JOIN members m ON m.member_id = a.member_id ";

    /** FOR UPDATE locks the row until commit/rollback so two clicks cannot create two open visits. */
    @Override
    public Attendance findOpenCheckInForUpdate(Connection con, int memberId) throws DAOException {
        String sql = "SELECT a.attendance_id, a.member_id, a.attendance_date, a.check_in_time, a.check_out_time, "
                   + "'' AS full_name, '' AS member_code FROM attendance a "
                   + "WHERE a.member_id = ? AND a.check_out_time IS NULL FOR UPDATE";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DAOException("Could not check current attendance", e);
        }
    }

    @Override
    public int checkIn(Connection con, int memberId, LocalDateTime when) throws DAOException {
        String sql = "INSERT INTO attendance (member_id, attendance_date, check_in_time) VALUES (?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, memberId);
            ps.setObject(2, when.toLocalDate());
            ps.setObject(3, when);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new DAOException("Check-in was saved but no ID was returned");
        } catch (SQLException e) {
            throw new DAOException("Could not save check-in", e);
        }
    }

    @Override
    public boolean checkOut(Connection con, int attendanceId, LocalDateTime when) throws DAOException {
        String sql = "UPDATE attendance SET check_out_time = ? WHERE attendance_id = ? AND check_out_time IS NULL";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, when);
            ps.setInt(2, attendanceId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DAOException("Could not save check-out", e);
        }
    }

    @Override
    public List<Attendance> findByDate(LocalDate date) throws DAOException {
        return query(SELECT + "WHERE a.attendance_date = ? ORDER BY a.check_in_time DESC", date, null, null);
    }

    @Override
    public List<Attendance> findByMember(int memberId) throws DAOException {
        return query(SELECT + "WHERE a.member_id = ? ORDER BY a.check_in_time DESC", null, memberId, null);
    }

    @Override
    public List<Attendance> searchByMember(String keyword) throws DAOException {
        return query(SELECT + "WHERE m.full_name LIKE ? OR m.member_code LIKE ? ORDER BY a.check_in_time DESC",
                     null, null, DbUtil.like(keyword));
    }

    @Override
    public List<Attendance> findBetween(LocalDate from, LocalDate to) throws DAOException {
        List<Attendance> list = new ArrayList<>();
        String sql = SELECT + "WHERE a.attendance_date BETWEEN ? AND ? ORDER BY a.check_in_time DESC";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, from);
            ps.setObject(2, to);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DAOException("Could not load attendance", e);
        }
    }

    /** Runs the SQL with a date parameter, a member id parameter, or a LIKE text (used for both ? marks). */
    private List<Attendance> query(String sql, LocalDate date, Integer memberId, String like) throws DAOException {
        List<Attendance> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (date != null) {
                ps.setObject(1, date);
            } else if (memberId != null) {
                ps.setInt(1, memberId);
            } else if (like != null) {
                ps.setString(1, like);
                ps.setString(2, like);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DAOException("Could not load attendance", e);
        }
    }

    private static Attendance mapRow(ResultSet rs) throws SQLException {
        Attendance a = new Attendance();
        a.setAttendanceId(rs.getInt("attendance_id"));
        a.setMemberId(rs.getInt("member_id"));
        a.setAttendanceDate(rs.getObject("attendance_date", LocalDate.class));
        a.setCheckInTime(rs.getObject("check_in_time", LocalDateTime.class));
        a.setCheckOutTime(rs.getObject("check_out_time", LocalDateTime.class));
        a.setMemberName(rs.getString("full_name"));
        a.setMemberCode(rs.getString("member_code"));
        return a;
    }
}
