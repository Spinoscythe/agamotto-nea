package com.srikrishnanethi.agamotto.entities;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Comment;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Login account. Maps to table {@code users}; PK column is {@code user_id}.
 */
@Entity
@Table(name = "users")
@Comment("Registered accounts that can sign in and own or join projects")
@AttributeOverride(
        name = "id",
        column = @Column(name = "user_id", length = 36, nullable = false, updatable = false))
public class User extends BaseEntity {

    @Comment("Display name shown in the UI and on invites")
    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Comment("Unique login email, stored lower-case")
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Comment("BCrypt hash of the password; never store plaintext")
    @Column(name = "passwordHash", nullable = false)
    private String passwordHash;

    @Comment("When this account row was created")
    @Column(name = "createdAt", nullable = false)
    private Instant createdAt = Instant.now();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private UserProfile profile;

    @OneToMany(mappedBy = "owner")
    private List<Project> projects = new ArrayList<>();

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setProfile(UserProfile profile) {
        this.profile = profile;
        if (profile != null) {
            profile.setUser(this);
        }
    }

    public UserProfile getProfile() {
        return profile;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<Project> getProjects() {
        return projects;
    }

    public void setProjects(List<Project> projects) {
        this.projects = projects;
    }
}
