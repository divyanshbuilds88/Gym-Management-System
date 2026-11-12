package com.smartgym.service;

import com.smartgym.dao.TrainerDAO;
import com.smartgym.dao.UserDAO;
import com.smartgym.dao.impl.TrainerDAOImpl;
import com.smartgym.dao.impl.UserDAOImpl;
import com.smartgym.exception.DAOException;
import com.smartgym.exception.InvalidInputException;
import com.smartgym.exception.SmartGymException;
import com.smartgym.model.Role;
import com.smartgym.model.Trainer;
import com.smartgym.model.User;
import com.smartgym.util.ActivityLog;
import com.smartgym.util.DatabaseConnection;
import com.smartgym.util.DbUtil;
import com.smartgym.util.PasswordUtil;
import com.smartgym.util.ValidationUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/** Trainer management. Everybody who is logged in may read the trainer list; only the admin may change it. */
public class TrainerService extends BaseService {

    private final TrainerDAO trainerDAO = new TrainerDAOImpl();
    private final UserDAO userDAO = new UserDAOImpl();

    public List<Trainer> getAllTrainers() throws SmartGymException {
        session.requireRole(Role.ADMIN, Role.TRAINER, Role.MEMBER);
        try {
            return trainerDAO.findAll();
        } catch (DAOException e) {
            throw dbError("loading trainers", e);
        }
    }

    public List<Trainer> searchTrainers(String keyword) throws SmartGymException {
        session.requireRole(Role.ADMIN, Role.TRAINER, Role.MEMBER);
        try {
            return ValidationUtil.isBlank(keyword) ? trainerDAO.findAll() : trainerDAO.search(keyword);
        } catch (DAOException e) {
            throw dbError("searching trainers", e);
        }
    }

    public Trainer getTrainer(int trainerId) throws SmartGymException {
        session.requireRole(Role.ADMIN, Role.TRAINER, Role.MEMBER);
        try {
            Trainer t = trainerDAO.findById(trainerId);
            if (t == null) {
                throw new InvalidInputException("Trainer not found.");
            }
            return t;
        } catch (DAOException e) {
            throw dbError("loading trainer", e);
        }
    }

    /** Adds a trainer without a login account. */
    public int addTrainer(Trainer trainer) throws SmartGymException {
        return addTrainer(trainer, null, null);
    }

    /** Adds a trainer and, when a username is given, a TRAINER login - in one transaction. */
    public int addTrainer(Trainer trainer, String username, String password) throws SmartGymException {
        session.requireRole(Role.ADMIN);
        validate(trainer);
        boolean createLogin = !ValidationUtil.isBlank(username);
        if (createLogin) {
            if (!ValidationUtil.isValidUsername(username)) {
                throw new InvalidInputException("Username must be 4-30 letters, digits or underscore.");
            }
            if (!ValidationUtil.isStrongPassword(password)) {
                throw new InvalidInputException("Password must be at least 6 characters with a letter and a digit.");
            }
        }
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);
            if (createLogin) {
                String salt = PasswordUtil.generateSalt();
                User user = new User(username.trim(), PasswordUtil.hash(salt, password), salt, Role.TRAINER,
                        trainer.getFullName(), trainer.getEmail(), trainer.getPhone());
                trainer.setUserId(userDAO.insert(con, user));
            }
            int id = trainerDAO.insert(con, trainer);
            con.commit();
            trainer.setTrainerId(id);
            ActivityLog.log("ADDED trainer " + trainer.getFullName());
            return id;
        } catch (SQLException e) {
            DbUtil.rollbackQuietly(con);
            throw new SmartGymException("Could not connect to the database. " + rootMessage(e), e);
        } catch (DAOException e) {
            DbUtil.rollbackQuietly(con);
            if (e.isDuplicateKey()) {
                throw new InvalidInputException("A trainer or login with this phone, email or username already exists.");
            }
            throw dbError("adding trainer", e);
        } finally {
            DbUtil.closeQuietly(con);
        }
    }

    public void updateTrainer(Trainer trainer) throws SmartGymException {
        session.requireRole(Role.ADMIN);
        validate(trainer);
        try {
            if (!trainerDAO.update(trainer)) {
                throw new InvalidInputException("Trainer not found - it may have been deleted.");
            }
        } catch (DAOException e) {
            if (e.isDuplicateKey()) {
                throw new InvalidInputException("Another trainer already uses this phone number or email.");
            }
            throw dbError("updating trainer", e);
        }
    }

    public void deleteTrainer(int trainerId) throws SmartGymException {
        session.requireRole(Role.ADMIN);
        try {
            if (!trainerDAO.delete(trainerId)) {
                throw new InvalidInputException("Trainer not found - it may already be deleted.");
            }
            ActivityLog.log("DELETED trainer id " + trainerId);
        } catch (DAOException e) {
            if (e.isForeignKeyViolation()) {
                throw new InvalidInputException("This trainer still has members assigned to workout plans. Reassign or remove them first.");
            }
            throw dbError("deleting trainer", e);
        }
    }

    private void validate(Trainer t) throws InvalidInputException {
        if (t == null) {
            throw new InvalidInputException("No trainer data supplied.");
        }
        if (t.getFullName() != null) {
            t.setFullName(t.getFullName().trim());
        }
        if (t.getEmail() != null) {
            t.setEmail(t.getEmail().trim().toLowerCase());
        }
        if (t.getPhone() != null) {
            t.setPhone(t.getPhone().trim());
        }
        if (!ValidationUtil.isValidName(t.getFullName())) {
            throw new InvalidInputException("Trainer name must be 2-100 characters (letters and spaces only).");
        }
        if (ValidationUtil.isBlank(t.getSpecialization()) || t.getSpecialization().trim().length() > 100) {
            throw new InvalidInputException("Specialization is required (maximum 100 characters).");
        }
        t.setSpecialization(t.getSpecialization().trim());
        if (!ValidationUtil.isValidPhone(t.getPhone())) {
            throw new InvalidInputException("Phone must be a 10-digit mobile number starting with 6-9.");
        }
        if (!ValidationUtil.isValidEmail(t.getEmail())) {
            throw new InvalidInputException("Please enter a valid email address.");
        }
        if (ValidationUtil.isBlank(t.getAvailability()) || t.getAvailability().trim().length() > 100) {
            throw new InvalidInputException("Availability is required, e.g. 'Mon-Sat 06:00-14:00'.");
        }
        t.setAvailability(t.getAvailability().trim());
        if (t.getSalary() < 0 || t.getSalary() > 1_000_000 || Double.isNaN(t.getSalary())) {
            throw new InvalidInputException("Salary must be between 0 and 1,000,000.");
        }
        t.setSalary(Math.round(t.getSalary() * 100) / 100.0);
        if (!ValidationUtil.isInRange(t.getExperienceYears(), 0, 50)) {
            throw new InvalidInputException("Experience must be between 0 and 50 years.");
        }
    }
}
