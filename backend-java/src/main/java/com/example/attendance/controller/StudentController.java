package com.example.attendance.controller;

import com.example.attendance.dto.StudentInsightDTO;
import com.example.attendance.entity.Student;
import com.example.attendance.entity.User;
import com.example.attendance.repository.AttendanceRepository;
import com.example.attendance.repository.UserRepository;
import com.example.attendance.service.InsightsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

@Controller
@RequestMapping("/student")
@PreAuthorize("hasRole('STUDENT') or hasRole('ADMIN')")
public class StudentController {

    private final UserRepository userRepository;
    private final InsightsService insightsService;
    private final AttendanceRepository attendanceRepository;

    public StudentController(UserRepository userRepository, InsightsService insightsService, AttendanceRepository attendanceRepository) {
        this.userRepository = userRepository;
        this.insightsService = insightsService;
        this.attendanceRepository = attendanceRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        User user = userRepository.findByEmailIgnoreCase(principal.getName()).orElse(null);
        if (user == null || user.getStudent() == null) {
            model.addAttribute("error", "Student record not found for this account. Contact your teacher.");
            return "student-error";
        }
        
        Student student = user.getStudent();
        model.addAttribute("student", student);
        
        // Find insight for this student using existing service
        StudentInsightDTO insight = insightsService.generateInsights(student.getCourse(), student.getYear(), student.getDivision())
            .getStudentInsights().stream()
            .filter(s -> s.getRollNo().equals(student.getRollNo()))
            .findFirst()
            .orElse(new StudentInsightDTO());
            
        model.addAttribute("insights", insight);
        model.addAttribute("attendances", attendanceRepository.findStudentAttendanceHistory(student.getId()));
        
        return "student-dashboard";
    }
}
