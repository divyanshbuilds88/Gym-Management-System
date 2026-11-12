package com.smartgym.exception;

import java.sql.SQLException;

/**
 * Checked exception thrown by the DAO layer. It wraps a low-level SQLException
 * so that the service and GUI layers do not depend on JDBC classes.
 */
public class DAOException extends Exception {

    private static final long serialVersionUID = 1L;

    private boolean duplicateKey = false;
    private boolean foreignKeyViolation = false;

    public DAOException(String message) {
        super(message);
    }

    public DAOException(String message, Throwable cause) {
        super(message, cause);
        if (cause instanceof SQLException) {
            int code = ((SQLException) cause).getErrorCode();
            duplicateKey = (code == 1062);                          // duplicate entry
            foreignKeyViolation = (code == 1451 || code == 1452);   // row is referenced / parent missing
        }
    }

    public boolean isDuplicateKey()        { return duplicateKey; }
    public boolean isForeignKeyViolation() { return foreignKeyViolation; }
}
