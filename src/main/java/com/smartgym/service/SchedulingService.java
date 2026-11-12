package com.smartgym.service;

import com.smartgym.dao.TrainerDAO;
import com.smartgym.dao.TrainerScheduleDAO;
import com.smartgym.dao.impl.TrainerDAOImpl;
import com.smartgym.dao.impl.TrainerScheduleDAOImpl;
import com.smartgym.exception.DAOException;
import com.smartgym.exception.InvalidInputException;
import com.smartgym.exception.SmartGymException;
import com.smartgym.exception.TrainerScheduleConflictException;
import com.smartgym.exception.UnauthorizedException;
import com.smartgym.model.Role;
import com.smartgym.model.TrainerSchedule;
import com.smartgym.util.ActivityLog;
import com.smartgym.util.Constants;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** Booking personal-training sessions, with conflict prevention. */
public class SchedulingService extends BaseService {

    private static final LocalTime GYM_OPEN = LocalTime.of(5, 0);
    private static final LocalTime GYM_CLOSE = LocalTime.of(22, 0);

    /**
     * SYNCHRONIZATION: two people clicking "Book" at the same moment must not both pass the
     * conflict check and then both insert. One shared lock makes "check + insert" a single step.
     */
    private static final Object BOOKING_LOCK = new Object();

    private final TrainerScheduleDAO scheduleDAO = new TrainerScheduleDAOImpl();
    private final TrainerDAO trainerDAO = new TrainerDAOImpl();
    private final MemberService memberService = new MemberService();
    private final MembershipService membershipService = new MembershipService();

    // ================================================================ booking

    public int scheduleSession(TrainerSchedule s) throws SmartGymException {
        session.requireRole(Role.ADMIN, Role.TRAINER);
        requireOwnTrainerIfTrainer(s.getTrainerId());
        validate(s, true);
        memberService.requireMember(s.getMemberId());
        membershipService.requireActiveMembership(s.getMemberId());      // MembershipExpiredException

        try {
            if (trainerDAO.findById(s.getTrainerId()) == null) {
                throw new InvalidInputException("Trainer not found.");
            }
            LocalTime end = s.getComputedEndTime();
            synchronized (BOOKING_LOCK) {                                // only one thread at a time in here
                checkConflicts(s, end, 0);
                s.setStatus(Constants.SCHEDULED);
                int id = scheduleDAO.insert(s);
                s.setScheduleId(id);
                ActivityLog.log("SESSION booked id " + id + " trainer " + s.getTrainerId() + " on "
                        + s.getSessionDate() + " " + s.getStartTime());
                return id;
            }
        } catch (DAOException e) {
            throw dbError("booking session", e);
        }
    }

    public void updateSession(TrainerSchedule s) throws SmartGymException {
        session.requireRole(Role.ADMIN, Role.TRAINER);
        requireOwnTrainerIfTrainer(s.getTrainerId());
        validate(s, true);
        try {
            TrainerSchedule old = scheduleDAO.findById(s.getScheduleId());
            if (old == null) {
                throw new InvalidInputException("Session not found.");
            }
            if (!Constants.SCHEDULED.equals(old.getStatus())) {
                throw new InvalidInputException("Only SCHEDULED sessions can be changed.");
            }
            membershipService.requireActiveMembership(s.getMemberId());
            LocalTime end = s.getComputedEndTime();
            synchronized (BOOKING_LOCK) {
                checkConflicts(s, end, s.getScheduleId());               // ignore the session being edited
                s.setStatus(Constants.SCHEDULED);
                scheduleDAO.update(s);
            }
        } catch (DAOException e) {
            throw dbError("updating session", e);
        }
    }

    public void cancelSession(int scheduleId) throws SmartGymException {
        changeStatus(scheduleId, Constants.CANCELLED);
    }

    public void completeSession(int scheduleId) throws SmartGymException {
        changeStatus(scheduleId, Constants.COMPLETED);
    }

    private void changeStatus(int scheduleId, String newStatus) throws SmartGymException {
        session.requireRole(Role.ADMIN, Role.TRAINER);
        try {
            TrainerSchedule s = scheduleDAO.findById(scheduleId);
            if (s == null) {
                throw new InvalidInputException("Session not found.");
            }
            requireOwnTrainerIfTrainer(s.getTrainerId());
            if (!Constants.SCHEDULED.equals(s.getStatus())) {
                throw new InvalidInputException("This session is already " + s.getStatus() + ".");
            }
            scheduleDAO.updateStatus(scheduleId, newStatus);
        } catch (DAOException e) {
            throw dbError("updating session", e);
        }
    }

