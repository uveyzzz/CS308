package com.cs308.store.user;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A row of the users table. The role has no setter on purpose: no request can change it,
 * and it is decided only when the account is created.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column
    private String homeAddress;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /** JPA needs an empty constructor; application code uses the one below. */
    protected User() {
    }

    public User(String fullName, String email, String passwordHash, Role role, String homeAddress) {
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.homeAddress = homeAddress;
        this.createdAt = Instant.now();
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setHomeAddress(String homeAddress) {
        this.homeAddress = homeAddress;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public String getHomeAddress() {
        return homeAddress;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
