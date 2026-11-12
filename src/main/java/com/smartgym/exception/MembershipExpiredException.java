package com.smartgym.exception;

/** Thrown when a member without an active membership tries to use the gym or book a session. */
public class MembershipExpiredException extends SmartGymException {

    private static final long serialVersionUID = 1L;

    public MembershipExpiredException(String message) {
        super(message);
    }
}
