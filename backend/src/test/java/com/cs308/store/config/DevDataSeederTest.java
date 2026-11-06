package com.cs308.store.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.cs308.store.user.Role;
import com.cs308.store.user.User;
import com.cs308.store.user.UserRepository;

class DevDataSeederTest {

    private static final String PASSWORD = "a-test-password";

    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final DevDataSeeder seeder = new DevDataSeeder(users, passwordEncoder, PASSWORD);

    @Test
    void createsOneHashedAccountPerRole() {
        given(users.existsByEmail(anyString())).willReturn(false);

        seeder.run(null);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users, times(Role.values().length)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(User::getRole).containsExactlyInAnyOrder(Role.values());
        assertThat(saved.getAllValues()).allMatch(user -> passwordEncoder.matches(PASSWORD, user.getPasswordHash()));
    }

    @Test
    void runningAgainCreatesNothing() {
        given(users.existsByEmail(anyString())).willReturn(true);

        seeder.run(null);

        verify(users, never()).save(any());
    }

    @Test
    void emailComesFromTheRoleName() {
        assertThat(DevDataSeeder.emailFor(Role.SALES_MANAGER)).isEqualTo("sales.manager@store.test");
    }
}
