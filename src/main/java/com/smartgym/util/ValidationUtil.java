package com.smartgym.util;

import java.time.LocalDate;
import java.util.Arrays;

/**
 * Reusable validation rules. Every method returns true/false;
 * the SERVICE classes decide which exception and message to use.
 */
public final class ValidationUtil {

    private ValidationUtil() { }

    public static boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }

    /** 2 to 100 characters; only letters, spaces, dots, apostrophes and hyphens. */
    public static boolean isValidName(String name) {
        if (isBlank(name)) {
            return false;
        }
        String n = name.trim();
        if (n.length() < 2 || n.length() > 100) {
            return false;
        }
        for (int i = 0; i < n.length(); i++) {
            char c = n.charAt(i);
            if (!Character.isLetter(c) && c != ' ' && c != '.' && c != '\'' && c != '-') {
                return false;
            }
        }
        return true;
    }

    /** Indian mobile number: exactly 10 digits, first digit 6-9. */
    public static boolean isValidPhone(String phone) {
        return phone != null && phone.matches("[6-9][0-9]{9}");
    }

    public static boolean isValidEmail(String email) {
        return email != null && email.length() <= 100
                && email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    /** 4-30 characters: letters, digits and underscore only. */
    public static boolean isValidUsername(String username) {
        return username != null && username.matches("[A-Za-z0-9_]{4,30}");
    }

    /** At least 6 characters with at least one letter and one digit. */
    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 6 || password.length() > 50) {
            return false;
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) {
                hasLetter = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            }
        }
        return hasLetter && hasDigit;
    }

    public static boolean isValidAge(int age) {
        return age >= 10 && age <= 100;
    }

    // Method OVERLOADING: same name, different parameter types
    public static boolean isPositive(int value) {
        return value > 0;
    }

    public static boolean isPositive(double value) {
        return value > 0 && !Double.isNaN(value) && !Double.isInfinite(value);
    }

    public static boolean isInRange(int value, int min, int max) {
        return value >= min && value <= max;
    }

    public static boolean isNotInFuture(LocalDate date) {
        return date != null && !date.isAfter(LocalDate.now());
    }

    public static boolean isOneOf(String value, String[] allowed) {
        return value != null && Arrays.asList(allowed).contains(value);
    }

    /** Parses an int; returns -1 when the text is not a whole number. */
    public static int parseIntOrMinusOne(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException | NullPointerException e) {   // unchecked exceptions
            return -1;
        }
    }

    /** Parses a decimal number; returns -1 when the text is not a number. */
    public static double parseDoubleOrMinusOne(String text) {
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException | NullPointerException e) {
            return -1;
        }
    }
}
