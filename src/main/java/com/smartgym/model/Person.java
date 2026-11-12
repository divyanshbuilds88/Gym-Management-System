package com.smartgym.model;

import java.io.Serializable;

/**
 * ABSTRACT base class for every human in the system.
 * User, Member and Trainer all INHERIT the common fields from here.
 */
public abstract class Person implements Serializable {

    private static final long serialVersionUID = 1L;

    private String fullName;
    private String phone;
    private String email;

    public Person() { }

    public Person(String fullName, String phone, String email) {
        this.fullName = fullName;     // 'this' reference
        this.phone = phone;
        this.email = email;
    }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    /** Every subclass must say what kind of person it is (abstract method). */
    public abstract String getRoleName();

    @Override
    public String toString() {
        return fullName;
    }
}
