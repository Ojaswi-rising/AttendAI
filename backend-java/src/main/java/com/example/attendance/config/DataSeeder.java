package com.example.attendance.config;

import com.example.attendance.entity.Role;
import com.example.attendance.entity.User;
import com.example.attendance.entity.UserStatus;
import com.example.attendance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin-email:admin@example.com}")
    private String adminEmail;

    @Value("${app.seed.admin-password:admin}")
    private String adminPassword;

    @Value("${app.seed.teacher-email:teacher@example.com}")
    private String teacherEmail;

    @Value("${app.seed.teacher-password:teacher}")
    private String teacherPassword;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            User admin = new User();
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole(Role.ADMIN);
            admin.setStatus(UserStatus.ACTIVE);
            userRepository.save(admin);

            User teacher = new User();
            teacher.setEmail(teacherEmail);
            teacher.setPassword(passwordEncoder.encode(teacherPassword));
            teacher.setRole(Role.TEACHER);
            teacher.setStatus(UserStatus.ACTIVE);
            userRepository.save(teacher);
        }
    }
}
