package com.srikrishnanethi.agamotto.repositories;

import com.srikrishnanethi.agamotto.entities.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, String> {
    Optional<UserProfile> findByUserId(String id);
}