    public void deleteSession(int scheduleId) throws SmartGymException {
        session.requireRole(Role.ADMIN);
        try {
            if (!scheduleDAO.delete(scheduleId)) {
                throw new InvalidInputException("Session not found.");
            }
        } catch (DAOException e) {
            throw dbError("deleting session", e);
        }
    }

    // ================================================================ reading

    public List<TrainerSchedule> getAllSessions() throws SmartGymException {
        session.requireRole(Role.ADMIN, Role.TRAINER);
        try {
            return scheduleDAO.findAll();
        } catch (DAOException e) {
            throw dbError("loading sessions", e);
        }
    }

    /** The timetable of one trainer. */
    public List<TrainerSchedule> getTimetable(int trainerId) throws SmartGymException {
        session.requireRole(Role.ADMIN, Role.TRAINER);
        try {
            return scheduleDAO.findByTrainer(trainerId);
        } catch (DAOException e) {
            throw dbError("loading timetable", e);
        }
    }

    public List<TrainerSchedule> getSessionsOfMember(int memberId) throws SmartGymException {
        memberService.requireAdminTrainerOrSelf(memberId);
        try {
            return scheduleDAO.findByMember(memberId);
        } catch (DAOException e) {
            throw dbError("loading sessions", e);
        }
    }

    public List<TrainerSchedule> getSessionsOnDate(LocalDate date) throws SmartGymException {
        session.requireRole(Role.ADMIN, Role.TRAINER);
        try {
            return scheduleDAO.findByDate(date);
        } catch (DAOException e) {
            throw dbError("loading sessions", e);
        }
    }

    public List<TrainerSchedule> getUpcoming(int limit) throws SmartGymException {
        session.requireRole(Role.ADMIN, Role.TRAINER);
        try {
            return scheduleDAO.findUpcoming(limit);
        } catch (DAOException e) {
            throw dbError("loading upcoming sessions", e);
        }
    }

    // ================================================================ rules

    private void checkConflicts(TrainerSchedule s, LocalTime end, int excludeId)
            throws DAOException, TrainerScheduleConflictException {
        if (scheduleDAO.hasTrainerConflict(s.getTrainerId(), s.getSessionDate(), s.getStartTime(), end, excludeId)) {
            throw new TrainerScheduleConflictException("The trainer is already booked between "
                    + s.getStartTime() + " and " + end + " on " + s.getSessionDate() + ".");
        }
        if (scheduleDAO.hasMemberConflict(s.getMemberId(), s.getSessionDate(), s.getStartTime(), end, excludeId)) {
            throw new TrainerScheduleConflictException("The member already has another session between "
                    + s.getStartTime() + " and " + end + " on " + s.getSessionDate() + ".");
        }
    }

    private void validate(TrainerSchedule s, boolean mustBeFuture) throws InvalidInputException {
        if (s == null || s.getSessionDate() == null || s.getStartTime() == null) {
            throw new InvalidInputException("Date and start time are required.");
        }
        if (s.getDurationMinutes() < 15 || s.getDurationMinutes() > 180) {
            throw new InvalidInputException("Duration must be between 15 and 180 minutes.");
        }
        LocalTime end = s.getComputedEndTime();
        if (!end.isAfter(s.getStartTime())) {
            throw new InvalidInputException("A session cannot run past midnight.");
        }
        if (s.getStartTime().isBefore(GYM_OPEN) || end.isAfter(GYM_CLOSE)) {
            throw new InvalidInputException("Sessions must be within gym hours (" + GYM_OPEN + " - " + GYM_CLOSE + ").");
        }
        if (mustBeFuture && LocalDateTime.of(s.getSessionDate(), s.getStartTime()).isBefore(LocalDateTime.now())) {
            throw new InvalidInputException("The session must be in the future.");
        }
        if (s.getNotes() != null && s.getNotes().length() > 255) {
            throw new InvalidInputException("Notes are too long (maximum 255 characters).");
        }
    }

    /** A trainer may only manage their OWN sessions; the admin may manage everyone's. */
    private void requireOwnTrainerIfTrainer(int trainerId) throws UnauthorizedException {
        if (session.getRole() == Role.TRAINER
                && (session.getTrainerId() == null || session.getTrainerId() != trainerId)) {
            throw new UnauthorizedException("Trainers can only manage their own sessions.");
        }
    }
}
