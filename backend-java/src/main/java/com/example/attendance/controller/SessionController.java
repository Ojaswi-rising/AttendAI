package com.example.attendance.controller;

import com.example.attendance.entity.Attendance;
import com.example.attendance.entity.AttendanceSession;
import com.example.attendance.entity.FaceEmbedding;
import com.example.attendance.entity.SessionPhoto;
import com.example.attendance.entity.Student;
import com.example.attendance.repository.AttendanceRepository;
import com.example.attendance.repository.AttendanceSessionRepository;
import com.example.attendance.repository.FaceEmbeddingRepository;
import com.example.attendance.repository.SessionPhotoRepository;
import com.example.attendance.repository.StudentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/session")
public class SessionController {

    @Autowired
    private AttendanceSessionRepository sessionRepository;

    @Autowired
    private SessionPhotoRepository sessionPhotoRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private FaceEmbeddingRepository faceEmbeddingRepository;

    private static final String SESSIONS_DIR = "uploads/sessions/";

    @GetMapping("/new")
    public String showNewSessionForm(Model model) {
        model.addAttribute("currentDate", LocalDate.now());
        model.addAttribute("currentTime", LocalTime.now());
        return "session-new";
    }

    @PostMapping("/new")
    public String createSession(
            @RequestParam("course") String course,
            @RequestParam(value = "branch", required = false) String branch,
            @RequestParam("year") String year,
            @RequestParam("division") String division,
            @RequestParam("subject") String subject,
            @RequestParam("sessionDate") LocalDate sessionDate,
            @RequestParam("sessionTime") LocalTime sessionTime) {

        AttendanceSession session = new AttendanceSession();
        session.setCourse(course);
        session.setBranch(branch);
        session.setYear(year);
        session.setDivision(division);
        session.setSubject(subject);
        session.setSessionDate(sessionDate);
        session.setSessionTime(sessionTime);
        session.setTeacherId(1L); // hardcoded for now
        session.setStatus("OPEN");

        session = sessionRepository.save(session);
        return "redirect:/session/" + session.getId() + "/upload";
    }

    @GetMapping("/{id}/upload")
    public String showUploadPage(@PathVariable("id") Long id, Model model) {
        Optional<AttendanceSession> sessionOpt = sessionRepository.findById(id);
        if (sessionOpt.isEmpty()) {
            return "redirect:/";
        }
        
        AttendanceSession session = sessionOpt.get();
        List<SessionPhoto> photos = sessionPhotoRepository.findBySessionId(id);
        List<Attendance> attendances = attendanceRepository.findBySessionId(id);

        model.addAttribute("attendanceSession", session);
        model.addAttribute("photos", photos);
        model.addAttribute("matchedCount", attendances.size());
        
        return "session-upload";
    }

