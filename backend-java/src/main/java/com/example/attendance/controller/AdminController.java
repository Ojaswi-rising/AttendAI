package com.example.attendance.controller;

import com.example.attendance.entity.Role;
import com.example.attendance.entity.User;
import com.example.attendance.entity.UserStatus;
import com.example.attendance.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/users")
    public String listUsers(Model model) {
        model.addAttribute("users", userRepository.findAll());
        return "admin-users";
    }

    @PostMapping("/users/{id}/enable")
    public String enableUser(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(id).orElseThrow();
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        redirectAttributes.addFlashAttribute("success", "User enabled.");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/disable")
    public String disableUser(@PathVariable("id") Long id, Principal principal, RedirectAttributes redirectAttributes) {
        User loggedIn = userRepository.findByEmailIgnoreCase(principal.getName()).orElseThrow();
        if (loggedIn.getId().equals(id)) {
            redirectAttributes.addFlashAttribute("error", "Cannot disable yourself.");
            return "redirect:/admin/users";
        }
        User user = userRepository.findById(id).orElseThrow();
        user.setStatus(UserStatus.DISABLED);
        userRepository.save(user);
        redirectAttributes.addFlashAttribute("success", "User disabled.");
        return "redirect:/admin/users";
    }

    @GetMapping("/teachers/new")
    public String showNewTeacherForm() {
        return "admin-teacher-new";
    }

    @PostMapping("/teachers/new")
    public String createTeacher(@RequestParam("email") String email,
                                @RequestParam("password") String password,
                                RedirectAttributes redirectAttributes) {
        email = email.trim();
        if (!email.contains("@")) {
            redirectAttributes.addFlashAttribute("error", "Invalid email.");
            return "redirect:/admin/teachers/new";
        }
        if (password.length() < 8) {
            redirectAttributes.addFlashAttribute("error", "Password must be at least 8 chars.");
            return "redirect:/admin/teachers/new";
        }
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "User already exists.");
            return "redirect:/admin/teachers/new";
        }
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(Role.TEACHER);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        redirectAttributes.addFlashAttribute("success", "Teacher created.");
        return "redirect:/admin/users";
    }
}
