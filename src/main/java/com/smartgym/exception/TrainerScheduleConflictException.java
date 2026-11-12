package com.smartgym.exception;

/** Thrown when a trainer (or member) is already booked at the requested time. */
public class TrainerScheduleConflictException extends SmartGymException {

    private static final long serialVersionUID = 1L;

    public TrainerScheduleConflictException(String message) {
        super(message);
    }
}
