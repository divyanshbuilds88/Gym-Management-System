package com.smartgym.exception;

/** Thrown when the username or password is wrong. */
public class AuthenticationException extends SmartGymException {

    private static final long serialVersionUID = 1L;

    public AuthenticationException(String message) {
        super(message);
    }
}
