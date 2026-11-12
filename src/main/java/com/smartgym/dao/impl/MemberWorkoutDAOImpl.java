package com.smartgym.dao.impl;

import com.smartgym.dao.MemberWorkoutDAO;
import com.smartgym.exception.DAOException;
import com.smartgym.model.MemberWorkout;
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

public class MemberWorkoutDAOImpl implements MemberWorkoutDAO {

    private static final String SELECT =
            "SELECT mw.assignment_id, mw.member_id, mw.workout_plan_id, mw.trainer_id, mw.assigned_date, mw.end_date, mw.status, "
          + "m.full_name AS member_name, w.plan_name, t.full_name AS trainer_name "
          + "FROM member_workouts mw "
          + "JOIN members m ON m.member_id = mw.member_id "
          + "JOIN workout_plans w ON w.workout_plan_id = mw.workout_plan_id "
          + "JOIN trainers t ON t.trainer_id = mw.trainer_id ";

    @Override
    public int assign(MemberWorkout a) throws DAOException {
        String sql = "INSERT INTO member_workouts (member_id, workout_plan_id, trainer_id, assigned_date, end_date, status) VALUES (?,?,?,?,?,?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, a.getMemberId());
            ps.setInt(2, a.getWorkoutPlanId());
            ps.setInt(3, a.getTrainerId());
            ps.setObject(4, a.getAssignedDate());
            DbUtil.setNullableDate(ps, 5, a.getEndDate());
            ps.setString(6, a.getStatus() == null ? "ACTIVE" : a.getStatus());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new DAOException("Assignment was saved but no ID was returned");
        } catch (SQLException e) {
            throw new DAOException("Could not assign workout plan", e);
        }
    }

    @Override
    public List<MemberWorkout> findAll() throws DAOException {
        return query(SELECT + "ORDER BY mw.assignment_id DESC", null);
    }

    @Override
    public List<MemberWorkout> findByMember(int memberId) throws DAOException {
        return query(SELECT + "WHERE mw.member_id = ? ORDER BY mw.assignment_id DESC", memberId);
    }

    @Override
    public List<MemberWorkout> findByTrainer(int trainerId) throws DAOException {
        return query(SELECT + "WHERE mw.trainer_id = ? ORDER BY mw.assignment_id DESC", trainerId);
    }

    private List<MemberWorkout> query(String sql, Integer param) throws DAOException {
        List<MemberWorkout> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (param != null) {
                ps.setInt(1, param);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MemberWorkout a = new MemberWorkout();
                    a.setAssignmentId(rs.getInt("assignment_id"));
                    a.setMemberId(rs.getInt("member_id"));
                    a.setWorkoutPlanId(rs.getInt("workout_plan_id"));
                    a.setTrainerId(rs.getInt("trainer_id"));
                    a.setAssignedDate(rs.getObject("assigned_date", LocalDate.class));
                    a.setEndDate(rs.getObject("end_date", LocalDate.class));
                    a.setStatus(rs.getString("status"));
                    a.setMemberName(rs.getString("member_name"));
                    a.setPlanName(rs.getString("plan_name"));
                    a.setTrainerName(rs.getString("trainer_name"));
                    list.add(a);
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DAOException("Could not load assigned workouts", e);
        }
    }

    @Override
    public boolean updateStatus(int assignmentId, String status) throws DAOException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE member_workouts SET status = ? WHERE assignment_id = ?")) {
            ps.setString(1, status);
            ps.setInt(2, assignmentId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DAOException("Could not update assignment", e);
        }
    }

    @Override
    public boolean delete(int assignmentId) throws DAOException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM member_workouts WHERE assignment_id = ?")) {
            ps.setInt(1, assignmentId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DAOException("Could not delete assignment", e);
        }
    }
}
