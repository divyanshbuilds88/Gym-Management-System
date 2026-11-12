package com.smartgym.service;

import com.smartgym.model.Member;
import com.smartgym.model.TrainerSchedule;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Everything the dashboard needs, loaded in one go. */
public class DashboardData {

    private int totalMembers;
    private int activeMemberships;
    private int expiredMemberships;
    private int totalTrainers;
    private int todayAttendance;
    private double monthlyRevenue;
    private List<TrainerSchedule> upcomingSessions;
    private List<Member> recentMembers;
    private Map<String, Double> revenueSeries;          // "2026-11" -> amount
    private Map<LocalDate, Integer> attendanceSeries;   // date -> visits

    public int getTotalMembers() { return totalMembers; }
    public void setTotalMembers(int totalMembers) { this.totalMembers = totalMembers; }
    public int getActiveMemberships() { return activeMemberships; }
    public void setActiveMemberships(int activeMemberships) { this.activeMemberships = activeMemberships; }
    public int getExpiredMemberships() { return expiredMemberships; }
    public void setExpiredMemberships(int expiredMemberships) { this.expiredMemberships = expiredMemberships; }
    public int getTotalTrainers() { return totalTrainers; }
    public void setTotalTrainers(int totalTrainers) { this.totalTrainers = totalTrainers; }
    public int getTodayAttendance() { return todayAttendance; }
    public void setTodayAttendance(int todayAttendance) { this.todayAttendance = todayAttendance; }
    public double getMonthlyRevenue() { return monthlyRevenue; }
    public void setMonthlyRevenue(double monthlyRevenue) { this.monthlyRevenue = monthlyRevenue; }
    public List<TrainerSchedule> getUpcomingSessions() { return upcomingSessions; }
    public void setUpcomingSessions(List<TrainerSchedule> upcomingSessions) { this.upcomingSessions = upcomingSessions; }
    public List<Member> getRecentMembers() { return recentMembers; }
    public void setRecentMembers(List<Member> recentMembers) { this.recentMembers = recentMembers; }
    public Map<String, Double> getRevenueSeries() { return revenueSeries; }
    public void setRevenueSeries(Map<String, Double> revenueSeries) { this.revenueSeries = revenueSeries; }
    public Map<LocalDate, Integer> getAttendanceSeries() { return attendanceSeries; }
    public void setAttendanceSeries(Map<LocalDate, Integer> attendanceSeries) { this.attendanceSeries = attendanceSeries; }
}
