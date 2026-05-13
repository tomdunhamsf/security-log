package com.securitylog.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    // 'user' and 'password' are SQL reserved words; using 'username' / 'password_hash' avoids quoting issues.
    @Id
    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    public User() {}

    public User(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
}
