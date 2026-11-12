package com.smartgym.model;

import java.io.Serializable;
import java.time.LocalDate;

/** A gym member. Extends Person. The last four fields are filled only when the DAO joins the membership tables. */
public class Member extends Person {

    private static final long serialVersionUID = 1L;

    private int memberId;
    private String memberCode;
    private Integer userId;
    private int age;
    private String gender;
    private String address;
    private LocalDate joiningDate;
    private String planName;
    private LocalDate membershipStart;
    private LocalDate membershipExpiry;
    private String membershipStatus;

    public Member() { }

    public int getMemberId() { return memberId; }
    public void setMemberId(int memberId) { this.memberId = memberId; }
    public String getMemberCode() { return memberCode; }
    public void setMemberCode(String memberCode) { this.memberCode = memberCode; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public LocalDate getJoiningDate() { return joiningDate; }
    public void setJoiningDate(LocalDate joiningDate) { this.joiningDate = joiningDate; }
    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
    public LocalDate getMembershipStart() { return membershipStart; }
    public void setMembershipStart(LocalDate membershipStart) { this.membershipStart = membershipStart; }
    public LocalDate getMembershipExpiry() { return membershipExpiry; }
    public void setMembershipExpiry(LocalDate membershipExpiry) { this.membershipExpiry = membershipExpiry; }
    public String getMembershipStatus() { return membershipStatus; }
    public void setMembershipStatus(String membershipStatus) { this.membershipStatus = membershipStatus; }

    public Member(String fullName, int age, String gender, String phone, String email,
                  String address, LocalDate joiningDate) {
        super(fullName, phone, email);
        this.age = age;
        this.gender = gender;
        this.address = address;
        this.joiningDate = joiningDate;
    }

    @Override
    public String getRoleName() { return "MEMBER"; }

    /** Text shown in combo boxes, e.g. "GYM0001 - Aman Singh". */
    public String getDisplayLabel() { return memberCode + " - " + getFullName(); }

    @Override
    public String toString() { return getDisplayLabel(); }
}
