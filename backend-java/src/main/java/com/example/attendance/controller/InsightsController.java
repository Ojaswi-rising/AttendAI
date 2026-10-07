package com.example.attendance.controller;

import com.example.attendance.dto.InsightsResponseDTO;
import com.example.attendance.repository.StudentRepository;
import com.example.attendance.service.InsightsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/insights")
public class InsightsController {

    private final InsightsService insightsService;
    private final StudentRepository studentRepository;

    public InsightsController(InsightsService insightsService, StudentRepository studentRepository) {
        this.insightsService = insightsService;
        this.studentRepository = studentRepository;
    }

    @GetMapping
    public String showInsights(
            @RequestParam(required = false) String course,
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String division,
            Model model) {

        InsightsResponseDTO insightsResponse = insightsService.generateInsights(course, year, division);

        model.addAttribute("insights", insightsResponse.getTextInsights());
        model.addAttribute("students", insightsResponse.getStudentInsights());
        model.addAttribute("overallAverage", insightsResponse.getOverallAverage());

        model.addAttribute("courses", studentRepository.findDistinctCourses());
        model.addAttribute("years", studentRepository.findDistinctYears());
        model.addAttribute("divisions", studentRepository.findDistinctDivisions());

        // Keep current filter values
        model.addAttribute("selectedCourse", course);
        model.addAttribute("selectedYear", year);
        model.addAttribute("selectedDivision", division);

        return "insights";
    }
}
