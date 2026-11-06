package com.cs308.store.config;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.cs308.store.user.Role;
import com.cs308.store.user.User;
import com.cs308.store.user.UserRepository;

/**
 * Creates one test account per role, because sign-up only ever creates customers.
 *
 * This is not a Flyway migration on purpose: the repo is public and migrations also run on the
 * shared database, so a password written there would open manager accounts to anyone.
 */
@Component
@Profile("dev")
public class DevDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String password;

    public DevDataSeeder(UserRepository users, PasswordEncoder passwordEncoder,
            @Value("${DEV_SEED_PASSWORD}") String password) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (Role role : Role.values()) {
            String email = emailFor(role);
            if (!users.existsByEmail(email)) {
                users.save(new User("Test " + role.name(), email, passwordEncoder.encode(password), role, null));
                log.info("Created test account {}", email);
            }
        }
    }

    /** For example SALES_MANAGER becomes sales.manager@store.test. */
    static String emailFor(Role role) {
        return role.name().toLowerCase(Locale.ROOT).replace('_', '.') + "@store.test";
    }
}
