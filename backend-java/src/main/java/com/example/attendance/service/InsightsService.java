package com.example.attendance.service;

import com.example.attendance.dto.InsightsResponseDTO;
import com.example.attendance.dto.StudentInsightDTO;
import com.example.attendance.entity.Attendance;
import com.example.attendance.entity.AttendanceSession;
import com.example.attendance.entity.Student;
import com.example.attendance.repository.AttendanceRepository;
import com.example.attendance.repository.AttendanceSessionRepository;
import com.example.attendance.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InsightsService {

    private final StudentRepository studentRepository;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceSessionRepository attendanceSessionRepository;

    public InsightsService(StudentRepository studentRepository,
                           AttendanceRepository attendanceRepository,
                           AttendanceSessionRepository attendanceSessionRepository) {
        this.studentRepository = studentRepository;
        this.attendanceRepository = attendanceRepository;
        this.attendanceSessionRepository = attendanceSessionRepository;
    }

    public InsightsResponseDTO generateInsights(String course, String year, String division) {
        // Fetch all students matching the filter
        List<Student> students = fetchStudents(course, year, division);

        // Fetch finalized sessions matching the filter
        List<AttendanceSession> sessions = fetchFinalizedSessions(course, year, division);
        int totalFinalizedSessions = sessions.size();

        List<StudentInsightDTO> studentInsights = new ArrayList<>();
        double totalAttendancePercentage = 0;
        int safeCount = 0, warningCount = 0, criticalCount = 0;
        List<String> textInsights = new ArrayList<>();

        for (Student student : students) {
            StudentInsightDTO dto = new StudentInsightDTO();
            dto.setRollNo(student.getRollNo());
            dto.setName(student.getName());

            LocalDate studentRegistrationDate = student.getCreatedAt() != null ? student.getCreatedAt().toLocalDate() : LocalDate.MIN;
            List<AttendanceSession> studentSessions = sessions.stream()
                    .filter(s -> !s.getSessionDate().isBefore(studentRegistrationDate))
                    .collect(Collectors.toList());
            int totalFinalizedSessionsForStudent = studentSessions.size();

            if (totalFinalizedSessionsForStudent == 0) {
                dto.setAttendancePercentage(0.0);
                dto.setTrend("Stable");
                dto.setEngagementScore(0);
                dto.setRiskStatus("WARNING");
                studentInsights.add(dto);
                continue;
            }

            // Fetch all attendance records for this student in the filtered finalized sessions
            List<Attendance> studentAttendances = fetchAttendancesForStudent(student.getId(), studentSessions);

            // Calculate overall attendance percentage
            long presentCount = studentAttendances.stream()
                    .filter(a -> "PRESENT".equalsIgnoreCase(a.getStatus()))
                    .count();
            
            double attendancePercentage = ((double) presentCount / totalFinalizedSessionsForStudent) * 100;
            dto.setAttendancePercentage(Math.round(attendancePercentage * 100.0) / 100.0);
            totalAttendancePercentage += attendancePercentage;

            // Determine Trend (Last 5 sessions vs overall)
            String trend = calculateTrend(studentAttendances, attendancePercentage);
            dto.setTrend(trend);

            // Calculate Consistency Score and Engagement Score
            int consistencyScore = calculateConsistencyScore(studentAttendances);
            int engagementScore = (int) Math.round((attendancePercentage * 0.7) + (consistencyScore * 0.3));
            dto.setEngagementScore(engagementScore);

            // Determine Risk Status
            String riskStatus = determineRiskStatus(attendancePercentage);
            dto.setRiskStatus(riskStatus);

            if ("CRITICAL".equals(riskStatus)) {
                criticalCount++;
                textInsights.add(student.getName() + " (" + student.getRollNo() + ") is in CRITICAL status. Needs immediate follow-up.");
            } else if ("WARNING".equals(riskStatus)) {
                warningCount++;
            } else {
                safeCount++;
            }

            if ("Declining".equals(trend)) {
                textInsights.add(student.getName() + " (" + student.getRollNo() + ") shows a Declining attendance trend recently.");
            }

            studentInsights.add(dto);
        }

        double overallAverage = students.isEmpty() ? 0 : (totalAttendancePercentage / students.size());
        overallAverage = Math.round(overallAverage * 100.0) / 100.0;

        String summary = String.format("Overall average attendance is %.2f%%. Risk distribution: %d SAFE, %d WARNING, %d CRITICAL.",
                overallAverage, safeCount, warningCount, criticalCount);
        textInsights.add(0, summary); // Add summary at the top

        InsightsResponseDTO response = new InsightsResponseDTO();
        response.setStudentInsights(studentInsights);
        response.setTextInsights(textInsights);
        response.setOverallAverage(overallAverage);

        return response;
    }

    private List<Student> fetchStudents(String course, String year, String division) {
        if (course != null && !course.isEmpty() && year != null && !year.isEmpty() && division != null && !division.isEmpty()) {
            return studentRepository.findByCourseAndYearAndDivision(course, year, division);
        }
        return studentRepository.findAll();
    }

    private List<AttendanceSession> fetchFinalizedSessions(String course, String year, String division) {
        // Need to add query method in repository
        List<AttendanceSession> sessions = attendanceSessionRepository.findAll();
        return sessions.stream()
                .filter(s -> "FINALIZED".equalsIgnoreCase(s.getStatus()))
                .filter(s -> (course == null || course.isEmpty() || course.equals(s.getCourse())))
                .filter(s -> (year == null || year.isEmpty() || year.equals(s.getYear())))
                .filter(s -> (division == null || division.isEmpty() || division.equals(s.getDivision())))
                .sorted(Comparator.comparing(AttendanceSession::getSessionDate).thenComparing(AttendanceSession::getSessionTime))
                .collect(Collectors.toList());
    }

    private List<Attendance> fetchAttendancesForStudent(Long studentId, List<AttendanceSession> sortedSessions) {
        List<Attendance> allAttendances = attendanceRepository.findAll().stream()
                .filter(a -> a.getStudentId().equals(studentId))
                .collect(Collectors.toList());

        List<Attendance> filteredAttendances = new ArrayList<>();
        for (AttendanceSession session : sortedSessions) {
            allAttendances.stream()
                    .filter(a -> a.getSessionId().equals(session.getId()))
                    .findFirst()
                    .ifPresentOrElse(
                            filteredAttendances::add,
                            () -> {
                                // If no record is found for a finalized session, we consider it ABSENT
                                Attendance absentRecord = new Attendance();
                                absentRecord.setStatus("ABSENT");
                                filteredAttendances.add(absentRecord);
                            }
                    );
        }
        return filteredAttendances;
    }

    private String calculateTrend(List<Attendance> chronologicallySortedAttendances, double overallPercentage) {
        if (chronologicallySortedAttendances.size() < 5) return "Stable";
        
        List<Attendance> last5 = chronologicallySortedAttendances.subList(chronologicallySortedAttendances.size() - 5, chronologicallySortedAttendances.size());
        long last5PresentCount = last5.stream().filter(a -> "PRESENT".equalsIgnoreCase(a.getStatus())).count();
        double last5Percentage = ((double) last5PresentCount / 5.0) * 100;

        if (last5Percentage > overallPercentage + 5.0) {
            return "Improving";
        } else if (last5Percentage < overallPercentage - 5.0) {
            return "Declining";
        }
        return "Stable";
    }

    private int calculateConsistencyScore(List<Attendance> attendances) {
        int consecutiveAbsences = 0;
        int gapsOf2OrMore = 0;

        for (Attendance a : attendances) {
            if ("ABSENT".equalsIgnoreCase(a.getStatus())) {
                consecutiveAbsences++;
            } else {
                if (consecutiveAbsences >= 2) {
                    gapsOf2OrMore++;
                }
                consecutiveAbsences = 0;
            }
        }
        if (consecutiveAbsences >= 2) {
            gapsOf2OrMore++;
        }

        int score = 100 - (gapsOf2OrMore * 10);
        return Math.max(score, 0); // floored at 0
    }

    private String determineRiskStatus(double attendancePercentage) {
        if (attendancePercentage >= 75) return "SAFE";
        if (attendancePercentage >= 60) return "WARNING";
        return "CRITICAL";
    }
}
