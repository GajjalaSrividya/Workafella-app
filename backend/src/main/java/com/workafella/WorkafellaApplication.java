package com.workafella;

import com.workafella.auth.AppUser;
import com.workafella.auth.Role;
import com.workafella.auth.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
@EnableScheduling
public class WorkafellaApplication {
    public static void main(String[] args) {
        SpringApplication.run(WorkafellaApplication.class, args);
    }

    @Bean
    CommandLineRunner seedAdmin(UserRepository users, PasswordEncoder encoder,
                                @Value("${app.admin-email}") String email,
                                @Value("${app.admin-password}") String password) {
        return args -> {
            if (users.findByEmail(email).isEmpty()) {
                AppUser admin = new AppUser();
                admin.setFullName("Workafella Admin");
                admin.setEmail(email);
                admin.setPasswordHash(encoder.encode(password));
                admin.setRole(Role.ADMIN);
                users.save(admin);
            }
        };
    }
}
