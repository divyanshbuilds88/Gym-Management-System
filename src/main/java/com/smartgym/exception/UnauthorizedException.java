package com.smartgym.exception;

/** Thrown when the logged-in role is not allowed to do an action (role-based access control). */
public class UnauthorizedException extends SmartGymException {

    private static final long serialVersionUID = 1L;

    public UnauthorizedException(String message) {
        super(message);
    }
}
