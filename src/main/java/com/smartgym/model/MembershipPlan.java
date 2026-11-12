package com.smartgym.model;

import java.io.Serializable;

/** A membership plan such as Monthly or Annual. */
public class MembershipPlan implements Serializable {

    private static final long serialVersionUID = 1L;

    private int planId;
    private String planName;
    private int durationMonths;
    private double fee;
    private String description;
    private boolean active;

    public MembershipPlan() { }

    public MembershipPlan(String planName, int durationMonths, double fee, String description) {
        this.planName = planName;
        this.durationMonths = durationMonths;
        this.fee = fee;
        this.description = description;
    }

    public int getPlanId() { return planId; }
    public void setPlanId(int planId) { this.planId = planId; }
    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
    public int getDurationMonths() { return durationMonths; }
    public void setDurationMonths(int durationMonths) { this.durationMonths = durationMonths; }
    public double getFee() { return fee; }
    public void setFee(double fee) { this.fee = fee; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public String toString() { return planName; }
}
