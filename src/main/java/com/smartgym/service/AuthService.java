package com.smartgym.service;

import com.smartgym.dao.MemberDAO;
import com.smartgym.dao.TrainerDAO;
import com.smartgym.dao.UserDAO;
import com.smartgym.dao.impl.MemberDAOImpl;
import com.smartgym.dao.impl.TrainerDAOImpl;
import com.smartgym.dao.impl.UserDAOImpl;
import com.smartgym.exception.AuthenticationException;
import com.smartgym.exception.DAOException;
import com.smartgym.exception.SmartGymException;
import com.smartgym.model.Member;
import com.smartgym.model.Trainer;
import com.smartgym.model.User;
import com.smartgym.util.ActivityLog;
import com.smartgym.util.PasswordUtil;
import com.smartgym.util.ValidationUtil;

/** Login and logout. */
public class AuthService extends BaseService {

    private final UserDAO userDAO = new UserDAOImpl();
    private final MemberDAO memberDAO = new MemberDAOImpl();
    private final TrainerDAO trainerDAO = new TrainerDAOImpl();

    /**
     * Checks username + password and starts the session.
     * The same message is used for "no such user" and "wrong password"
     * so an attacker cannot find out which usernames exist.
     */
    public User login(String username, String password) throws SmartGymException {
        if (ValidationUtil.isBlank(username) || password == null || password.isEmpty()) {
            throw new AuthenticationException("Please enter both username and password.");
        }
        try {
            User user = userDAO.findByUsername(username.trim());
            if (user == null || !user.isActive()
                    || !PasswordUtil.verify(password, user.getSalt(), user.getPasswordHash())) {
                ActivityLog.log("FAILED login for username '" + username.trim() + "'");
                throw new AuthenticationException("Invalid username or password.");
            }

            Integer memberId = null;
            Integer trainerId = null;
            switch (user.getRole()) {                       // switch on an enum
                case MEMBER:
                    Member m = memberDAO.findByUserId(user.getUserId());
                    if (m != null) {
                        memberId = m.getMemberId();
                    }
                    break;
                case TRAINER:
                    Trainer t = trainerDAO.findByUserId(user.getUserId());
                    if (t != null) {
                        trainerId = t.getTrainerId();
                    }
                    break;
                default:                                     // ADMIN needs no extra id
                    break;
            }
            session.login(user, memberId, trainerId);
            ActivityLog.log("LOGIN " + user.getRole() + " " + user.getUsername());
            return user;
        } catch (DAOException e) {
            throw dbError("logging in", e);
        }
    }

    public void logout() {
        if (session.isLoggedIn()) {
            ActivityLog.log("LOGOUT " + session.getCurrentUser().getUsername());
        }
        session.logout();
    }
}
