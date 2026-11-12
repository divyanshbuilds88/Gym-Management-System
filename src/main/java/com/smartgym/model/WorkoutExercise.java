package com.smartgym.model;

import java.io.Serializable;

/** One exercise line inside a workout plan. */
public class WorkoutExercise implements Serializable {

    private static final long serialVersionUID = 1L;

    private int exerciseId;
    private int workoutPlanId;
    private String exerciseName;
    private int sets;
    private int repetitions;
    private int durationMinutes;
    private String targetMuscleGroup;
    private String scheduleDay;

    public WorkoutExercise() { }

    public int getExerciseId() { return exerciseId; }
    public void setExerciseId(int exerciseId) { this.exerciseId = exerciseId; }
    public int getWorkoutPlanId() { return workoutPlanId; }
    public void setWorkoutPlanId(int workoutPlanId) { this.workoutPlanId = workoutPlanId; }
    public String getExerciseName() { return exerciseName; }
    public void setExerciseName(String exerciseName) { this.exerciseName = exerciseName; }
    public int getSets() { return sets; }
    public void setSets(int sets) { this.sets = sets; }
    public int getRepetitions() { return repetitions; }
    public void setRepetitions(int repetitions) { this.repetitions = repetitions; }
    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public String getTargetMuscleGroup() { return targetMuscleGroup; }
    public void setTargetMuscleGroup(String targetMuscleGroup) { this.targetMuscleGroup = targetMuscleGroup; }
    public String getScheduleDay() { return scheduleDay; }
    public void setScheduleDay(String scheduleDay) { this.scheduleDay = scheduleDay; }
}
