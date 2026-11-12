package com.smartgym.service;

import com.smartgym.exception.DAOException;
import com.smartgym.exception.SmartGymException;

/**
 * ABSTRACT parent of all service classes: shares the session and the error-conversion code.
 * Every service turns a low-level DAOException into a SmartGymException,
 * so the GUI layer never has to know about SQL.
 */
public abstract class BaseService {

    protected final SessionManager session = SessionManager.getInstance();

    protected SmartGymException dbError(String action, DAOException e) {
        return new SmartGymException("Database error while " + action + ". " + rootMessage(e), e);
    }

    /** Message of the deepest cause, e.g. the original MySQL message. */
    protected static String rootMessage(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() == null ? "" : cause.getMessage();
    }
}
