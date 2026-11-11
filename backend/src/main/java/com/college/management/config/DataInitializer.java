
package com.college.management.config;

import com.college.management.model.*;
import com.college.management.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedUsers(UserRepository users, PasswordEncoder encoder) {
        return args -> {
            create(users, encoder, "System Admin", "admin@college.com",
                    "Admin@123", Role.ADMIN, null, "CSE");

            create(users, encoder, "Demo Teacher", "teacher@college.com",
                    "Teacher@123", Role.TEACHER, null, "CSE");

            create(users, encoder, "Demo Student", "student@college.com",
                    "Student@123", Role.STUDENT, "STU001", "CSE");
        };
    }

    private void create(UserRepository users, PasswordEncoder encoder,
                        String name, String email, String password,
                        Role role, String roll, String dept) {

        if (!users.existsByEmail(email)) {
            AppUser u = new AppUser();
            u.setName(name);
            u.setEmail(email);
            u.setPassword(encoder.encode(password));
            u.setRole(role);
            u.setRollNumber(roll);
            u.setDepartment(dept);

            users.save(u);
        }
    }
}
