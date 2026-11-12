package com.smartgym.model;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** One gym visit. The member is still inside while checkOutTime is null. */
public class Attendance implements Serializable {

    private static final long serialVersionUID = 1L;

    private int attendanceId;
    private int memberId;
    private LocalDate attendanceDate;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private String memberName;
    private String memberCode;

    public Attendance() { }

    public int getAttendanceId() { return attendanceId; }
    public void setAttendanceId(int attendanceId) { this.attendanceId = attendanceId; }
    public int getMemberId() { return memberId; }
    public void setMemberId(int memberId) { this.memberId = memberId; }
    public LocalDate getAttendanceDate() { return attendanceDate; }
    public void setAttendanceDate(LocalDate attendanceDate) { this.attendanceDate = attendanceDate; }
    public LocalDateTime getCheckInTime() { return checkInTime; }
    public void setCheckInTime(LocalDateTime checkInTime) { this.checkInTime = checkInTime; }
    public LocalDateTime getCheckOutTime() { return checkOutTime; }
    public void setCheckOutTime(LocalDateTime checkOutTime) { this.checkOutTime = checkOutTime; }
    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }
    public String getMemberCode() { return memberCode; }
    public void setMemberCode(String memberCode) { this.memberCode = memberCode; }

    public boolean isCheckedOut() { return checkOutTime != null; }

    /** Minutes spent in the gym, or -1 if the member has not checked out yet. */
    public long getDurationMinutes() {
        if (checkInTime == null || checkOutTime == null) {
            return -1;
        }
        return Duration.between(checkInTime, checkOutTime).toMinutes();
    }
}
