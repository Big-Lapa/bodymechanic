package com.lapin.bodymechanic.config;

import com.lapin.bodymechanic.domain.Role;
import com.lapin.bodymechanic.domain.User;
import com.lapin.bodymechanic.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.EnumSet;

/**
 * Creates the "admin" account (or re-enables it if it is disabled) when ADMIN_PASSWORD is set,
 * so no default credentials have to live in the migrations. An active admin is never touched,
 * so a password changed in the profile survives restarts.
 */
@Component
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private static final String ADMIN = "admin";
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.password:}")
    private String adminPassword;

    public AdminInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(adminPassword)) {
            return;
        }
        if (adminPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException(
                    "ADMIN_PASSWORD must be at least " + MIN_PASSWORD_LENGTH + " characters long");
        }

        User admin = userRepository.findByUsername(ADMIN);
        if (admin != null && admin.isActive()) {
            return;
        }
        if (admin == null) {
            admin = new User();
            admin.setUsername(ADMIN);
            log.info("Creating admin account from ADMIN_PASSWORD");
        } else {
            log.warn("Admin account was disabled: re-enabling it and resetting its password from ADMIN_PASSWORD");
        }

        if (!StringUtils.hasText(admin.getEmail())) {
            admin.setEmail("admin@localhost");
        }
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setActive(true);
        admin.setRoles(EnumSet.of(Role.USER, Role.ADMIN));
        userRepository.save(admin);
    }
}
