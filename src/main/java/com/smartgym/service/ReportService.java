package com.smartgym.service;

import com.smartgym.dao.AttendanceDAO;
import com.smartgym.dao.DashboardDAO;
import com.smartgym.dao.MembershipDAO;
import com.smartgym.dao.PaymentDAO;
import com.smartgym.dao.TrainerScheduleDAO;
import com.smartgym.dao.impl.AttendanceDAOImpl;
import com.smartgym.dao.impl.DashboardDAOImpl;
import com.smartgym.dao.impl.MembershipDAOImpl;
import com.smartgym.dao.impl.PaymentDAOImpl;
import com.smartgym.dao.impl.TrainerScheduleDAOImpl;
import com.smartgym.exception.DAOException;
import com.smartgym.exception.InvalidInputException;
import com.smartgym.exception.SmartGymException;
import com.smartgym.model.Attendance;
import com.smartgym.model.Membership;
import com.smartgym.model.Payment;
import com.smartgym.model.Role;
import com.smartgym.model.TrainerSchedule;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/** Builds the six reports and exports any of them to CSV. */
public class ReportService extends BaseService {

    private final DashboardDAO dashboardDAO = new DashboardDAOImpl();
    private final MembershipDAO membershipDAO = new MembershipDAOImpl();
    private final AttendanceDAO attendanceDAO = new AttendanceDAOImpl();
    private final TrainerScheduleDAO scheduleDAO = new TrainerScheduleDAOImpl();
    private final PaymentDAO paymentDAO = new PaymentDAOImpl();
    private final MemberService memberService = new MemberService();

    /** Report 1: total members and membership counts. */
    public ReportTable memberSummaryReport() throws SmartGymException {
        session.requireRole(Role.ADMIN, Role.TRAINER);
        try {
            ReportTable t = new ReportTable("Member Summary", "Measure", "Value");
            t.addRow("Total members", String.valueOf(dashboardDAO.countMembers()));
            t.addRow("Members with active membership", String.valueOf(dashboardDAO.countActiveMemberships()));
            t.addRow("Members with expired membership", String.valueOf(dashboardDAO.countExpiredMemberships()));
            t.addRow("Total trainers", String.valueOf(dashboardDAO.countTrainers()));
            t.addRow("Today's attendance", String.valueOf(dashboardDAO.countTodayAttendance()));
            return t;
        } catch (DAOException e) {
            throw dbError("building the member summary", e);
        }
    }

    /** Report 2: every membership with its status (active / expired / cancelled). */
    public ReportTable membershipStatusReport() throws SmartGymException {
        session.requireRole(Role.ADMIN, Role.TRAINER);
        try {
            ReportTable t = new ReportTable("Membership Status", "Code", "Member", "Plan", "Start", "Expiry", "Status");
            for (Membership m : membershipDAO.findAll()) {
                t.addRow(m.getMemberCode(), m.getMemberName(), m.getPlanName(),
                        String.valueOf(m.getStartDate()), String.valueOf(m.getExpiryDate()), m.getStatus());
            }
            return t;
        } catch (DAOException e) {
            throw dbError("building the membership report", e);
        }
    }

    /** Report 3: who visited on a given day. */
    public ReportTable dailyAttendanceReport(LocalDate date) throws SmartGymException {
        session.requireRole(Role.ADMIN, Role.TRAINER);
        if (date == null) {
            throw new InvalidInputException("Please choose a date.");
        }
        try {
            ReportTable t = new ReportTable("Attendance on " + date, "Code", "Member", "Check-in", "Check-out", "Minutes");
            for (Attendance a : attendanceDAO.findByDate(date)) {
                t.addRow(a.getMemberCode(), a.getMemberName(),
                        String.valueOf(a.getCheckInTime().toLocalTime()),
                        a.getCheckOutTime() == null ? "still inside" : String.valueOf(a.getCheckOutTime().toLocalTime()),
                        a.getDurationMinutes() < 0 ? "-" : String.valueOf(a.getDurationMinutes()));
            }
            return t;
        } catch (DAOException e) {
            throw dbError("building the attendance report", e);
        }
    }

