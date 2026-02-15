package it.unicas.spring.springai.config;

import it.unicas.spring.springai.model.User;
import it.unicas.spring.springai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            // Crea utente admin di default se non esiste
            if (!userRepository.existsByUsername("admin")) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setEmail("admin@example.com");
                admin.setNome("Admin");
                admin.setCognome("Sistema");
                admin.setEnabled(true);
                admin.setRoles(Set.of("ADMIN", "USER"));

                userRepository.save(admin);
                log.info("Created default admin user (username: admin, password: admin123)");
            }

            // Crea utente test se non esiste
            if (!userRepository.existsByUsername("user")) {
                User user = new User();
                user.setUsername("user");
                user.setPassword(passwordEncoder.encode("user123"));
                user.setEmail("user@example.com");
                user.setNome("Utente");
                user.setCognome("Test");
                user.setEnabled(true);
                user.setRoles(Set.of("USER"));

                userRepository.save(user);
                log.info("Created default test user (username: user, password: user123)");
            }
        };
    }
}
