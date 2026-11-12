package com.smartgym.service;

import com.smartgym.dao.DashboardDAO;
import com.smartgym.dao.MemberDAO;
import com.smartgym.dao.TrainerScheduleDAO;
import com.smartgym.dao.impl.DashboardDAOImpl;
import com.smartgym.dao.impl.MemberDAOImpl;
import com.smartgym.dao.impl.TrainerScheduleDAOImpl;
import com.smartgym.exception.DAOException;
import com.smartgym.exception.SmartGymException;
import com.smartgym.model.Role;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;

/** Loads all dashboard numbers from MySQL. */
public class DashboardService extends BaseService {

    private final DashboardDAO dashboardDAO = new DashboardDAOImpl();
    private final MemberDAO memberDAO = new MemberDAOImpl();
    private final TrainerScheduleDAO scheduleDAO = new TrainerScheduleDAOImpl();

    public DashboardData load() throws SmartGymException {
        session.requireRole(Role.ADMIN, Role.TRAINER);
        try {
            DashboardData d = new DashboardData();
            d.setTotalMembers(dashboardDAO.countMembers());
            d.setActiveMemberships(dashboardDAO.countActiveMemberships());
            d.setExpiredMemberships(dashboardDAO.countExpiredMemberships());
            d.setTotalTrainers(dashboardDAO.countTrainers());
            d.setTodayAttendance(dashboardDAO.countTodayAttendance());

            LocalDate today = LocalDate.now();
            d.setMonthlyRevenue(dashboardDAO.getMonthlyRevenue(today.getYear(), today.getMonthValue()));
            d.setUpcomingSessions(scheduleDAO.findUpcoming(5));
            d.setRecentMembers(memberDAO.findRecent(5));

            // Fill months / days that have no data with 0 so the charts have no gaps
            Map<String, Double> revenueFromDb = dashboardDAO.getMonthlyRevenueSeries(6);
            Map<String, Double> revenue = new LinkedHashMap<>();
            for (int i = 5; i >= 0; i--) {
                String key = YearMonth.now().minusMonths(i).toString();
                revenue.put(key, revenueFromDb.getOrDefault(key, 0.0));
            }
            d.setRevenueSeries(revenue);

            Map<LocalDate, Integer> visitsFromDb = dashboardDAO.getAttendanceSeries(7);
            Map<LocalDate, Integer> visits = new LinkedHashMap<>();
            for (int i = 6; i >= 0; i--) {
                LocalDate day = today.minusDays(i);
                visits.put(day, visitsFromDb.getOrDefault(day, 0));
            }
            d.setAttendanceSeries(visits);
            return d;
        } catch (DAOException e) {
            throw dbError("loading the dashboard", e);
        }
    }
}
