package ca.yarbond.bookclub.config;

import ca.yarbond.bookclub.model.User;
import ca.yarbond.bookclub.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class UserInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.admin.username:admin}")
    private String adminUsername;

    @Value("${app.security.admin.password:admin}")
    private String adminPassword;

    @Value("${app.security.user.username:user}")
    private String userUsername;

    @Value("${app.security.user.password:user}")
    private String userPassword;

    public UserInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Only initialize if no users exist
        if (userRepository.count() == 0) {
            User adminUser = new User();
            adminUser.setUsername(adminUsername);
            adminUser.setPasswordHash(passwordEncoder.encode(adminPassword));
            adminUser.setRole(User.Role.ADMIN);
            userRepository.save(adminUser);

            User regularUser = new User();
            regularUser.setUsername(userUsername);
            regularUser.setPasswordHash(passwordEncoder.encode(userPassword));
            regularUser.setRole(User.Role.USER);
            userRepository.save(regularUser);
        }
    }
}