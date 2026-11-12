package com.smartgym.exception;

/** Thrown for check-in / check-out problems, e.g. duplicate check-in. */
public class AttendanceException extends SmartGymException {

    private static final long serialVersionUID = 1L;

    public AttendanceException(String message) {
        super(message);
    }
}
