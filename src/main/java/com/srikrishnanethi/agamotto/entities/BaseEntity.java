package com.srikrishnanethi.agamotto.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import org.hibernate.annotations.Comment;

import java.util.Objects;
import java.util.UUID;

/**
 * Shared primary-key mapping for every persisted Agamotto table.
 * IDs are UUID strings stored as {@code CHAR(36)}.
 */
@MappedSuperclass
public abstract class BaseEntity {

    /** Surrogate primary key assigned on first persist. */
    @Id
    @Comment("Primary key: UUID string (36 chars)")
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    /** Generates a random UUID when the row is first inserted. */
    @PrePersist
    protected void assignId() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof BaseEntity that)) {
            return false;
        }
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
