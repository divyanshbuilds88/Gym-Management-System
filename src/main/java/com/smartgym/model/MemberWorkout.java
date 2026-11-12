package com.smartgym.model;

import java.io.Serializable;
import java.time.LocalDate;

/** A workout plan assigned to a member by a trainer. */
public class MemberWorkout implements Serializable {

    private static final long serialVersionUID = 1L;

    private int assignmentId;
    private int memberId;
    private int workoutPlanId;
    private int trainerId;
    private LocalDate assignedDate;
    private LocalDate endDate;
    private String status;
    private String memberName;
    private String planName;
    private String trainerName;

    public MemberWorkout() { }

    public int getAssignmentId() { return assignmentId; }
    public void setAssignmentId(int assignmentId) { this.assignmentId = assignmentId; }
    public int getMemberId() { return memberId; }
    public void setMemberId(int memberId) { this.memberId = memberId; }
    public int getWorkoutPlanId() { return workoutPlanId; }
    public void setWorkoutPlanId(int workoutPlanId) { this.workoutPlanId = workoutPlanId; }
    public int getTrainerId() { return trainerId; }
    public void setTrainerId(int trainerId) { this.trainerId = trainerId; }
    public LocalDate getAssignedDate() { return assignedDate; }
    public void setAssignedDate(LocalDate assignedDate) { this.assignedDate = assignedDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }
    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
    public String getTrainerName() { return trainerName; }
    public void setTrainerName(String trainerName) { this.trainerName = trainerName; }
}
