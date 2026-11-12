package com.smartgym.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Password hashing: hash = SHA-256( salt + password ), written as 64 hex characters.
 * A different random salt per user means two users with the same password
 * still get different hashes.
 */
public final class PasswordUtil {

    private PasswordUtil() { }

    /** 16 random bytes written as 32 hex characters (fits the VARCHAR(32) salt column). */
    public static String generateSalt() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return toHex(bytes);
    }

    public static String hash(String salt, String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] result = digest.digest((salt + password).getBytes(StandardCharsets.UTF_8));
            return toHex(result);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed to exist in every Java installation
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public static boolean verify(String password, String salt, String storedHash) {
        return hash(salt, password).equalsIgnoreCase(storedHash);
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();      // StringBuilder demo
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
