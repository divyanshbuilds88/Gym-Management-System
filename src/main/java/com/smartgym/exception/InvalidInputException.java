package com.smartgym.exception;

/** Thrown when data typed by the user fails validation. */
public class InvalidInputException extends SmartGymException {

    private static final long serialVersionUID = 1L;

    public InvalidInputException(String message) {
        super(message);
    }
}
