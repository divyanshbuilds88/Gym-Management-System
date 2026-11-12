package com.smartgym.model;

/** The three kinds of login accounts (an enum is a special class with fixed constants). */
public enum Role {
    ADMIN, TRAINER, MEMBER;

    public static Role fromString(String text) {
        return Role.valueOf(text.trim().toUpperCase());
    }
}
