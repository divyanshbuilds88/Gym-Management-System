package com.smartgym.dao;

import com.smartgym.exception.DAOException;
import com.smartgym.model.WorkoutExercise;
import com.smartgym.model.WorkoutPlan;

import java.util.List;

public interface WorkoutPlanDAO extends CrudDAO<WorkoutPlan> {

    /** Saves a plan and all its exercises in ONE transaction (all or nothing). */
    int insertWithExercises(WorkoutPlan plan, List<WorkoutExercise> exercises) throws DAOException;

    List<WorkoutExercise> findExercises(int workoutPlanId) throws DAOException;

    WorkoutExercise findExerciseById(int exerciseId) throws DAOException;

    int addExercise(WorkoutExercise exercise) throws DAOException;

    boolean updateExercise(WorkoutExercise exercise) throws DAOException;

    boolean deleteExercise(int exerciseId) throws DAOException;
}
