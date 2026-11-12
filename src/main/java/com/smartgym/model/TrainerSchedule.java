package com.smartgym.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;

/** A personal-training session between one trainer and one member. */
public class TrainerSchedule implements Serializable {

    private static final long serialVersionUID = 1L;

    private int scheduleId;
    private int trainerId;
    private int memberId;
    private LocalDate sessionDate;
    private LocalTime startTime;
    private int durationMinutes;
    private LocalTime endTime;
    private String status;
    private String notes;
    private String trainerName;
    private String memberName;

    public TrainerSchedule() { }

    public int getScheduleId() { return scheduleId; }
    public void setScheduleId(int scheduleId) { this.scheduleId = scheduleId; }
    public int getTrainerId() { return trainerId; }
    public void setTrainerId(int trainerId) { this.trainerId = trainerId; }
    public int getMemberId() { return memberId; }
    public void setMemberId(int memberId) { this.memberId = memberId; }
    public LocalDate getSessionDate() { return sessionDate; }
    public void setSessionDate(LocalDate sessionDate) { this.sessionDate = sessionDate; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getTrainerName() { return trainerName; }
    public void setTrainerName(String trainerName) { this.trainerName = trainerName; }
    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }

    /** End time is always start time + duration, so the two can never disagree. */
    public LocalTime getComputedEndTime() {
        return startTime == null ? null : startTime.plusMinutes(durationMinutes);
    }
}
