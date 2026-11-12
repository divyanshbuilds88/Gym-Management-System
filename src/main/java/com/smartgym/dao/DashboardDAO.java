package com.smartgym.dao;

import com.smartgym.exception.DAOException;

import java.time.LocalDate;
import java.util.Map;

/** Numbers and chart data for the dashboard and reports. */
public interface DashboardDAO {
    int countMembers() throws DAOException;

    int countActiveMemberships() throws DAOException;

    int countExpiredMemberships() throws DAOException;

    int countTrainers() throws DAOException;

    int countTodayAttendance() throws DAOException;

    /** Calls the stored procedure sp_monthly_revenue (CallableStatement). */
    double getMonthlyRevenue(int year, int month) throws DAOException;

    /** Revenue per calendar month for the last N months (including this one), oldest first, key like "2026-11". Months without payments are missing. */
    Map<String, Double> getMonthlyRevenueSeries(int months) throws DAOException;

    /** Number of check-ins per day for the last N days, oldest first. */
    Map<LocalDate, Integer> getAttendanceSeries(int days) throws DAOException;
}
