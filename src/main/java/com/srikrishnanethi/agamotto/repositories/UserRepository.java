package com.srikrishnanethi.agamotto.repositories;

import com.srikrishnanethi.agamotto.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * CRUD plus login lookup for {@code users}.
 *
 * Emails are stored lower-case at register/login time, so {@link #findByEmail}
 * is an exact match after the caller normalises the address.
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    /**
     * SELECT by unique email. Used by {@code UserServiceImpl} login and by
     * {@code CollaborationServiceImpl} when resolving an invitee.
     */
    Optional<User> findByEmail(String email);

    /**
     * {@code SELECT COUNT(*) > 0} equivalent. Used before insert so a duplicate
     * email becomes a 409 instead of a unique-constraint 500.
     */
    boolean existsByEmail(String email);
}
