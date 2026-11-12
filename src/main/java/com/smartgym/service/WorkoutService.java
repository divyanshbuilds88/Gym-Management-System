package com.smartgym.service;

import com.smartgym.dao.MemberWorkoutDAO;
import com.smartgym.dao.TrainerDAO;
import com.smartgym.dao.WorkoutPlanDAO;
import com.smartgym.dao.impl.MemberWorkoutDAOImpl;
import com.smartgym.dao.impl.TrainerDAOImpl;
import com.smartgym.dao.impl.WorkoutPlanDAOImpl;
import com.smartgym.exception.DAOException;
import com.smartgym.exception.InvalidInputException;
import com.smartgym.exception.SmartGymException;
import com.smartgym.model.MemberWorkout;
import com.smartgym.model.Role;
import com.smartgym.model.WorkoutExercise;
import com.smartgym.model.WorkoutPlan;
import com.smartgym.util.Constants;
import com.smartgym.util.ValidationUtil;

import java.time.LocalDate;
import java.util.List;

/** Workout plans, their exercises, and assigning plans to members. */
public class WorkoutService extends BaseService {

    private final WorkoutPlanDAO planDAO = new WorkoutPlanDAOImpl();
    private final MemberWorkoutDAO assignmentDAO = new MemberWorkoutDAOImpl();
    private final TrainerDAO trainerDAO = new TrainerDAOImpl();
    private final MemberService memberService = new MemberService();

    // ================================================================ plans

    public List<WorkoutPlan> getAllPlans() throws SmartGymException {
        session.requireRole(Role.ADMIN, Role.TRAINER, Role.MEMBER);
        try {
            return planDAO.findAll();
        } catch (DAOException e) {
            throw dbError("loading workout plans", e);
        }
    }

    public List<WorkoutExercise> getExercises(int workoutPlanId) throws SmartGymException {
        session.requireRole(Role.ADMIN, Role.TRAINER, Role.MEMBER);
        try {
            return planDAO.findExercises(workoutPlanId);
        } catch (DAOException e) {
            throw dbError("loading exercises", e);
        }
    }

    /** Creates a plan and all its exercises together (one transaction in the DAO). */
    public int createPlan(WorkoutPlan plan, List<WorkoutExercise> exercises) throws SmartGymException {
        session.requireRole(Role.ADMIN, Role.TRAINER);
        validatePlan(plan);
        if (exercises == null || exercises.isEmpty()) {
            throw new InvalidInputException("Add at least one exercise to the plan.");
        }
        for (WorkoutExercise e : exercises) {
            validateExercise(e);
        }
        try {
            return planDAO.insertWithExercises(plan, exercises);
        } catch (DAOException e) {
            if (e.isDuplicateKey()) {
                throw new InvalidInputException("A workout plan named '" + plan.getPlanName() + "' already exists.");
            }
            throw dbError("creating workout plan", e);
        }
    }

    public void updatePlan(WorkoutPlan plan) throws SmartGymException {
        session.requireRole(Role.ADMIN, Role.TRAINER);
        validatePlan(plan);
        try {
            if (!planDAO.update(plan)) {
                throw new InvalidInputException("Workout plan not found.");
            }
        } catch (DAOException e) {
            if (e.isDuplicateKey()) {
                throw new InvalidInputException("A workout plan named '" + plan.getPlanName() + "' already exists.");
            }
            throw dbError("updating workout plan", e);
        }
    }

    public void deletePlan(int workoutPlanId) throws SmartGymException {
        session.requireRole(Role.ADMIN, Role.TRAINER);
        try {
            if (!planDAO.delete(workoutPlanId)) {
                throw new InvalidInputException("Workout plan not found.");
            }
        } catch (DAOException e) {
            throw dbError("deleting workout plan", e);
        }
    }

    // ================================================================ exercises

    public int addExercise(WorkoutExercise exercise) throws SmartGymException {
        session.requireRole(Role.ADMIN, Role.TRAINER);
        validateExercise(exercise);
        try {
            return planDAO.addExercise(exercise);
        } catch (DAOException e) {
            throw dbError("adding exercise", e);
        }
    }

    public void updateExercise(WorkoutExercise exercise) throws SmartGymException {
        session.requireRole(Role.ADMIN, Role.TRAINER);
        validateExercise(exercise);
        try {
            if (!planDAO.updateExercise(exercise)) {
                throw new InvalidInputException("Exercise not found.");
            }
        } catch (DAOException e) {
            throw dbError("updating exercise", e);
        }
    }

    public void deleteExercise(int exerciseId) throws SmartGymException {
        session.requireRole(Role.ADMIN, Role.TRAINER);
        try {
            if (!planDAO.deleteExercise(exerciseId)) {
                throw new InvalidInputException("Exercise not found.");
            }
        } catch (DAOException e) {
            throw dbError("deleting exercise", e);
        }
    }

    // ================================================================ assignments

