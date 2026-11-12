package com.smartgym.demo;

import com.smartgym.exception.AttendanceException;
import com.smartgym.exception.AuthenticationException;
import com.smartgym.exception.InvalidInputException;
import com.smartgym.exception.InvalidMemberException;
import com.smartgym.exception.InvalidPaymentException;
import com.smartgym.exception.MembershipExpiredException;
import com.smartgym.exception.SmartGymException;
import com.smartgym.exception.TrainerScheduleConflictException;
import com.smartgym.exception.UnauthorizedException;
import com.smartgym.model.Attendance;
import com.smartgym.model.Member;
import com.smartgym.model.MembershipPlan;
import com.smartgym.model.Membership;
import com.smartgym.model.Payment;
import com.smartgym.model.Trainer;
import com.smartgym.model.TrainerSchedule;
import com.smartgym.model.WorkoutExercise;
import com.smartgym.model.WorkoutPlan;
import com.smartgym.service.AttendanceService;
import com.smartgym.service.AuthService;
import com.smartgym.service.DashboardData;
import com.smartgym.service.DashboardService;
import com.smartgym.service.MemberService;
import com.smartgym.service.MembershipService;
import com.smartgym.service.PaymentService;
import com.smartgym.service.ProfileService;
import com.smartgym.service.ReportService;
import com.smartgym.service.ReportTable;
import com.smartgym.service.SchedulingService;
import com.smartgym.service.TrainerService;
import com.smartgym.service.WorkoutService;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Phase 3 self-test: runs every service against the real MySQL database.
 * It creates test data (phone 9876500001, user phase3_user ...) and DELETES it again at the end.
 * Run:  com.smartgym.demo.Phase3Test
 */
public class Phase3Test {

    /** A piece of code that may throw (used to test that the right exception happens). */
    interface Action {
        void run() throws Exception;
    }

    private static int passed = 0;
    private static int failed = 0;

    private static final AuthService auth = new AuthService();
    private static final MemberService members = new MemberService();
    private static final MembershipService memberships = new MembershipService();
    private static final PaymentService payments = new PaymentService();
    private static final TrainerService trainers = new TrainerService();
    private static final WorkoutService workouts = new WorkoutService();
    private static final SchedulingService scheduling = new SchedulingService();
    private static final AttendanceService attendance = new AttendanceService();
    private static final ReportService reports = new ReportService();
    private static final DashboardService dashboard = new DashboardService();
    private static final ProfileService profile = new ProfileService();

    private static int testMemberId;
    private static int workoutPlanId;

