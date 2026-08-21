package com.srikrishnanethi.agamotto.repositories;

import com.srikrishnanethi.agamotto.entities.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * CRUD for {@code user_profiles}. Lookups are always by the owning user's id
 * because the 1:1 FK {@code user_id} is unique.
 */
@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, String> {

    /**
     * Load the profile row for a user, or empty if they have never saved one.
     * {@code UserServiceImpl} creates a default row at register time; the
     * scheduler reads working hours and scoring weights from here.
     */
    Optional<UserProfile> findByUserId(String id);
}
