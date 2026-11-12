package com.smartgym.dao.impl;

import com.smartgym.dao.WorkoutPlanDAO;
import com.smartgym.exception.DAOException;
import com.smartgym.model.WorkoutExercise;
import com.smartgym.model.WorkoutPlan;
import com.smartgym.util.DatabaseConnection;
import com.smartgym.util.DbUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class WorkoutPlanDAOImpl implements WorkoutPlanDAO {

    private static final String SELECT =
            "SELECT w.workout_plan_id, w.plan_name, w.goal, w.level, w.description, w.created_by, t.full_name AS trainer_name "
          + "FROM workout_plans w LEFT JOIN trainers t ON t.trainer_id = w.created_by ";

    private static final String SELECT_EXERCISE =
            "SELECT exercise_id, workout_plan_id, exercise_name, sets_count, repetitions, duration_minutes, "
          + "target_muscle_group, schedule_day FROM workout_exercises ";

    private static final String INSERT_PLAN =
            "INSERT INTO workout_plans (plan_name, goal, level, description, created_by) VALUES (?,?,?,?,?)";

    private static final String INSERT_EXERCISE =
            "INSERT INTO workout_exercises (workout_plan_id, exercise_name, sets_count, repetitions, duration_minutes, "
          + "target_muscle_group, schedule_day) VALUES (?,?,?,?,?,?,?)";

    @Override
    public int insert(WorkoutPlan plan) throws DAOException {
        try (Connection con = DatabaseConnection.getConnection()) {
            return insertPlan(con, plan);
        } catch (SQLException e) {
            throw new DAOException("Could not add workout plan", e);
        }
    }

    /** Transaction: the plan and ALL its exercises are saved together, or nothing is saved. */
    @Override
    public int insertWithExercises(WorkoutPlan plan, List<WorkoutExercise> exercises) throws DAOException {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);
            int planId = insertPlan(con, plan);
            for (WorkoutExercise ex : exercises) {
                ex.setWorkoutPlanId(planId);
                insertExercise(con, ex);
            }
            con.commit();
            return planId;
        } catch (SQLException e) {
            DbUtil.rollbackQuietly(con);          // undo the plan if any exercise failed
            throw new DAOException("Could not save workout plan", e);
        } finally {
            DbUtil.closeQuietly(con);
        }
    }

    private int insertPlan(Connection con, WorkoutPlan p) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(INSERT_PLAN, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getPlanName());
            ps.setString(2, p.getGoal());
            ps.setString(3, p.getLevel());
            ps.setString(4, p.getDescription());
            DbUtil.setNullableInt(ps, 5, p.getCreatedBy());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new SQLException("No generated key returned for workout plan");
        }
    }

    private int insertExercise(Connection con, WorkoutExercise e) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(INSERT_EXERCISE, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, e.getWorkoutPlanId());
            ps.setString(2, e.getExerciseName());
            ps.setInt(3, e.getSets());
            ps.setInt(4, e.getRepetitions());
            ps.setInt(5, e.getDurationMinutes());
            ps.setString(6, e.getTargetMuscleGroup());
            ps.setString(7, e.getScheduleDay());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new SQLException("No generated key returned for exercise");
        }
    }

    @Override
    public boolean update(WorkoutPlan p) throws DAOException {
        String sql = "UPDATE workout_plans SET plan_name = ?, goal = ?, level = ?, description = ?, created_by = ? WHERE workout_plan_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, p.getPlanName());
            ps.setString(2, p.getGoal());
            ps.setString(3, p.getLevel());
            ps.setString(4, p.getDescription());
            DbUtil.setNullableInt(ps, 5, p.getCreatedBy());
            ps.setInt(6, p.getWorkoutPlanId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DAOException("Could not update workout plan", e);
        }
    }

    @Override
    public boolean delete(int id) throws DAOException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM workout_plans WHERE workout_plan_id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DAOException("Could not delete workout plan", e);
        }
    }

    @Override
    public WorkoutPlan findById(int id) throws DAOException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT + "WHERE w.workout_plan_id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapPlan(rs) : null;
            }
        } catch (SQLException e) {
            throw new DAOException("Could not look up workout plan", e);
        }
    }

    @Override
    public List<WorkoutPlan> findAll() throws DAOException {
        List<WorkoutPlan> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT + "ORDER BY w.workout_plan_id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapPlan(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DAOException("Could not load workout plans", e);
        }
    }

    @Override
    public List<WorkoutExercise> findExercises(int workoutPlanId) throws DAOException {
        List<WorkoutExercise> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     SELECT_EXERCISE + "WHERE workout_plan_id = ? ORDER BY FIELD(schedule_day,'MON','TUE','WED','THU','FRI','SAT','SUN'), exercise_id")) {
            ps.setInt(1, workoutPlanId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapExercise(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DAOException("Could not load exercises", e);
        }
    }

    @Override
    public WorkoutExercise findExerciseById(int exerciseId) throws DAOException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_EXERCISE + "WHERE exercise_id = ?")) {
            ps.setInt(1, exerciseId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapExercise(rs) : null;
            }
        } catch (SQLException e) {
            throw new DAOException("Could not look up exercise", e);
        }
    }

    @Override
    public int addExercise(WorkoutExercise ex) throws DAOException {
        try (Connection con = DatabaseConnection.getConnection()) {
            return insertExercise(con, ex);
        } catch (SQLException e) {
            throw new DAOException("Could not add exercise", e);
        }
    }

    @Override
    public boolean updateExercise(WorkoutExercise e) throws DAOException {
        String sql = "UPDATE workout_exercises SET exercise_name = ?, sets_count = ?, repetitions = ?, duration_minutes = ?, "
                   + "target_muscle_group = ?, schedule_day = ? WHERE exercise_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, e.getExerciseName());
            ps.setInt(2, e.getSets());
            ps.setInt(3, e.getRepetitions());
            ps.setInt(4, e.getDurationMinutes());
            ps.setString(5, e.getTargetMuscleGroup());
            ps.setString(6, e.getScheduleDay());
            ps.setInt(7, e.getExerciseId());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new DAOException("Could not update exercise", ex);
        }
    }

    @Override
    public boolean deleteExercise(int exerciseId) throws DAOException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM workout_exercises WHERE exercise_id = ?")) {
            ps.setInt(1, exerciseId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DAOException("Could not delete exercise", e);
        }
    }

    private static WorkoutPlan mapPlan(ResultSet rs) throws SQLException {
        WorkoutPlan p = new WorkoutPlan();
        p.setWorkoutPlanId(rs.getInt("workout_plan_id"));
        p.setPlanName(rs.getString("plan_name"));
        p.setGoal(rs.getString("goal"));
        p.setLevel(rs.getString("level"));
        p.setDescription(rs.getString("description"));
        p.setCreatedBy(DbUtil.getNullableInt(rs, "created_by"));
        p.setTrainerName(rs.getString("trainer_name"));
        return p;
    }

    private static WorkoutExercise mapExercise(ResultSet rs) throws SQLException {
        WorkoutExercise e = new WorkoutExercise();
        e.setExerciseId(rs.getInt("exercise_id"));
        e.setWorkoutPlanId(rs.getInt("workout_plan_id"));
        e.setExerciseName(rs.getString("exercise_name"));
        e.setSets(rs.getInt("sets_count"));
        e.setRepetitions(rs.getInt("repetitions"));
        e.setDurationMinutes(rs.getInt("duration_minutes"));
        e.setTargetMuscleGroup(rs.getString("target_muscle_group"));
        e.setScheduleDay(rs.getString("schedule_day"));
        return e;
    }
}
