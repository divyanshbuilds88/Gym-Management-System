package com.smartgym.service;

import com.smartgym.dao.AttendanceDAO;
import com.smartgym.dao.impl.AttendanceDAOImpl;
import com.smartgym.exception.AttendanceException;
import com.smartgym.exception.DAOException;
import com.smartgym.exception.SmartGymException;
import com.smartgym.model.Attendance;
import com.smartgym.model.Member;
import com.smartgym.model.Role;
import com.smartgym.util.ActivityLog;
import com.smartgym.util.DatabaseConnection;
import com.smartgym.util.DbUtil;
import com.smartgym.util.ValidationUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Check-in / check-out with duplicate prevention. */
public class AttendanceService extends BaseService {

    /** Makes check-in/out one-at-a-time inside this program (SYNCHRONIZATION). */
    private static final Object ATTENDANCE_LOCK = new Object();

    private final AttendanceDAO attendanceDAO = new AttendanceDAOImpl();
    private final MemberService memberService = new MemberService();
    private final MembershipService membershipService = new MembershipService();

    /**
     * Checks a member in. Rules:
     *  1. the member must have an ACTIVE membership        -> MembershipExpiredException
     *  2. the member must not already be inside the gym    -> AttendanceException
     * The open-visit lookup uses SELECT ... FOR UPDATE inside a transaction, so the database
     * row is locked until we commit.
     */
    public Attendance checkIn(int memberId) throws SmartGymException {
        requireAdminOrSelf(memberId);
        Member member = memberService.requireMember(memberId);
        membershipService.requireActiveMembership(memberId);

        Connection con = null;
        synchronized (ATTENDANCE_LOCK) {
            try {
                con = DatabaseConnection.getConnection();
                con.setAutoCommit(false);

                Attendance open = attendanceDAO.findOpenCheckInForUpdate(con, memberId);
                if (open != null) {
                    throw new AttendanceException(member.getFullName() + " is already checked in since "
                            + open.getCheckInTime().format(DateTimeFormatter.ofPattern("HH:mm")) + ".");
                }
                LocalDateTime now = LocalDateTime.now().withNano(0);
                int id = attendanceDAO.checkIn(con, memberId, now);
                con.commit();

                Attendance a = new Attendance();
                a.setAttendanceId(id);
                a.setMemberId(memberId);
                a.setAttendanceDate(now.toLocalDate());
                a.setCheckInTime(now);
                a.setMemberName(member.getFullName());
                a.setMemberCode(member.getMemberCode());
                ActivityLog.log("CHECK-IN " + member.getMemberCode());
                return a;
            } catch (SQLException e) {
                DbUtil.rollbackQuietly(con);
                throw new SmartGymException("Could not connect to the database. " + rootMessage(e), e);
            } catch (DAOException e) {
                DbUtil.rollbackQuietly(con);
                throw dbError("checking in", e);
            } catch (SmartGymException e) {
                DbUtil.rollbackQuietly(con);
                throw e;
            } finally {
                DbUtil.closeQuietly(con);
            }
        }
    }

    /** Checks a member out and returns the finished visit (with duration). */
    public Attendance checkOut(int memberId) throws SmartGymException {
        requireAdminOrSelf(memberId);
        Member member = memberService.requireMember(memberId);

        Connection con = null;
        synchronized (ATTENDANCE_LOCK) {
            try {
                con = DatabaseConnection.getConnection();
                con.setAutoCommit(false);

                Attendance open = attendanceDAO.findOpenCheckInForUpdate(con, memberId);
                if (open == null) {
                    throw new AttendanceException(member.getFullName() + " is not checked in.");
                }
                LocalDateTime now = LocalDateTime.now().withNano(0);
                if (now.isBefore(open.getCheckInTime())) {
                    now = open.getCheckInTime();                       // guard against clock changes
                }
                attendanceDAO.checkOut(con, open.getAttendanceId(), now);
                con.commit();

                open.setCheckOutTime(now);
                open.setMemberName(member.getFullName());
                open.setMemberCode(member.getMemberCode());
                ActivityLog.log("CHECK-OUT " + member.getMemberCode());
                return open;
            } catch (SQLException e) {
                DbUtil.rollbackQuietly(con);
                throw new SmartGymException("Could not connect to the database. " + rootMessage(e), e);
            } catch (DAOException e) {
                DbUtil.rollbackQuietly(con);
                throw dbError("checking out", e);
            } catch (SmartGymException e) {
                DbUtil.rollbackQuietly(con);
                throw e;
            } finally {
                DbUtil.closeQuietly(con);
            }
        }
    }

    // ================================================================ reading

    public List<Attendance> getTodayAttendance() throws SmartGymException {
        return getAttendanceOn(LocalDate.now());
    }

    public List<Attendance> getAttendanceOn(LocalDate date) throws SmartGymException {
        session.requireRole(Role.ADMIN, Role.TRAINER);
        try {
            return attendanceDAO.findByDate(date);
        } catch (DAOException e) {
            throw dbError("loading attendance", e);
        }
    }

    /** Attendance history of one member (members may see only their own). */
    public List<Attendance> getHistory(int memberId) throws SmartGymException {
        memberService.requireAdminTrainerOrSelf(memberId);
        try {
            return attendanceDAO.findByMember(memberId);
        } catch (DAOException e) {
            throw dbError("loading attendance history", e);
        }
    }

    /** Search by member name or code. */
    public List<Attendance> searchByMember(String keyword) throws SmartGymException {
        session.requireRole(Role.ADMIN, Role.TRAINER);
        if (ValidationUtil.isBlank(keyword)) {
            return getTodayAttendance();
        }
        try {
            return attendanceDAO.searchByMember(keyword);
        } catch (DAOException e) {
            throw dbError("searching attendance", e);
        }
    }

    public List<Attendance> getAttendanceBetween(LocalDate from, LocalDate to) throws SmartGymException {
        session.requireRole(Role.ADMIN, Role.TRAINER);
        if (from == null || to == null || to.isBefore(from)) {
            throw new AttendanceException("Choose a valid date range (the end date cannot be before the start date).");
        }
        try {
            return attendanceDAO.findBetween(from, to);
        } catch (DAOException e) {
            throw dbError("loading attendance", e);
        }
    }

    /** Admin (front desk) can check anyone in; a member only themself. */
    private void requireAdminOrSelf(int memberId) throws SmartGymException {
        if (!session.isOwnMemberId(memberId)) {
            session.requireRole(Role.ADMIN);
        }
    }
}
