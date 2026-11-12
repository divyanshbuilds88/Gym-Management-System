package com.smartgym.dao.impl;

import com.smartgym.dao.TrainerScheduleDAO;
import com.smartgym.exception.DAOException;
import com.smartgym.model.TrainerSchedule;
import com.smartgym.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class TrainerScheduleDAOImpl implements TrainerScheduleDAO {

    private static final String SELECT =
            "SELECT s.schedule_id, s.trainer_id, s.member_id, s.session_date, s.start_time, s.duration_minutes, "
          + "s.end_time, s.status, s.notes, t.full_name AS trainer_name, m.full_name AS member_name "
          + "FROM trainer_schedules s "
          + "JOIN trainers t ON t.trainer_id = s.trainer_id "
          + "JOIN members m ON m.member_id = s.member_id ";

    @Override
    public int insert(TrainerSchedule s) throws DAOException {
        String sql = "INSERT INTO trainer_schedules (trainer_id, member_id, session_date, start_time, duration_minutes, end_time, status, notes) "
                   + "VALUES (?,?,?,?,?,?,?,?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, s.getTrainerId());
            ps.setInt(2, s.getMemberId());
            ps.setObject(3, s.getSessionDate());
            ps.setObject(4, s.getStartTime());
            ps.setInt(5, s.getDurationMinutes());
            ps.setObject(6, s.getComputedEndTime());
            ps.setString(7, s.getStatus() == null ? "SCHEDULED" : s.getStatus());
            ps.setString(8, s.getNotes());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new DAOException("Session was saved but no ID was returned");
        } catch (SQLException e) {
            throw new DAOException("Could not schedule session", e);
        }
    }

    @Override
    public boolean update(TrainerSchedule s) throws DAOException {
        String sql = "UPDATE trainer_schedules SET trainer_id = ?, member_id = ?, session_date = ?, start_time = ?, "
                   + "duration_minutes = ?, end_time = ?, status = ?, notes = ? WHERE schedule_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, s.getTrainerId());
            ps.setInt(2, s.getMemberId());
            ps.setObject(3, s.getSessionDate());
            ps.setObject(4, s.getStartTime());
            ps.setInt(5, s.getDurationMinutes());
            ps.setObject(6, s.getComputedEndTime());
            ps.setString(7, s.getStatus());
            ps.setString(8, s.getNotes());
            ps.setInt(9, s.getScheduleId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DAOException("Could not update session", e);
        }
    }

    @Override
    public boolean delete(int id) throws DAOException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM trainer_schedules WHERE schedule_id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DAOException("Could not delete session", e);
        }
    }

    @Override
    public TrainerSchedule findById(int id) throws DAOException {
        List<TrainerSchedule> list = query(SELECT + "WHERE s.schedule_id = ?", id, null);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public List<TrainerSchedule> findAll() throws DAOException {
        return query(SELECT + "ORDER BY s.session_date DESC, s.start_time", null, null);
    }

    @Override
    public List<TrainerSchedule> findByTrainer(int trainerId) throws DAOException {
        return query(SELECT + "WHERE s.trainer_id = ? ORDER BY s.session_date, s.start_time", trainerId, null);
    }

    @Override
    public List<TrainerSchedule> findByMember(int memberId) throws DAOException {
        return query(SELECT + "WHERE s.member_id = ? ORDER BY s.session_date, s.start_time", memberId, null);
    }

    @Override
    public List<TrainerSchedule> findByDate(LocalDate date) throws DAOException {
        return query(SELECT + "WHERE s.session_date = ? ORDER BY s.start_time", null, date);
    }

    @Override
    public List<TrainerSchedule> findUpcoming(int limit) throws DAOException {
        String sql = SELECT + "WHERE s.status = 'SCHEDULED' "
                   + "AND TIMESTAMP(s.session_date, s.start_time) >= NOW() "
                   + "ORDER BY s.session_date, s.start_time LIMIT " + limit;   // limit is an int, not user text
        return query(sql, null, null);
    }

    @Override
    public boolean updateStatus(int scheduleId, String status) throws DAOException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE trainer_schedules SET status = ? WHERE schedule_id = ?")) {
            ps.setString(1, status);
            ps.setInt(2, scheduleId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DAOException("Could not update session status", e);
        }
    }

    @Override
    public boolean hasTrainerConflict(int trainerId, LocalDate date, LocalTime start, LocalTime end, int excludeScheduleId)
            throws DAOException {
        return overlapCount("trainer_id", trainerId, date, start, end, excludeScheduleId) > 0;
    }

    @Override
    public boolean hasMemberConflict(int memberId, LocalDate date, LocalTime start, LocalTime end, int excludeScheduleId)
            throws DAOException {
        return overlapCount("member_id", memberId, date, start, end, excludeScheduleId) > 0;
    }

    /**
     * Two time ranges overlap when: existingStart < newEnd AND existingEnd > newStart.
     * 'column' is only ever the fixed text "trainer_id" or "member_id" written in this class (never user input).
     */
    private int overlapCount(String column, int id, LocalDate date, LocalTime start, LocalTime end, int excludeId)
            throws DAOException {
        String sql = "SELECT COUNT(*) FROM trainer_schedules WHERE " + column + " = ? AND session_date = ? "
                   + "AND status <> 'CANCELLED' AND start_time < ? AND end_time > ? AND schedule_id <> ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setObject(2, date);
            ps.setObject(3, end);
            ps.setObject(4, start);
            ps.setInt(5, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DAOException("Could not check schedule conflicts", e);
        }
    }

    private List<TrainerSchedule> query(String sql, Integer intParam, LocalDate dateParam) throws DAOException {
        List<TrainerSchedule> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (intParam != null) {
                ps.setInt(1, intParam);
            } else if (dateParam != null) {
                ps.setObject(1, dateParam);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DAOException("Could not load sessions", e);
        }
    }

    private static TrainerSchedule mapRow(ResultSet rs) throws SQLException {
        TrainerSchedule s = new TrainerSchedule();
        s.setScheduleId(rs.getInt("schedule_id"));
        s.setTrainerId(rs.getInt("trainer_id"));
        s.setMemberId(rs.getInt("member_id"));
        s.setSessionDate(rs.getObject("session_date", LocalDate.class));
        s.setStartTime(rs.getObject("start_time", LocalTime.class));
        s.setDurationMinutes(rs.getInt("duration_minutes"));
        s.setEndTime(rs.getObject("end_time", LocalTime.class));
        s.setStatus(rs.getString("status"));
        s.setNotes(rs.getString("notes"));
        s.setTrainerName(rs.getString("trainer_name"));
        s.setMemberName(rs.getString("member_name"));
        return s;
    }
}