    @PostMapping("/{id}/photos")
    public String uploadPhotos(
            @PathVariable("id") Long id,
            @RequestParam("photos") MultipartFile[] photos,
            RedirectAttributes redirectAttributes) {

        Optional<AttendanceSession> sessionOpt = sessionRepository.findById(id);
        if (sessionOpt.isEmpty()) {
            return "redirect:/";
        }
        AttendanceSession session = sessionOpt.get();

        if ("FINALIZED".equals(session.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "Session is already finalized.");
            return "redirect:/session/" + id + "/upload";
        }

        // Find relevant students
        List<Student> students;
        if (session.getBranch() != null && !session.getBranch().isEmpty()) {
            students = studentRepository.findByCourseAndBranchAndYearAndDivision(
                    session.getCourse(), session.getBranch(), session.getYear(), session.getDivision());
        } else {
            students = studentRepository.findByCourseAndYearAndDivision(
                    session.getCourse(), session.getYear(), session.getDivision());
        }

        // Build known_faces JSON
        List<Map<String, Object>> knownFacesList = new ArrayList<>();
        Map<String, Student> rollNoToStudent = new HashMap<>();

        for (Student student : students) {
            FaceEmbedding embedding = faceEmbeddingRepository.findByStudentId(student.getId());
            if (embedding != null && embedding.getEmbedding() != null) {
                String[] embStr = embedding.getEmbedding().split(",");
                List<Double> embList = new ArrayList<>();
                for (String s : embStr) {
                    embList.add(Double.parseDouble(s.trim()));
                }
                
                Map<String, Object> faceData = new HashMap<>();
                faceData.put("roll_no", student.getRollNo());
                faceData.put("embedding", embList);
                knownFacesList.add(faceData);
                
                rollNoToStudent.put(student.getRollNo(), student);
            }
        }

        String knownFacesJson;
        try {
            knownFacesJson = new ObjectMapper().writeValueAsString(knownFacesList);
        } catch (JsonProcessingException e) {
            redirectAttributes.addFlashAttribute("error", "Error preparing face data.");
            return "redirect:/session/" + id + "/upload";
        }

        RestTemplate restTemplate = new RestTemplate();
        
        File sessionDir = new File(SESSIONS_DIR + id);
        if (!sessionDir.exists()) {
            sessionDir.mkdirs();
        }

        for (MultipartFile photo : photos) {
            if (photo.isEmpty()) continue;

            try {
                String fileName = System.currentTimeMillis() + "_" + photo.getOriginalFilename();
                Path filePath = Paths.get(sessionDir.getPath() + "/" + fileName);
                Files.write(filePath, photo.getBytes());

                SessionPhoto sessionPhoto = new SessionPhoto();
                sessionPhoto.setSessionId(id);
                sessionPhoto.setImagePath(filePath.toString());
                sessionPhotoRepository.save(sessionPhoto);

                // Call python service
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                body.add("known_faces", knownFacesJson);
                body.add("image", new FileSystemResource(filePath.toFile()));

                HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                try {
                    ResponseEntity<List> response = restTemplate.postForEntity(
                            "http://localhost:8000/recognize-group",
                            requestEntity,
                            List.class
                    );

                    List<Map<String, Object>> results = response.getBody();
                    if (results != null) {
                        for (Map<String, Object> result : results) {
                            String matchedRollNo = (String) result.get("matched_roll_no");
                            if (!"unknown".equals(matchedRollNo)) {
                                Double confidence = (Double) result.get("confidence");
                                if (confidence != null && confidence > 0.45) {
                                    Student student = rollNoToStudent.get(matchedRollNo);
                                    if (student != null) {
                                        Optional<Attendance> existing = attendanceRepository.findBySessionIdAndStudentId(id, student.getId());
                                        if (existing.isEmpty()) {
                                            Attendance attendance = new Attendance();
                                            attendance.setSessionId(id);
                                            attendance.setStudentId(student.getId());
                                            attendance.setStatus("PRESENT");
                                            attendance.setConfidence(confidence);
                                            attendance.setMethod("AUTO");
                                            attendance.setMarkedAt(LocalDateTime.now());
                                            attendanceRepository.save(attendance);
                                        } else {
                                            Attendance attendance = existing.get();
                                            if (confidence > attendance.getConfidence()) {
                                                attendance.setConfidence(confidence);
                                                attendanceRepository.save(attendance);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    redirectAttributes.addFlashAttribute("error", "Python service unreachable or returned error.");
                }

            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", "Failed to process image: " + photo.getOriginalFilename());
            }
        }

        return "redirect:/session/" + id + "/upload";
    }
    @PostMapping("/{id}/finalize")
    public String finalizeSession(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        Optional<AttendanceSession> sessionOpt = sessionRepository.findById(id);
        if (sessionOpt.isEmpty()) {
            return "redirect:/";
        }
        AttendanceSession session = sessionOpt.get();
        if ("FINALIZED".equals(session.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "Session is already finalized.");
            return "redirect:/session/" + id + "/upload";
        }

        // Find relevant students
        List<Student> students;
        if (session.getBranch() != null && !session.getBranch().isEmpty()) {
            students = studentRepository.findByCourseAndBranchAndYearAndDivision(
                    session.getCourse(), session.getBranch(), session.getYear(), session.getDivision());
        } else {
            students = studentRepository.findByCourseAndYearAndDivision(
                    session.getCourse(), session.getYear(), session.getDivision());
        }

        List<Attendance> attendances = attendanceRepository.findBySessionId(id);
        List<Long> presentStudentIds = new ArrayList<>();
        for (Attendance a : attendances) {
            presentStudentIds.add(a.getStudentId());
        }

        for (Student student : students) {
            if (!presentStudentIds.contains(student.getId())) {
                Attendance attendance = new Attendance();
                attendance.setSessionId(id);
                attendance.setStudentId(student.getId());
                attendance.setStatus("ABSENT");
                attendance.setMethod("AUTO");
                attendance.setConfidence(null);
                attendance.setMarkedAt(LocalDateTime.now());
                attendanceRepository.save(attendance);
            }
        }

        session.setStatus("FINALIZED");
        sessionRepository.save(session);

        return "redirect:/session/" + id + "/summary";
    }

    @GetMapping("/{id}/summary")
    public String showSummary(@PathVariable("id") Long id, Model model) {
        Optional<AttendanceSession> sessionOpt = sessionRepository.findById(id);
        if (sessionOpt.isEmpty()) {
            return "redirect:/";
        }
        AttendanceSession session = sessionOpt.get();

        List<Student> students;
        if (session.getBranch() != null && !session.getBranch().isEmpty()) {
            students = studentRepository.findByCourseAndBranchAndYearAndDivision(
                    session.getCourse(), session.getBranch(), session.getYear(), session.getDivision());
        } else {
            students = studentRepository.findByCourseAndYearAndDivision(
                    session.getCourse(), session.getYear(), session.getDivision());
        }

        List<Attendance> attendances = attendanceRepository.findBySessionId(id);
        Map<Long, Attendance> attendanceMap = new HashMap<>();
        int presentCount = 0;
        int absentCount = 0;
        for (Attendance a : attendances) {
            attendanceMap.put(a.getStudentId(), a);
            if ("PRESENT".equals(a.getStatus())) {
                presentCount++;
            } else if ("ABSENT".equals(a.getStatus())) {
                absentCount++;
            }
        }

        List<Map<String, Object>> studentList = new ArrayList<>();
        for (Student student : students) {
            Map<String, Object> map = new HashMap<>();
            map.put("name", student.getName());
            map.put("rollNo", student.getRollNo());
            Attendance a = attendanceMap.get(student.getId());
            if (a != null) {
                map.put("status", a.getStatus());
                map.put("confidence", a.getConfidence());
            } else {
                map.put("status", "UNKNOWN");
                map.put("confidence", null);
            }
            studentList.add(map);
        }

        // Sort students numerically by roll number
        studentList.sort((m1, m2) -> {
            String r1 = (String) m1.get("rollNo");
            String r2 = (String) m2.get("rollNo");
            Integer i1 = null;
            Integer i2 = null;
            try {
                if (r1 != null) i1 = Integer.parseInt(r1.trim());
            } catch (NumberFormatException ignored) {}
            try {
                if (r2 != null) i2 = Integer.parseInt(r2.trim());
            } catch (NumberFormatException ignored) {}
            
            if (i1 != null && i2 != null) {
                return i1.compareTo(i2);
            } else if (i1 != null) {
                return -1;
            } else if (i2 != null) {
                return 1;
            } else {
                return (r1 == null ? "" : r1).compareTo(r2 == null ? "" : r2);
            }
        });

        model.addAttribute("attendanceSession", session);
        model.addAttribute("totalExpected", students.size());
        model.addAttribute("presentCount", presentCount);
        model.addAttribute("absentCount", absentCount);
        model.addAttribute("students", studentList);

        return "session-summary";
    }
}