    /** Assigns a plan to a member. A member cannot get the same plan twice while it is still ACTIVE. */
    public int assignToMember(int memberId, int workoutPlanId, int trainerId, LocalDate endDate)
            throws SmartGymException {
        session.requireRole(Role.ADMIN, Role.TRAINER);
        memberService.requireMember(memberId);                       // throws if the member does not exist
        try {
            if (planDAO.findById(workoutPlanId) == null) {
                throw new InvalidInputException("Workout plan not found.");
            }
            if (trainerDAO.findById(trainerId) == null) {
                throw new InvalidInputException("Trainer not found.");
            }
            if (endDate != null && !endDate.isAfter(LocalDate.now())) {
                throw new InvalidInputException("End date must be after today.");
            }
            for (MemberWorkout existing : assignmentDAO.findByMember(memberId)) {
                if (existing.getWorkoutPlanId() == workoutPlanId && Constants.ACTIVE.equals(existing.getStatus())) {
                    throw new InvalidInputException("This member already has this workout plan active.");
                }
            }
            MemberWorkout mw = new MemberWorkout();
            mw.setMemberId(memberId);
            mw.setWorkoutPlanId(workoutPlanId);
            mw.setTrainerId(trainerId);
            mw.setAssignedDate(LocalDate.now());
            mw.setEndDate(endDate);
            mw.setStatus(Constants.ACTIVE);
            return assignmentDAO.assign(mw);
        } catch (DAOException e) {
            throw dbError("assigning workout plan", e);
        }
    }

    public List<MemberWorkout> getAllAssignments() throws SmartGymException {
        session.requireRole(Role.ADMIN, Role.TRAINER);
        try {
            return assignmentDAO.findAll();
        } catch (DAOException e) {
            throw dbError("loading assignments", e);
        }
    }

    public List<MemberWorkout> getAssignmentsOfMember(int memberId) throws SmartGymException {
        memberService.requireAdminTrainerOrSelf(memberId);
        try {
            return assignmentDAO.findByMember(memberId);
        } catch (DAOException e) {
            throw dbError("loading assignments", e);
        }
    }

    public List<MemberWorkout> getAssignmentsOfTrainer(int trainerId) throws SmartGymException {
        session.requireRole(Role.ADMIN, Role.TRAINER);
        try {
            return assignmentDAO.findByTrainer(trainerId);
        } catch (DAOException e) {
            throw dbError("loading assignments", e);
        }
    }

    public void updateAssignmentStatus(int assignmentId, String status) throws SmartGymException {
        session.requireRole(Role.ADMIN, Role.TRAINER);
        if (!ValidationUtil.isOneOf(status, new String[] {"ACTIVE", "COMPLETED", "CANCELLED"})) {
            throw new InvalidInputException("Invalid assignment status.");
        }
        try {
            if (!assignmentDAO.updateStatus(assignmentId, status)) {
                throw new InvalidInputException("Assignment not found.");
            }
        } catch (DAOException e) {
            throw dbError("updating assignment", e);
        }
    }

    public void removeAssignment(int assignmentId) throws SmartGymException {
        session.requireRole(Role.ADMIN, Role.TRAINER);
        try {
            if (!assignmentDAO.delete(assignmentId)) {
                throw new InvalidInputException("Assignment not found.");
            }
        } catch (DAOException e) {
            throw dbError("removing assignment", e);
        }
    }

    // ================================================================ validation

    private void validatePlan(WorkoutPlan p) throws InvalidInputException {
        if (p == null || ValidationUtil.isBlank(p.getPlanName()) || p.getPlanName().trim().length() > 100) {
            throw new InvalidInputException("Plan name is required (maximum 100 characters).");
        }
        p.setPlanName(p.getPlanName().trim());
        if (ValidationUtil.isBlank(p.getGoal()) || p.getGoal().trim().length() > 100) {
            throw new InvalidInputException("Goal is required (maximum 100 characters).");
        }
        p.setGoal(p.getGoal().trim());
        if (!ValidationUtil.isOneOf(p.getLevel(), Constants.WORKOUT_LEVELS)) {
            throw new InvalidInputException("Please choose a level.");
        }
        if (p.getDescription() != null && p.getDescription().length() > 255) {
            throw new InvalidInputException("Description is too long (maximum 255 characters).");
        }
    }

    private void validateExercise(WorkoutExercise e) throws InvalidInputException {
        if (e == null || ValidationUtil.isBlank(e.getExerciseName()) || e.getExerciseName().trim().length() > 100) {
            throw new InvalidInputException("Exercise name is required (maximum 100 characters).");
        }
        e.setExerciseName(e.getExerciseName().trim());
        if (!ValidationUtil.isInRange(e.getSets(), 1, 20)) {
            throw new InvalidInputException("Sets must be between 1 and 20.");
        }
        if (!ValidationUtil.isInRange(e.getRepetitions(), 1, 200)) {
            throw new InvalidInputException("Repetitions must be between 1 and 200.");
        }
        if (!ValidationUtil.isInRange(e.getDurationMinutes(), 1, 240)) {
            throw new InvalidInputException("Duration must be between 1 and 240 minutes.");
        }
        if (ValidationUtil.isBlank(e.getTargetMuscleGroup()) || e.getTargetMuscleGroup().trim().length() > 50) {
            throw new InvalidInputException("Target muscle group is required (maximum 50 characters).");
        }
        e.setTargetMuscleGroup(e.getTargetMuscleGroup().trim());
        if (!ValidationUtil.isOneOf(e.getScheduleDay(), Constants.DAYS)) {
            throw new InvalidInputException("Please choose a day of the week.");
        }
    }
}
