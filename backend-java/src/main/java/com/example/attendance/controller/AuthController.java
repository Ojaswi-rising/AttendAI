package com.example.attendance.controller;

import com.example.attendance.entity.Role;
import com.example.attendance.entity.Student;
import com.example.attendance.entity.User;
import com.example.attendance.entity.UserStatus;
import com.example.attendance.repository.StudentRepository;
import com.example.attendance.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class AuthController {
    
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, StudentRepository studentRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/signup")
    public String showSignupPage() {
        return "signup";
    }
    
    @PostMapping("/signup")
    public String processSignup(@RequestParam("email") String email,
                                @RequestParam("password") String password,
                                @RequestParam("confirmPassword") String confirmPassword,
                                RedirectAttributes redirectAttributes) {
        email = email.trim();
        if (!email.toLowerCase().endsWith("@tcetmumbai.in")) {
            redirectAttributes.addFlashAttribute("error", "Email must end with @tcetmumbai.in");
            return "redirect:/signup";
        }
        if (!password.equals(confirmPassword) || password.length() < 8) {
            redirectAttributes.addFlashAttribute("error", "Passwords must match and be at least 8 characters");
            return "redirect:/signup";
        }
        
        String finalEmail = email;
        Optional<Student> studentOpt = studentRepository.findAll().stream().filter(s -> finalEmail.equalsIgnoreCase(s.getEmail())).findFirst();
        if (studentOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Your email is not registered by your teacher. Contact your teacher.");
            return "redirect:/signup";
        }
        
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "User already exists with this email.");
            return "redirect:/signup";
        }
        
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(Role.STUDENT);
        user.setStatus(UserStatus.ACTIVE);
        user.setStudent(studentOpt.get());
        userRepository.save(user);
        
        redirectAttributes.addFlashAttribute("success", "Signup successful. Please login.");
        return "redirect:/login";
    }
}
