package com.smartgym.util;

/** Fixed lists used by combo boxes and validation. */
public final class Constants {

    private Constants() { }

    public static final String[] GENDERS           = {"MALE", "FEMALE", "OTHER"};
    public static final String[] PAYMENT_METHODS   = {"CASH", "CARD", "UPI", "NET_BANKING"};
    public static final String[] PAYMENT_STATUSES  = {"PAID", "PENDING", "FAILED"};
    public static final String[] SESSION_STATUSES  = {"SCHEDULED", "COMPLETED", "CANCELLED"};
    public static final String[] WORKOUT_LEVELS    = {"BEGINNER", "INTERMEDIATE", "ADVANCED"};
    public static final String[] DAYS              = {"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};

    public static final String ACTIVE    = "ACTIVE";
    public static final String EXPIRED   = "EXPIRED";
    public static final String CANCELLED = "CANCELLED";
    public static final String COMPLETED = "COMPLETED";
    public static final String SCHEDULED = "SCHEDULED";
    public static final String PAID      = "PAID";
    public static final String PENDING   = "PENDING";
}
