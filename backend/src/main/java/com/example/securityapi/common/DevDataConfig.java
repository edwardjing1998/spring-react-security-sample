package com.example.securityapi.common;

import com.example.securityapi.user.AppUser;
import com.example.securityapi.user.Role;
import com.example.securityapi.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("dev")
public class DevDataConfig {
    @Bean
    CommandLineRunner demoAdmin(UserRepository users, PasswordEncoder encoder) {
        return args -> {
            if (!users.existsByEmailIgnoreCase("admin@example.com")) {
                users.save(new AppUser("Demo Admin", "admin@example.com",
                        encoder.encode("ChangeMe123!"), Role.ADMIN));
            }
        };
    }
}

