package com.securitylog.init;

import com.securitylog.entity.User;
import com.securitylog.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            String defaultPassword = "ChangeMe123!";
            userRepository.save(new User("secadmin", passwordEncoder.encode(defaultPassword)));
            log.warn("=============================================================");
            log.warn("No users found. Created default admin account.");
            log.warn("  Username: secadmin");
            log.warn("  Password: {}", defaultPassword);
            log.warn("Change this password immediately in production!");
            log.warn("=============================================================");
        }
    }
}