    // ------------------------------------------------------------------ tiny test helpers

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  [PASS] " + name);
        } else {
            failed++;
            System.out.println("  [FAIL] " + name);
        }
    }

    /** Passes when 'action' throws an exception of the given type. */
    private static void expect(String name, Class<? extends Exception> type, Action action) {
        try {
            action.run();
            failed++;
            System.out.println("  [FAIL] " + name + "  (no exception thrown, expected " + type.getSimpleName() + ")");
        } catch (Exception e) {
            if (type.isInstance(e)) {
                passed++;
                System.out.println("  [PASS] " + name + "  -> " + type.getSimpleName() + ": " + e.getMessage());
            } else {
                failed++;
                System.out.println("  [FAIL] " + name + "  (got " + e.getClass().getSimpleName() + ": " + e.getMessage() + ")");
            }
        }
    }

    /** Passes when 'action' finishes without any exception. */
    private static void works(String name, Action action) {
        try {
            action.run();
            passed++;
            System.out.println("  [PASS] " + name);
        } catch (Exception e) {
            failed++;
            System.out.println("  [FAIL] " + name + "  (" + e.getClass().getSimpleName() + ": " + e.getMessage() + ")");
        }
    }

    private static void section(String title) {
        System.out.println("\n--- " + title + " ---");
    }

    private static Member newMember(String name, int age, String phone, String email) {
        return new Member(name, age, "MALE", phone, email, "Test Street, Ludhiana", LocalDate.now());
    }

    private static TrainerSchedule newSession(int trainerId, int memberId, LocalDate date, LocalTime start, int minutes) {
        TrainerSchedule s = new TrainerSchedule();
        s.setTrainerId(trainerId);
        s.setMemberId(memberId);
        s.setSessionDate(date);
        s.setStartTime(start);
        s.setDurationMinutes(minutes);
        s.setNotes("Phase 3 test");
        return s;
    }

    // ------------------------------------------------------------------ main

    public static void main(String[] args) {
        System.out.println("=== Smart Gym - Phase 3 service test ===");
        try {
            run();
        } catch (Exception e) {
            failed++;
            System.out.println("\n[ABORTED] Unexpected error: " + e);
            e.printStackTrace(System.out);
        } finally {
            try {
                cleanUp();
            } catch (Exception e) {
                System.out.println("[WARN] Clean-up problem: " + e.getMessage());
            }
        }
        System.out.println("\n=== RESULT: " + passed + " passed, " + failed + " failed ===");
        System.exit(failed == 0 ? 0 : 1);
    }

    private static void run() throws Exception {
        auth.login("admin", "Admin@123");
        removeLeftovers();                                          // in case an earlier run stopped half-way

        // ============================================================== 1
        section("1. Authentication");
        auth.logout();
        expect("wrong password is rejected", AuthenticationException.class, () -> auth.login("admin", "wrong"));
        expect("unknown user is rejected", AuthenticationException.class, () -> auth.login("nobody", "x1y2z3"));
        expect("blank fields are rejected", AuthenticationException.class, () -> auth.login("  ", ""));
        works("admin can log in", () -> auth.login("admin", "Admin@123"));

        // ============================================================== 2
        section("2. Role-based access");
        auth.logout();
        auth.login("member1", "Member@123");
        expect("member cannot list all members", UnauthorizedException.class, () -> members.getAllMembers());
        expect("member cannot add a member", UnauthorizedException.class,
                () -> members.addMember(newMember("Hacker Boy", 20, "9876500009", "h@example.com")));
        check("member sees OWN payments", !payments.getPaymentsOfMember(1).isEmpty());
        expect("member cannot see ANOTHER member's payments", UnauthorizedException.class,
                () -> payments.getPaymentsOfMember(2));
        auth.logout();
        auth.login("trainer1", "Trainer@123");
        expect("trainer cannot delete a member", UnauthorizedException.class, () -> members.deleteMember(1));
        expect("trainer cannot see all payments", UnauthorizedException.class, () -> payments.getAllPayments());
        expect("trainer cannot book another trainer's session", UnauthorizedException.class,
                () -> scheduling.scheduleSession(newSession(2, 1, LocalDate.now().plusDays(3), LocalTime.of(9, 0), 60)));
        check("trainer can read the trainer list", trainers.getAllTrainers().size() >= 3);
        auth.logout();
        expect("nothing works when logged out", UnauthorizedException.class, () -> members.getAllMembers());
        auth.login("admin", "Admin@123");

        // ============================================================== 3
        section("3. Members");
        expect("bad name rejected", InvalidMemberException.class,
                () -> members.addMember(newMember("X1", 20, "9876500001", "phase3.test@example.com")));
        expect("bad phone rejected", InvalidMemberException.class,
                () -> members.addMember(newMember("Phase Tester", 20, "12345", "phase3.test@example.com")));
        expect("bad email rejected", InvalidMemberException.class,
                () -> members.addMember(newMember("Phase Tester", 20, "9876500001", "not-an-email")));
        expect("age 5 rejected", InvalidMemberException.class,
                () -> members.addMember(newMember("Phase Tester", 5, "9876500001", "phase3.test@example.com")));
        expect("weak password rejected", InvalidMemberException.class,
                () -> members.addMember(newMember("Phase Tester", 22, "9876500001", "phase3.test@example.com"), "phase3_user", "abc"));

        Member m = newMember("Phase Tester", 22, "9876500001", "phase3.test@example.com");
        testMemberId = members.addMember(m, "phase3_user", "Phase@123");
        check("member added, code generated: " + m.getMemberCode(), testMemberId > 0 && m.getMemberCode().startsWith("GYM"));
        expect("duplicate phone rejected", InvalidMemberException.class,
                () -> members.addMember(newMember("Another Person", 30, "9876500001", "other@example.com")));
        check("member can be found by search", members.searchMembers("Phase Tester").size() == 1);

        Member loaded = members.getMember(testMemberId);
        loaded.setAddress("New Address, Model Town");
        members.updateMember(loaded);
        check("member update saved", "New Address, Model Town".equals(members.getMember(testMemberId).getAddress()));
        check("new member has no membership yet", "NONE".equals(members.getMember(testMemberId).getMembershipStatus()));

        auth.logout();
        works("new member can log in with the chosen password", () -> auth.login("phase3_user", "Phase@123"));
        auth.logout();
        auth.login("admin", "Admin@123");

        // ============================================================== 4
        section("4. Membership plans, memberships, payments");
        expect("check-in without membership rejected", MembershipExpiredException.class,
                () -> attendance.checkIn(testMemberId));
        expect("booking a session without membership rejected", MembershipExpiredException.class,
                () -> scheduling.scheduleSession(newSession(1, testMemberId, LocalDate.now().plusDays(1), LocalTime.of(10, 0), 60)));
        check("4 active plans exist", memberships.getActivePlans().size() == 4);

        Payment first = memberships.assignMembership(testMemberId, 1, LocalDate.now(), "CASH", true);
        check("monthly plan bought, receipt " + first.getReceiptNo() + ", amount " + first.getAmount(),
                first.getAmount() == 1500.0 && "PAID".equals(first.getStatus()) && first.getReceiptNo().matches("RCP-\\d{4}"));
        Member afterBuy = members.getMember(testMemberId);
        check("member now ACTIVE on Monthly", "ACTIVE".equals(afterBuy.getMembershipStatus()) && "Monthly".equals(afterBuy.getPlanName()));

        Payment second = memberships.renewMembership(testMemberId, 2, "UPI", true);
        List<Membership> history = memberships.getMembershipsOfMember(testMemberId);
        check("renewal creates a 2nd membership", history.size() == 2);
        check("renewal starts when the first one ends",
                history.get(0).getStartDate().equals(history.get(1).getExpiryDate()));
        check("quarterly expiry = start + 3 months",
                history.get(0).getExpiryDate().equals(history.get(0).getStartDate().plusMonths(3)));

        Payment pending = payments.recordPayment(testMemberId, null, 500, "CARD", "PENDING", "Locker fee");
        check("pending payment listed", payments.getPendingPayments().stream().anyMatch(p -> p.getPaymentId() == pending.getPaymentId()));
        works("pending payment marked PAID", () -> payments.markAsPaid(pending.getPaymentId()));
        expect("paying twice rejected", InvalidPaymentException.class, () -> payments.markAsPaid(pending.getPaymentId()));
        expect("negative amount rejected", InvalidPaymentException.class,
                () -> payments.recordPayment(testMemberId, null, -5, "CASH", "PAID", null));
        expect("bad method rejected", InvalidPaymentException.class,
                () -> payments.recordPayment(testMemberId, null, 100, "BITCOIN", "PAID", null));
        List<Payment> memberPayments = payments.getPaymentsOfMember(testMemberId);
        check("total paid = 1500 + 4000 + 500", PaymentService.totalPaid(memberPayments) == 6000.0);

        String receipt = payments.buildReceiptText(second);
        check("receipt text contains name and receipt number",
                receipt.contains("Phase Tester") && receipt.contains(second.getReceiptNo()));
        File receiptFile = File.createTempFile("receipt", ".txt");
        payments.saveReceiptToFile(second, receiptFile);
        check("receipt file written", receiptFile.length() > 100);
        receiptFile.delete();

        expect("expired member cannot check in", MembershipExpiredException.class, () -> attendance.checkIn(3));
        check("expire-overdue procedure runs", memberships.expireOverdueMemberships() >= 0);

        // plans
        MembershipPlan plan = new MembershipPlan("Phase3 Plan", 2, 999.0, "test plan");
        plan.setActive(true);
        int planId = memberships.addPlan(plan);
        check("new plan added", planId > 0);
        expect("duplicate plan name rejected", InvalidInputException.class, () -> memberships.addPlan(new MembershipPlan("Phase3 Plan", 2, 999.0, "dup")));
        expect("plan fee 0 rejected", InvalidInputException.class, () -> memberships.addPlan(new MembershipPlan("Free Plan", 1, 0, "x")));
        works("unused plan deleted", () -> memberships.deletePlan(planId));
        expect("plan in use cannot be deleted", InvalidInputException.class, () -> memberships.deletePlan(1));

        // ============================================================== 5
        section("5. Attendance");
        Attendance in = attendance.checkIn(testMemberId);
        check("check-in saved", in.getAttendanceId() > 0);
        expect("duplicate check-in rejected", AttendanceException.class, () -> attendance.checkIn(testMemberId));
        Attendance out = attendance.checkOut(testMemberId);
        check("check-out saved, duration " + out.getDurationMinutes() + " min", out.isCheckedOut() && out.getDurationMinutes() >= 0);
        expect("check-out when not inside rejected", AttendanceException.class, () -> attendance.checkOut(testMemberId));
        check("member appears in today's attendance",
                attendance.getTodayAttendance().stream().anyMatch(a -> a.getMemberId() == testMemberId));
        check("attendance history has 1 visit", attendance.getHistory(testMemberId).size() == 1);
        check("attendance search by name works", !attendance.searchByMember("Phase").isEmpty());
        expect("bad date range rejected", AttendanceException.class,
                () -> attendance.getAttendanceBetween(LocalDate.now(), LocalDate.now().minusDays(2)));
        attendance.checkIn(testMemberId);                              // leave one open visit; removed with the member

        // ============================================================== 6
        section("6. Trainer scheduling");
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        TrainerSchedule s1 = newSession(1, testMemberId, tomorrow, LocalTime.of(10, 0), 60);
        int s1Id = scheduling.scheduleSession(s1);
        check("session 10:00-11:00 booked", s1Id > 0);
        expect("overlapping session (10:30) rejected", TrainerScheduleConflictException.class,
                () -> scheduling.scheduleSession(newSession(1, testMemberId, tomorrow, LocalTime.of(10, 30), 60)));
        expect("same member double-booked with another trainer rejected", TrainerScheduleConflictException.class,
                () -> scheduling.scheduleSession(newSession(2, testMemberId, tomorrow, LocalTime.of(10, 15), 30)));
        works("back-to-back session (11:00) allowed",
                () -> scheduling.scheduleSession(newSession(1, testMemberId, tomorrow, LocalTime.of(11, 0), 45)));
        expect("session in the past rejected", InvalidInputException.class,
                () -> scheduling.scheduleSession(newSession(1, testMemberId, LocalDate.now().minusDays(1), LocalTime.of(10, 0), 60)));
        expect("session outside gym hours rejected", InvalidInputException.class,
                () -> scheduling.scheduleSession(newSession(1, testMemberId, tomorrow, LocalTime.of(21, 30), 60)));
        expect("expired member cannot book", MembershipExpiredException.class,
                () -> scheduling.scheduleSession(newSession(1, 3, tomorrow, LocalTime.of(15, 0), 60)));
        works("first session cancelled", () -> scheduling.cancelSession(s1Id));
        works("cancelled slot can be booked again",
                () -> scheduling.scheduleSession(newSession(1, testMemberId, tomorrow, LocalTime.of(10, 0), 60)));
        expect("cancelled session cannot be cancelled again", InvalidInputException.class, () -> scheduling.cancelSession(s1Id));
        check("trainer timetable lists sessions", scheduling.getTimetable(1).size() >= 3);
        check("member sessions listed", scheduling.getSessionsOfMember(testMemberId).size() == 3);

        // ============================================================== 7
        section("7. Workout plans");
        WorkoutPlan wp = new WorkoutPlan();
        wp.setPlanName("Phase3 Test Plan");
        wp.setGoal("Testing");
        wp.setLevel("BEGINNER");
        wp.setDescription("created by test");
        wp.setCreatedBy(1);
        List<WorkoutExercise> list = new ArrayList<>();
        list.add(exercise("Push Up", 3, 12, "Chest", "MON"));
        list.add(exercise("Squat", 4, 10, "Legs", "WED"));
        workoutPlanId = workouts.createPlan(wp, list);
        check("plan with 2 exercises saved", workoutPlanId > 0 && workouts.getExercises(workoutPlanId).size() == 2);
        List<WorkoutExercise> bad = new ArrayList<>();
        bad.add(exercise("Bad Move", 0, 10, "Legs", "MON"));
        WorkoutPlan badPlan = new WorkoutPlan();
        badPlan.setPlanName("Phase3 Bad Plan");
        badPlan.setGoal("Testing");
        badPlan.setLevel("BEGINNER");
        expect("exercise with 0 sets rejected", InvalidInputException.class,
                () -> workouts.createPlan(badPlan, bad));
        expect("plan without exercises rejected", InvalidInputException.class,
                () -> workouts.createPlan(badPlan, new ArrayList<>()));
        int assignmentId = workouts.assignToMember(testMemberId, workoutPlanId, 1, LocalDate.now().plusDays(30));
        check("plan assigned to member", assignmentId > 0 && workouts.getAssignmentsOfMember(testMemberId).size() == 1);
        expect("same plan cannot be assigned twice", InvalidInputException.class,
                () -> workouts.assignToMember(testMemberId, workoutPlanId, 1, null));
        works("assignment marked COMPLETED", () -> workouts.updateAssignmentStatus(assignmentId, "COMPLETED"));

        // ============================================================== 8
        section("8. Trainers");
        Trainer bt = new Trainer("Phase Coach", "9876500002", "phase3.trainer@example.com", "Yoga", "Mon-Fri 07:00-12:00", -5, 2);
        expect("negative salary rejected", InvalidInputException.class, () -> trainers.addTrainer(bt));
        Trainer good = new Trainer("Phase Coach", "9876500002", "phase3.trainer@example.com", "Yoga", "Mon-Fri 07:00-12:00", 25000, 2);
        int trainerId = trainers.addTrainer(good, "phase3_coach", "Coach@123");
        check("trainer + login created", trainerId > 0);
        check("trainer search works", trainers.searchTrainers("Phase Coach").size() == 1);
        works("trainer deleted", () -> trainers.deleteTrainer(trainerId));
        expect("trainer with assigned workouts cannot be deleted", InvalidInputException.class, () -> trainers.deleteTrainer(1));

        // ============================================================== 9
        section("9. Reports and CSV");
        check("member summary has 5 rows", reports.memberSummaryReport().getRowCount() == 5);
        ReportTable revenue = reports.monthlyRevenueReport(3);
        check("revenue report: 3 months + total row", revenue.getRowCount() == 4);
        check("membership report not empty", reports.membershipStatusReport().getRowCount() >= 5);
        check("today's attendance report has our member", reports.dailyAttendanceReport(LocalDate.now()).getRowCount() >= 1);
        check("trainer schedule report not empty", reports.trainerScheduleReport(null).getRowCount() >= 3);
        check("payment history report has 3 payments", reports.memberPaymentHistoryReport(testMemberId).getRowCount() == 3);
        ReportTable tricky = new ReportTable("Tricky", "A", "B");
        tricky.addRow("x,y", "say \"hi\"");
        check("CSV escaping", ReportService.toCsv(tricky).equals("A,B\r\n\"x,y\",\"say \"\"hi\"\"\"\r\n"));
        expect("row with wrong number of cells rejected", IllegalArgumentException.class, () -> tricky.addRow("only one"));
        File csv = File.createTempFile("report", ".csv");
        reports.exportToCsv(revenue, csv);
        List<String> lines = Files.readAllLines(csv.toPath());
        check("CSV file exported (" + lines.size() + " lines)", lines.size() == 5 && lines.get(0).endsWith("Month,Revenue (Rs.)"));
        csv.delete();

        // ============================================================== 10
        section("10. Dashboard");
        DashboardData d = dashboard.load();
        check("dashboard totals match member list", d.getTotalMembers() == members.getAllMembers().size());
        check("dashboard charts filled (6 months, 7 days)", d.getRevenueSeries().size() == 6 && d.getAttendanceSeries().size() == 7);
        check("upcoming sessions loaded", !d.getUpcomingSessions().isEmpty());
        check("recent members loaded", d.getRecentMembers().size() == 5);
        System.out.println("  (members " + d.getTotalMembers() + ", active " + d.getActiveMemberships()
                + ", expired " + d.getExpiredMemberships() + ", today " + d.getTodayAttendance()
                + ", revenue this month " + d.getMonthlyRevenue() + ")");

        // ============================================================== 11
        section("11. Profile");
        auth.logout();
        auth.login("phase3_user", "Phase@123");
        check("profile is a Member object (polymorphism)", profile.getProfile() instanceof Member);
        works("contact details updated", () -> profile.updateContact("9876500003", "phase3.new@example.com", "Another Road"));
        check("update visible in profile", "9876500003".equals(profile.getProfile().getPhone()));
        expect("invalid phone rejected", InvalidInputException.class, () -> profile.updateContact("123", "a@b.co", "x"));
        expect("wrong old password rejected", AuthenticationException.class, () -> profile.changePassword("wrong1", "Newer@123", "Newer@123"));
        expect("weak new password rejected", InvalidInputException.class, () -> profile.changePassword("Phase@123", "abc", "abc"));
        expect("confirmation mismatch rejected", InvalidInputException.class, () -> profile.changePassword("Phase@123", "Newer@123", "Newer@124"));
        works("password changed", () -> profile.changePassword("Phase@123", "Newer@123", "Newer@123"));
        auth.logout();
        expect("old password no longer works", AuthenticationException.class, () -> auth.login("phase3_user", "Phase@123"));
        works("new password works", () -> auth.login("phase3_user", "Newer@123"));
        auth.logout();
        auth.login("admin", "Admin@123");
    }

    private static WorkoutExercise exercise(String name, int sets, int reps, String muscle, String day) {
        WorkoutExercise e = new WorkoutExercise();
        e.setExerciseName(name);
        e.setSets(sets);
        e.setRepetitions(reps);
        e.setDurationMinutes(15);
        e.setTargetMuscleGroup(muscle);
        e.setScheduleDay(day);
        return e;
    }

    // ------------------------------------------------------------------ clean-up

    private static void removeLeftovers() throws SmartGymException {
        for (Member old : members.searchMembers("9876500")) {
            members.deleteMember(old.getMemberId());
        }
        for (WorkoutPlan p : workouts.getAllPlans()) {
            if ("Phase3 Test Plan".equals(p.getPlanName())) {
                workouts.deletePlan(p.getWorkoutPlanId());
            }
        }
        for (Trainer t : trainers.searchTrainers("Phase Coach")) {
            trainers.deleteTrainer(t.getTrainerId());
        }
    }

    private static void cleanUp() throws SmartGymException {
        section("Clean-up");
        auth.logout();
        auth.login("admin", "Admin@123");
        removeLeftovers();
        check("test data removed", members.searchMembers("9876500").isEmpty());
        auth.logout();
    }
}
