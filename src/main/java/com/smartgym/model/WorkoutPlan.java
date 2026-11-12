package com.smartgym.model;

import java.io.Serializable;

/** A workout plan template (its exercises are stored separately). */
public class WorkoutPlan implements Serializable {

    private static final long serialVersionUID = 1L;

    private int workoutPlanId;
    private String planName;
    private String goal;
    private String level;
    private String description;
    private Integer createdBy;
    private String trainerName;

    public WorkoutPlan() { }

    public int getWorkoutPlanId() { return workoutPlanId; }
    public void setWorkoutPlanId(int workoutPlanId) { this.workoutPlanId = workoutPlanId; }
    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getCreatedBy() { return createdBy; }
    public void setCreatedBy(Integer createdBy) { this.createdBy = createdBy; }
    public String getTrainerName() { return trainerName; }
    public void setTrainerName(String trainerName) { this.trainerName = trainerName; }

    @Override
    public String toString() { return planName; }
}
