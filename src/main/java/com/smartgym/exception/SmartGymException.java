package com.smartgym.exception;

/**
 * Base class of all business-rule errors (a CHECKED exception).
 * The GUI only needs to catch this one type to handle every service error.
 *
 * Exception hierarchy used in this project:
 *   Throwable
 *    └─ Exception
 *        ├─ DAOException                 (database problems, DAO layer)
 *        └─ SmartGymException            (business problems, service layer)
 *            ├─ InvalidInputException
 *            │   ├─ InvalidMemberException
 *            │   └─ InvalidPaymentException
 *            ├─ MembershipExpiredException
 *            ├─ TrainerScheduleConflictException
 *            ├─ AttendanceException
 *            ├─ AuthenticationException
 *            └─ UnauthorizedException
 *   RuntimeException (UNCHECKED) -> IllegalStateException, NumberFormatException ...
 */
public class SmartGymException extends Exception {

    private static final long serialVersionUID = 1L;

    public SmartGymException(String message) {
        super(message);
    }

    public SmartGymException(String message, Throwable cause) {
        super(message, cause);
    }
}
