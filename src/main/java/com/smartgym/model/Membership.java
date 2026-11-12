package com.smartgym.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** A plan bought (or renewed) by a member. memberName/memberCode/planName are display fields filled by joins. */
public class Membership implements Serializable {

    private static final long serialVersionUID = 1L;

    private int membershipId;
    private int memberId;
    private int planId;
    private LocalDate startDate;
    private LocalDate expiryDate;
    private String status;
    private String memberName;
    private String memberCode;
    private String planName;

    public Membership() { }

    public int getMembershipId() { return membershipId; }
    public void setMembershipId(int membershipId) { this.membershipId = membershipId; }
    public int getMemberId() { return memberId; }
    public void setMemberId(int memberId) { this.memberId = memberId; }
    public int getPlanId() { return planId; }
    public void setPlanId(int planId) { this.planId = planId; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }
    public String getMemberCode() { return memberCode; }
    public void setMemberCode(String memberCode) { this.memberCode = memberCode; }
    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }

    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(LocalDate.now());
    }

    public long getDaysLeft() {
        return ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
    }
}
