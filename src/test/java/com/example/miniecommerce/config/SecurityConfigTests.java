package com.example.miniecommerce.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SecurityConfigTests {
    @Test void missingOrInvalidConfigurationCreatesNoAdministrator() {
        var config = new SecurityConfig();
        assertThrows(UsernameNotFoundException.class, () -> config.adminUsers("", "").loadUserByUsername("admin"));
        assertThrows(UsernameNotFoundException.class, () -> config.adminUsers("admin@example.invalid", "plaintext")
            .loadUserByUsername("admin@example.invalid"));
    }
}
