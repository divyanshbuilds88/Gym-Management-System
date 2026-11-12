package com.smartgym.demo;

import com.smartgym.dao.DashboardDAO;
import com.smartgym.dao.MemberDAO;
import com.smartgym.dao.UserDAO;
import com.smartgym.dao.impl.DashboardDAOImpl;
import com.smartgym.dao.impl.MemberDAOImpl;
import com.smartgym.dao.impl.UserDAOImpl;
import com.smartgym.exception.DAOException;
import com.smartgym.model.Member;
import com.smartgym.model.User;
import com.smartgym.util.DatabaseConnection;
import com.smartgym.util.PasswordUtil;

import java.time.LocalDate;
import java.util.List;

/**
 * Phase 2 console test: checks the database connection and the DAO layer.
 * Run it any time to check that MySQL and db.properties are set up correctly.
 */
public class Phase2Test {

    public static void main(String[] args) {
        System.out.println("=== Smart Gym - Phase 2 test ===");

        // 1. Connection
        if (!DatabaseConnection.testConnection()) {
            System.out.println("Fix db.properties (user/password) and make sure MySQL is running, then run again.");
            return;
        }
        System.out.println("[OK] Connected to MySQL");

        try {
            // 2. MemberDAO (PreparedStatement + joins)
            MemberDAO memberDAO = new MemberDAOImpl();
            List<Member> members = memberDAO.findAll();
            System.out.println("\n[OK] Members loaded: " + members.size());
            for (Member m : members) {
                System.out.printf("  %s | %-12s | %-10s | expires %s | %s%n",
                        m.getMemberCode(), m.getFullName(), m.getPlanName(),
                        m.getMembershipExpiry(), m.getMembershipStatus());
            }
            System.out.println("  Next member code would be: " + memberDAO.generateNextCode());
            System.out.println("  Search 'neha' finds: " + memberDAO.search("neha").size() + " member(s)");

            // 3. DashboardDAO (Statement + CallableStatement)
            DashboardDAO dash = new DashboardDAOImpl();
            LocalDate today = LocalDate.now();
            System.out.println("\n[OK] Dashboard numbers");
            System.out.println("  Total members      : " + dash.countMembers());
            System.out.println("  Active memberships : " + dash.countActiveMemberships());
            System.out.println("  Expired memberships: " + dash.countExpiredMemberships());
            System.out.println("  Trainers           : " + dash.countTrainers());
            System.out.println("  Today's attendance : " + dash.countTodayAttendance());
            System.out.println("  Revenue this month : " + dash.getMonthlyRevenue(today.getYear(), today.getMonthValue()));

            // 4. Password hashing against the sample admin account
            UserDAO userDAO = new UserDAOImpl();
            User admin = userDAO.findByUsername("admin");
            if (admin == null) {
                System.out.println("\n[FAIL] user 'admin' not found - did you run smart_gym_schema.sql?");
            } else {
                boolean good = PasswordUtil.verify("Admin@123", admin.getSalt(), admin.getPasswordHash());
                boolean bad = PasswordUtil.verify("wrong", admin.getSalt(), admin.getPasswordHash());
                System.out.println("\n[" + (good && !bad ? "OK" : "FAIL") + "] Password check for admin: "
                        + "correct password -> " + good + ", wrong password -> " + bad);
            }
            System.out.println("\n=== Phase 2 test finished ===");

        } catch (DAOException e) {
            System.out.println("\n[FAIL] " + e.getMessage());
            if (e.getCause() != null) {
                System.out.println("Cause: " + e.getCause().getMessage());
            }
        }
    }
}
