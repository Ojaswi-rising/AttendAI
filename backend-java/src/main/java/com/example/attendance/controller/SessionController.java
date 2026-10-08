package com.example.attendance.controller;

import com.example.attendance.entity.Attendance;
import com.example.attendance.entity.AttendanceSession;
import com.example.attendance.entity.FaceEmbedding;
import com.example.attendance.entity.FaceDetection;
import com.example.attendance.entity.SessionPhoto;
import com.example.attendance.entity.Student;
import com.example.attendance.repository.AttendanceRepository;
import com.example.attendance.repository.AttendanceSessionRepository;
import com.example.attendance.repository.FaceEmbeddingRepository;
import com.example.attendance.repository.FaceDetectionRepository;
import com.example.attendance.repository.SessionPhotoRepository;
import com.example.attendance.repository.StudentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.bind.annotation.ResponseBody;
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
import java.util.Set;
import java.util.HashSet;

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

    @Autowired
    private FaceDetectionRepository faceDetectionRepository;

    @Value("${upload.dir}")
    private String uploadDir;

    @Value("${face.service.url}")
    private String faceServiceUrl;

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
        
        File sessionDir = new File(uploadDir + "/sessions/" + id);
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
                            faceServiceUrl + "/recognize-group",
                            requestEntity,
                            List.class
                    );

                    List<Map<String, Object>> results = response.getBody();
                    if (results != null) {
                        for (Map<String, Object> result : results) {
                            String matchedRollNo = (String) result.get("matched_roll_no");

                            FaceDetection fd = new FaceDetection();
                            fd.setSessionPhoto(sessionPhoto);

                            List<Integer> bbox = (List<Integer>) result.get("bbox");
                            if (bbox != null && bbox.size() == 4) {
                                fd.setX1(bbox.get(0));
                                fd.setY1(bbox.get(1));
                                fd.setX2(bbox.get(2));
                                fd.setY2(bbox.get(3));
                            }
                            
                            if (result.get("image_width") instanceof Integer) {
                                fd.setImageWidth((Integer) result.get("image_width"));
                            }
                            if (result.get("image_height") instanceof Integer) {
                                fd.setImageHeight((Integer) result.get("image_height"));
                            }

                            fd.setMatchedRollNo(matchedRollNo);
                            
                            if (result.get("matched") instanceof Boolean) {
                                fd.setMatched((Boolean) result.get("matched"));
                            }
                            
                            Double confidence = null;
                            if (result.get("confidence") instanceof Double) {
                                confidence = (Double) result.get("confidence");
                            } else if (result.get("confidence") instanceof Integer) {
                                confidence = ((Integer) result.get("confidence")).doubleValue();
                            }
                            fd.setConfidence(confidence);

                            if (result.get("second_confidence") instanceof Number) {
                                fd.setSecondConfidence(((Number) result.get("second_confidence")).doubleValue());
                            }
                            if (result.get("ambiguous") instanceof Boolean) {
                                fd.setAmbiguous((Boolean) result.get("ambiguous"));
                            } else {
                                fd.setAmbiguous(false);
                            }
                            if (result.get("second_best_roll_no") instanceof String) {
                                fd.setSecondBestRollNo((String) result.get("second_best_roll_no"));
                            }

                            faceDetectionRepository.save(fd);

                            if (!"unknown".equals(matchedRollNo)) {
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

        List<Student> students;
        if (session.getBranch() != null && !session.getBranch().isEmpty()) {
            students = studentRepository.findByCourseAndBranchAndYearAndDivision(
                    session.getCourse(), session.getBranch(), session.getYear(), session.getDivision());
        } else {
            students = studentRepository.findByCourseAndYearAndDivision(
                    session.getCourse(), session.getYear(), session.getDivision());
        }

        Map<Long, Student> idToStudent = new HashMap<>();
        for (Student s : students) idToStudent.put(s.getId(), s);
        Map<String, Student> rollNoToStudent = new HashMap<>();
        for (Student s : students) rollNoToStudent.put(s.getRollNo(), s);

        List<SessionPhoto> sessionPhotos = sessionPhotoRepository.findBySessionId(id);
        List<FaceDetection> detections = sessionPhotos.isEmpty() ? new ArrayList<>() : faceDetectionRepository.findBySessionPhotoIn(sessionPhotos);

        Map<Long, Double> autoConfidence = new HashMap<>();
        Set<Long> manualStudentIds = new HashSet<>();

        for (FaceDetection fd : detections) {
            if (Boolean.TRUE.equals(fd.getManualOverride())) {
                if (fd.getStudentId() != null) {
                    manualStudentIds.add(fd.getStudentId());
                }
            } else {
                if (fd.getMatchedRollNo() != null && !"unknown".equals(fd.getMatchedRollNo()) && fd.getConfidence() != null && fd.getConfidence() > 0.45) {
                    Student student = rollNoToStudent.get(fd.getMatchedRollNo());
                    if (student != null) {
                        double conf = fd.getConfidence();
                        autoConfidence.put(student.getId(), Math.max(autoConfidence.getOrDefault(student.getId(), 0.0), conf));
                    }
                }
            }
        }

        List<Attendance> attendances = attendanceRepository.findBySessionId(id);
        attendanceRepository.deleteAll(attendances);

        for (Student student : students) {
            Attendance attendance = new Attendance();
            attendance.setSessionId(id);
            attendance.setStudentId(student.getId());
            attendance.setMarkedAt(LocalDateTime.now());

            if (manualStudentIds.contains(student.getId())) {
                attendance.setStatus("PRESENT");
                attendance.setMethod("MANUAL");
                attendance.setConfidence(1.0);
            } else if (autoConfidence.containsKey(student.getId())) {
                attendance.setStatus("PRESENT");
                attendance.setMethod("AUTO");
                attendance.setConfidence(autoConfidence.get(student.getId()));
            } else {
                attendance.setStatus("ABSENT");
                attendance.setMethod("AUTO");
                attendance.setConfidence(null);
            }
            attendanceRepository.save(attendance);
        }

        session.setStatus("FINALIZED");
        sessionRepository.save(session);

        sessionPhotos = sessionPhotoRepository.findBySessionId(id);
        if (!sessionPhotos.isEmpty()) {
            faceDetectionRepository.deleteBySessionPhotoIn(sessionPhotos);
        }

        for (SessionPhoto photo : sessionPhotos) {
            try {
                if (photo.getImagePath() != null) {
                    Files.deleteIfExists(Paths.get(photo.getImagePath()));
                }
            } catch (Exception e) {
                System.err.println("Warning: Failed to delete session photo " + photo.getImagePath() + ": " + e.getMessage());
            }
        }
        sessionPhotoRepository.deleteAll(sessionPhotos);

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
    @GetMapping("/{id}/detections")
    @ResponseBody
    public List<FaceDetection> getDetections(@PathVariable("id") Long id) {
        List<SessionPhoto> sessionPhotos = sessionPhotoRepository.findBySessionId(id);
        if (sessionPhotos.isEmpty()) {
            return new ArrayList<>();
        }
        return faceDetectionRepository.findBySessionPhotoIn(sessionPhotos);
    }

    @GetMapping("/{id}/photo/{photoId}")
    @ResponseBody
    public ResponseEntity<org.springframework.core.io.Resource> getSessionPhoto(@PathVariable("id") Long id, @PathVariable("photoId") Long photoId) {
        Optional<SessionPhoto> photoOpt = sessionPhotoRepository.findById(photoId);
        if (photoOpt.isEmpty() || !photoOpt.get().getSessionId().equals(id)) {
            return ResponseEntity.notFound().build();
        }
        File file = new File(photoOpt.get().getImagePath());
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }
        org.springframework.core.io.Resource resource = new FileSystemResource(file);
        
        String contentType = "image/jpeg";
        if (file.getName().toLowerCase().endsWith(".png")) {
            contentType = "image/png";
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    @GetMapping("/{id}/review")
    public String reviewSession(@PathVariable("id") Long id, Model model) {
        Optional<AttendanceSession> sessionOpt = sessionRepository.findById(id);
        if (sessionOpt.isEmpty()) {
            return "redirect:/";
        }
        AttendanceSession session = sessionOpt.get();

        List<SessionPhoto> photos = sessionPhotoRepository.findBySessionId(id);
        
        List<Student> students;
        if (session.getBranch() != null && !session.getBranch().isEmpty()) {
            students = studentRepository.findByCourseAndBranchAndYearAndDivision(
                    session.getCourse(), session.getBranch(), session.getYear(), session.getDivision());
        } else {
            students = studentRepository.findByCourseAndYearAndDivision(
                    session.getCourse(), session.getYear(), session.getDivision());
        }
        
        Map<String, String> rollNoToName = new HashMap<>();
        Map<Long, Student> idToStudent = new HashMap<>();
        for (Student s : students) {
            rollNoToName.put(s.getRollNo(), s.getName());
            idToStudent.put(s.getId(), s);
        }

        // Sort students numerically by roll number for the dropdown
        students.sort((s1, s2) -> {
            String r1 = s1.getRollNo();
            String r2 = s2.getRollNo();
            Integer i1 = null, i2 = null;
            try { if (r1 != null) i1 = Integer.parseInt(r1.trim()); } catch (Exception ignored) {}
            try { if (r2 != null) i2 = Integer.parseInt(r2.trim()); } catch (Exception ignored) {}
            if (i1 != null && i2 != null) return i1.compareTo(i2);
            if (i1 != null) return -1;
            if (i2 != null) return 1;
            return (r1 == null ? "" : r1).compareTo(r2 == null ? "" : r2);
        });
        
        List<FaceDetection> detections = photos.isEmpty() ? new ArrayList<>() : faceDetectionRepository.findBySessionPhotoIn(photos);
        Map<Long, List<Map<String, Object>>> photoDetectionsMap = new HashMap<>();
        
        for (FaceDetection fd : detections) {
            Map<String, Object> detMap = new HashMap<>();
            detMap.put("id", fd.getId());
            detMap.put("x1", fd.getX1());
            detMap.put("y1", fd.getY1());
            detMap.put("x2", fd.getX2());
            detMap.put("y2", fd.getY2());
            detMap.put("imageWidth", fd.getImageWidth());
            detMap.put("imageHeight", fd.getImageHeight());
            detMap.put("confidence", fd.getConfidence());
            detMap.put("manualOverride", fd.getManualOverride() != null ? fd.getManualOverride() : false);
            detMap.put("studentId", fd.getStudentId());
            
            boolean ambiguous = fd.getAmbiguous() != null ? fd.getAmbiguous() : false;
            detMap.put("ambiguous", ambiguous);
            
            String secondBestName = "Unknown";
            if (fd.getSecondBestRollNo() != null && rollNoToName.containsKey(fd.getSecondBestRollNo())) {
                secondBestName = rollNoToName.get(fd.getSecondBestRollNo());
            }
            detMap.put("secondBestName", secondBestName);
            
            boolean needsVerification = false;
            if (Boolean.TRUE.equals(fd.getManualOverride())) {
                needsVerification = false;
            } else if ("unknown".equals(fd.getMatchedRollNo()) || fd.getMatchedRollNo() == null || (fd.getConfidence() != null && fd.getConfidence() < 0.65) || ambiguous) {
                needsVerification = true;
            }
            detMap.put("needsVerification", needsVerification);
            
            String name = "Unknown";
            String roll = "unknown";
            
            if (Boolean.TRUE.equals(fd.getManualOverride())) {
                if (fd.getStudentId() != null && fd.getStudentId() == -1L) {
                    name = "Excluded (Absent)";
                    roll = "absent";
                } else if (fd.getStudentId() != null && idToStudent.containsKey(fd.getStudentId())) {
                    Student s = idToStudent.get(fd.getStudentId());
                    name = s.getName();
                    roll = s.getRollNo();
                }
            } else {
                roll = fd.getMatchedRollNo() != null ? fd.getMatchedRollNo() : "unknown";
                if (!"unknown".equals(roll) && rollNoToName.containsKey(roll)) {
                    name = rollNoToName.get(roll);
                }
            }
            
            detMap.put("matchedRollNo", roll);
            detMap.put("studentName", name);
            
            photoDetectionsMap
                .computeIfAbsent(fd.getSessionPhoto().getId(), k -> new ArrayList<>())
                .add(detMap);
        }

        model.addAttribute("attendanceSession", session);
        model.addAttribute("photos", photos);
        
        // Sort faces per photo so needsVerification is at the top
        for (List<Map<String, Object>> dets : photoDetectionsMap.values()) {
            dets.sort((d1, d2) -> {
                boolean v1 = (boolean) d1.get("needsVerification");
                boolean v2 = (boolean) d2.get("needsVerification");
                if (v1 == v2) return 0;
                return v1 ? -1 : 1;
            });
        }
        
        model.addAttribute("photoDetectionsMap", photoDetectionsMap);
        model.addAttribute("allStudents", students);

        return "session-review";
    }

    @PostMapping("/{id}/detection/{detectionId}/assign")
    public String assignDetection(
            @PathVariable("id") Long id,
            @PathVariable("detectionId") Long detectionId,
            @RequestParam(value = "studentId", required = false) Long studentId,
            RedirectAttributes redirectAttributes) {
            
        Optional<FaceDetection> fdOpt = faceDetectionRepository.findById(detectionId);
        if (fdOpt.isPresent()) {
            FaceDetection fd = fdOpt.get();
            if (fd.getSessionPhoto() != null && fd.getSessionPhoto().getSessionId().equals(id)) {
                fd.setStudentId(studentId);
                fd.setManualOverride(true);
                faceDetectionRepository.save(fd);
            }
        }
        return "redirect:/session/" + id + "/review";
    }
}
