package com.cs308.store.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    /** Emails are stored in lower case, so pass the normalized value. */
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