    /** Report 4: revenue for each of the last N months (uses the stored procedure). */
    public ReportTable monthlyRevenueReport(int months) throws SmartGymException {
        session.requireRole(Role.ADMIN);
        if (months < 1 || months > 24) {
            throw new InvalidInputException("Choose between 1 and 24 months.");
        }
        try {
            ReportTable t = new ReportTable("Monthly Revenue (PAID payments)", "Month", "Revenue (Rs.)");
            double total = 0;
            YearMonth current = YearMonth.now();
            for (int i = months - 1; i >= 0; i--) {                      // oldest month first
                YearMonth ym = current.minusMonths(i);
                double revenue = dashboardDAO.getMonthlyRevenue(ym.getYear(), ym.getMonthValue());
                total += revenue;
                t.addRow(ym.toString(), String.format("%.2f", revenue));
            }
            t.addRow("TOTAL", String.format("%.2f", total));
            return t;
        } catch (DAOException e) {
            throw dbError("building the revenue report", e);
        }
    }

    /** Report 5: trainer sessions. trainerId = null means all trainers. */
    public ReportTable trainerScheduleReport(Integer trainerId) throws SmartGymException {
        session.requireRole(Role.ADMIN, Role.TRAINER);
        try {
            List<TrainerSchedule> list = (trainerId == null) ? scheduleDAO.findAll() : scheduleDAO.findByTrainer(trainerId);
            ReportTable t = new ReportTable("Trainer Schedules", "Trainer", "Member", "Date", "Start", "End", "Status");
            for (TrainerSchedule s : list) {
                t.addRow(s.getTrainerName(), s.getMemberName(), String.valueOf(s.getSessionDate()),
                        String.valueOf(s.getStartTime()), String.valueOf(s.getEndTime()), s.getStatus());
            }
            return t;
        } catch (DAOException e) {
            throw dbError("building the schedule report", e);
        }
    }

    /** Report 6: all payments of one member. */
    public ReportTable memberPaymentHistoryReport(int memberId) throws SmartGymException {
        if (!session.isOwnMemberId(memberId)) {
            session.requireRole(Role.ADMIN);
        }
        try {
            ReportTable t = new ReportTable("Payment History", "Receipt", "Date", "Amount (Rs.)", "Method", "Status", "Notes");
            for (Payment p : paymentDAO.findByMember(memberId)) {
                t.addRow(p.getReceiptNo(), String.valueOf(p.getPaymentDate()), String.format("%.2f", p.getAmount()),
                        p.getMethod(), p.getStatus(), p.getNotes() == null ? "" : p.getNotes());
            }
            return t;
        } catch (DAOException e) {
            throw dbError("building the payment history", e);
        }
    }

    // ================================================================ CSV export (file handling)

    /** Converts a report to CSV text. Cells containing commas, quotes or line breaks are put in quotes. */
    public static String toCsv(ReportTable table) {
        StringBuilder sb = new StringBuilder();
        appendCsvLine(sb, table.getHeaders());
        for (String[] row : table.getRows()) {
            appendCsvLine(sb, row);
        }
        return sb.toString();
    }

    private static void appendCsvLine(StringBuilder sb, String[] cells) {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escapeCsv(cells[i]));
        }
        sb.append("\r\n");
    }

    private static String escapeCsv(String cell) {
        String text = (cell == null) ? "" : cell;
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    /** FILE WRITING: saves the report as a .csv file (opens in Excel). */
    public void exportToCsv(ReportTable table, File file) throws SmartGymException {
        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            writer.write('\uFEFF');                                   // byte-order mark so Excel reads UTF-8 correctly
            writer.write(toCsv(table));
        } catch (IOException e) {
            throw new SmartGymException("Could not write the CSV file: " + e.getMessage(), e);
        }
    }
}
