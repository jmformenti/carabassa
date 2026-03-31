package org.atypical.carabassa.security.configuration;

import org.atypical.carabassa.security.entity.UserEntity;
import org.atypical.carabassa.security.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "carabassa.auth.enabled", havingValue = "true")
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${carabassa.auth.admin.username:admin}")
    private String adminUsername;

    @Value("${carabassa.auth.admin.password:changeme}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            UserEntity admin = new UserEntity(
                    adminUsername,
                    passwordEncoder.encode(adminPassword),
                    "ADMIN");
            userRepository.save(admin);
            logger.info("Created default admin user: '{}'", adminUsername);
            logger.warn("Change the default admin password immediately!");
        }
    }

}
