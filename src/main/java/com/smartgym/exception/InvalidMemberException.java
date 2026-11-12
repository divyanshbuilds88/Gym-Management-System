package com.smartgym.exception;

/** Thrown when member data is wrong or a member cannot be found or saved. */
public class InvalidMemberException extends InvalidInputException {

    private static final long serialVersionUID = 1L;

    public InvalidMemberException(String message) {
        super(message);
    }
}
