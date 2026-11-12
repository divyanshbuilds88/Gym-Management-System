package com.smartgym.model;

import java.io.Serializable;

/** A login account. Extends Person (name, phone, email). */
public class User extends Person {

    private static final long serialVersionUID = 1L;

    private int userId;
    private String username;
    private String passwordHash;
    private String salt;
    private Role role;
    private boolean active;

    public User() { }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public User(String username, String passwordHash, String salt, Role role,
                String fullName, String email, String phone) {
        super(fullName, phone, email);          // 'super' calls the Person constructor
        this.username = username;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.role = role;
        this.active = true;
    }

    @Override
    public String getRoleName() { return role == null ? "UNKNOWN" : role.name(); }
}
