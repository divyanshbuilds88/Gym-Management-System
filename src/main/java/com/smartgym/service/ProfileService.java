package com.smartgym.service;

import com.smartgym.dao.MemberDAO;
import com.smartgym.dao.TrainerDAO;
import com.smartgym.dao.UserDAO;
import com.smartgym.dao.impl.MemberDAOImpl;
import com.smartgym.dao.impl.TrainerDAOImpl;
import com.smartgym.dao.impl.UserDAOImpl;
import com.smartgym.exception.AuthenticationException;
import com.smartgym.exception.DAOException;
import com.smartgym.exception.InvalidInputException;
import com.smartgym.exception.SmartGymException;
import com.smartgym.model.Member;
import com.smartgym.model.Person;
import com.smartgym.model.Role;
import com.smartgym.model.Trainer;
import com.smartgym.model.User;
import com.smartgym.util.ActivityLog;
import com.smartgym.util.PasswordUtil;
import com.smartgym.util.ValidationUtil;

/** Profile of the logged-in person: view, update contact details, change password. */
public class ProfileService extends BaseService {

    private final UserDAO userDAO = new UserDAOImpl();
    private final MemberDAO memberDAO = new MemberDAOImpl();
    private final TrainerDAO trainerDAO = new TrainerDAOImpl();

    /**
     * POLYMORPHISM: the return type is Person, but the object is really a User (admin),
     * a Trainer or a Member depending on who is logged in.
     */
    public Person getProfile() throws SmartGymException {
        session.requireRole(Role.ADMIN, Role.TRAINER, Role.MEMBER);
        User user = session.getCurrentUser();
        try {
            Person profile = null;
            switch (user.getRole()) {
                case MEMBER:
                    profile = memberDAO.findByUserId(user.getUserId());
                    break;
                case TRAINER:
                    profile = trainerDAO.findByUserId(user.getUserId());
                    break;
                default:
                    profile = userDAO.findById(user.getUserId());
                    break;
            }
            if (profile == null) {
                throw new InvalidInputException("No profile is linked to this account.");
            }
            return profile;
        } catch (DAOException e) {
            throw dbError("loading profile", e);
        }
    }

    /** Updates phone and email (and the address for members). */
    public void updateContact(String phone, String email, String address) throws SmartGymException {
        session.requireRole(Role.ADMIN, Role.TRAINER, Role.MEMBER);
        String cleanPhone = phone == null ? null : phone.trim();
        String cleanEmail = email == null ? null : email.trim().toLowerCase();
        if (!ValidationUtil.isValidPhone(cleanPhone)) {
            throw new InvalidInputException("Phone must be a 10-digit mobile number starting with 6-9.");
        }
        if (!ValidationUtil.isValidEmail(cleanEmail)) {
            throw new InvalidInputException("Please enter a valid email address.");
        }
        if (address != null && address.length() > 255) {
            throw new InvalidInputException("Address is too long (maximum 255 characters).");
        }

        User user = session.getCurrentUser();
        try {
            switch (user.getRole()) {
                case MEMBER:
                    Member m = memberDAO.findByUserId(user.getUserId());
                    if (m == null) {
                        throw new InvalidInputException("No member profile is linked to this account.");
                    }
                    memberDAO.updateContact(m.getMemberId(), cleanPhone, cleanEmail, address);
                    break;
                case TRAINER:
                    Trainer t = trainerDAO.findByUserId(user.getUserId());
                    if (t == null) {
                        throw new InvalidInputException("No trainer profile is linked to this account.");
                    }
                    trainerDAO.updateContact(t.getTrainerId(), cleanPhone, cleanEmail);
                    break;
                default:
                    break;                                            // admin has only the users row
            }
            userDAO.updateContact(user.getUserId(), user.getFullName(), cleanEmail, cleanPhone);
            refreshSessionUser();
            ActivityLog.log("PROFILE updated for " + user.getUsername());
        } catch (DAOException e) {
            if (e.isDuplicateKey()) {
                throw new InvalidInputException("Another account already uses this phone number or email.");
            }
            throw dbError("updating profile", e);
        }
    }

    /** Changes the password after checking the old one. */
    public void changePassword(String oldPassword, String newPassword, String confirmPassword) throws SmartGymException {
        session.requireRole(Role.ADMIN, Role.TRAINER, Role.MEMBER);
        try {
            User user = userDAO.findById(session.getCurrentUser().getUserId());
            if (user == null || oldPassword == null
                    || !PasswordUtil.verify(oldPassword, user.getSalt(), user.getPasswordHash())) {
                throw new AuthenticationException("The current password is not correct.");
            }
            if (!ValidationUtil.isStrongPassword(newPassword)) {
                throw new InvalidInputException("New password must be at least 6 characters with a letter and a digit.");
            }
            if (!newPassword.equals(confirmPassword)) {
                throw new InvalidInputException("The new password and the confirmation do not match.");
            }
            if (newPassword.equals(oldPassword)) {
                throw new InvalidInputException("The new password must be different from the old one.");
            }
            String salt = PasswordUtil.generateSalt();                // a fresh salt for every password change
            userDAO.updatePassword(user.getUserId(), PasswordUtil.hash(salt, newPassword), salt);
            ActivityLog.log("PASSWORD changed for " + user.getUsername());
        } catch (DAOException e) {
            throw dbError("changing password", e);
        }
    }

    /** Keeps the session's User object in step with the database after an update. */
    private void refreshSessionUser() throws DAOException {
        User fresh = userDAO.findById(session.getCurrentUser().getUserId());
        if (fresh != null) {
            session.login(fresh, session.getMemberId(), session.getTrainerId());
        }
    }
}
