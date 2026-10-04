package com.example.attendance.controller;

import com.example.attendance.dto.RecordDTO;
import com.example.attendance.repository.AttendanceRepository;
import com.example.attendance.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/records")
public class RecordsController {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private StudentRepository studentRepository;

    private List<RecordDTO> fetchRecords(String course, String year, String division, String subject, LocalDate fromDate, LocalDate toDate) {
        return attendanceRepository.findAttendanceRecords(
                (course == null || course.isEmpty()) ? null : course,
                (year == null || year.isEmpty()) ? null : year,
                (division == null || division.isEmpty()) ? null : division,
                (subject == null || subject.isEmpty()) ? null : subject,
                fromDate,
                toDate
        );
    }

    @GetMapping
    public String showRecords(
            @RequestParam(required = false) String course,
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String division,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Model model) {

        List<RecordDTO> records = fetchRecords(course, year, division, subject, fromDate, toDate);
        long presentCount = records.stream().filter(r -> "PRESENT".equals(r.getStatus())).count();
        long absentCount = records.stream().filter(r -> "ABSENT".equals(r.getStatus())).count();

        model.addAttribute("records", records);
        model.addAttribute("totalCount", records.size());
        model.addAttribute("presentCount", presentCount);
        model.addAttribute("absentCount", absentCount);
        
        model.addAttribute("courses", studentRepository.findDistinctCourses());
        model.addAttribute("years", studentRepository.findDistinctYears());
        model.addAttribute("divisions", studentRepository.findDistinctDivisions());

        // Keep current filter values
        model.addAttribute("selectedCourse", course);
        model.addAttribute("selectedYear", year);
        model.addAttribute("selectedDivision", division);
        model.addAttribute("subject", subject);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);

        return "records";
    }

    @GetMapping("/export.csv")
    public void exportCsv(
            @RequestParam(required = false) String course,
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String division,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            HttpServletResponse response) throws IOException {

        response.setContentType("text/csv");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"attendance_records.csv\"");
        
        List<RecordDTO> records = fetchRecords(course, year, division, subject, fromDate, toDate);

        PrintWriter writer = response.getWriter();
        // UTF-8 BOM
        writer.write('\ufeff');
        writer.println("Date,Subject,Course,Year,Division,Roll No,Name,Status,Confidence,Method");

        for (RecordDTO r : records) {
            writer.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                    escapeCsv(r.getSessionDate() != null ? r.getSessionDate().toString() : ""),
                    escapeCsv(r.getSubject()),
                    escapeCsv(r.getCourse()),
                    escapeCsv(r.getYear()),
                    escapeCsv(r.getDivision()),
                    escapeCsv(r.getRollNo()),
                    escapeCsv(r.getStudentName()),
                    escapeCsv(r.getStatus()),
                    escapeCsv("MANUAL".equals(r.getMethod()) ? "Manual" : (r.getConfidence() != null ? String.format("%.2f", r.getConfidence()) : "-")),
                    escapeCsv(r.getMethod())
            );
        }
    }

    private String escapeCsv(String data) {
        if (data == null) return "";
        String escapedData = data.replaceAll("\\R", " ");
        if (data.contains(",") || data.contains("\"") || data.contains("'")) {
            escapedData = "\"" + escapedData.replace("\"", "\"\"") + "\"";
        }
        return escapedData;
    }
}
