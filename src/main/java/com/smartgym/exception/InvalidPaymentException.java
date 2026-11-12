package com.smartgym.exception;

/** Thrown when a payment amount, method or status is not acceptable. */
public class InvalidPaymentException extends InvalidInputException {

    private static final long serialVersionUID = 1L;

    public InvalidPaymentException(String message) {
        super(message);
    }
}
