package com.smartgym.service;

import com.smartgym.exception.UnauthorizedException;
import com.smartgym.model.Role;
import com.smartgym.model.User;

/**
 * Remembers WHO is logged in. There is only one session in the whole program,
 * so this class uses the Singleton pattern (private constructor + getInstance()).
 * Services call requireRole(...) before doing protected work = role-based access control.
 */
public class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();

    private User currentUser;
    private Integer memberId;     // set when a MEMBER is logged in
    private Integer trainerId;    // set when a TRAINER is logged in

    private SessionManager() { }

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public synchronized void login(User user, Integer memberId, Integer trainerId) {
        this.currentUser = user;
        this.memberId = memberId;
        this.trainerId = trainerId;
    }

    public synchronized void logout() {
        currentUser = null;
        memberId = null;
        trainerId = null;
    }

    public synchronized boolean isLoggedIn()        { return currentUser != null; }
    public synchronized User getCurrentUser()       { return currentUser; }
    public synchronized Integer getMemberId()       { return memberId; }
    public synchronized Integer getTrainerId()      { return trainerId; }

    public synchronized Role getRole() {
        return currentUser == null ? null : currentUser.getRole();
    }

    /** Varargs: requireRole(Role.ADMIN) or requireRole(Role.ADMIN, Role.TRAINER). */
    public synchronized void requireRole(Role... allowed) throws UnauthorizedException {
        if (currentUser == null) {
            throw new UnauthorizedException("Please log in first.");
        }
        for (Role r : allowed) {
            if (currentUser.getRole() == r) {
                return;
            }
        }
        throw new UnauthorizedException("Your role (" + currentUser.getRole() + ") is not allowed to do this.");
    }

    /** True when the logged-in user is a MEMBER and the given id is that member's own id. */
    public synchronized boolean isOwnMemberId(int id) {
        return currentUser != null && currentUser.getRole() == Role.MEMBER
                && memberId != null && memberId == id;
    }
}
