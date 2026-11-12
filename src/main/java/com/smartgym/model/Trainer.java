package com.smartgym.model;

import java.io.Serializable;

/** A gym trainer. Extends Person. */
public class Trainer extends Person {

    private static final long serialVersionUID = 1L;

    private int trainerId;
    private Integer userId;
    private String specialization;
    private String availability;
    private double salary;
    private int experienceYears;

    public Trainer() { }

    public int getTrainerId() { return trainerId; }
    public void setTrainerId(int trainerId) { this.trainerId = trainerId; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }
    public String getAvailability() { return availability; }
    public void setAvailability(String availability) { this.availability = availability; }
    public double getSalary() { return salary; }
    public void setSalary(double salary) { this.salary = salary; }
    public int getExperienceYears() { return experienceYears; }
    public void setExperienceYears(int experienceYears) { this.experienceYears = experienceYears; }

    public Trainer(String fullName, String phone, String email, String specialization,
                   String availability, double salary, int experienceYears) {
        super(fullName, phone, email);
        this.specialization = specialization;
        this.availability = availability;
        this.salary = salary;
        this.experienceYears = experienceYears;
    }

    @Override
    public String getRoleName() { return "TRAINER"; }

    @Override
    public String toString() { return getFullName() + " (" + specialization + ")"; }
}
