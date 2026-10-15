package com.example.attendance.controller;

import com.example.attendance.entity.FaceEmbedding;
import com.example.attendance.entity.Student;
import com.example.attendance.repository.FaceEmbeddingRepository;
import com.example.attendance.repository.StudentRepository;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Controller
@org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
public class RegistrationController {

    private static final String EMAIL_DOMAIN = "@tcetmumbai.in";

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private FaceEmbeddingRepository faceEmbeddingRepository;

    @Value("${upload.dir}")
    private String uploadDir;

    @Value("${face.service.url}")
    private String faceServiceUrl;

    // ---------- helpers ----------

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private boolean isValidCollegeEmail(String email) {
        return email.endsWith(EMAIL_DOMAIN) && email.length() > EMAIL_DOMAIN.length();
    }

    // excludeId = null for new student; student's own id when editing
    private boolean emailTaken(String email, Long excludeId) {
        return studentRepository.findAll().stream()
                .anyMatch(s -> email.equalsIgnoreCase(s.getEmail())
                        && (excludeId == null || s.getId().longValue() != excludeId));
    }

    // ---------- registration ----------

    @GetMapping("/register")
    public String showRegistrationForm() {
        return "register";
    }

    @PostMapping("/register")
    public String registerStudent(
            @RequestParam("name") String name,
            @RequestParam("rollNo") String rollNo,
            @RequestParam("course") String course,
            @RequestParam(value = "branch", required = false) String branch,
            @RequestParam("year") String year,
            @RequestParam("division") String division,
            @RequestParam("email") String email,
            @RequestParam("photo") MultipartFile photo,
            Model model,
            RedirectAttributes redirectAttributes) {

        // 1. Validate everything BEFORE saving photo / calling face service
        String finalEmail = normalizeEmail(email);
        if (!isValidCollegeEmail(finalEmail)) {
            model.addAttribute("error", "Email must be a valid " + EMAIL_DOMAIN + " address.");
            return "register";
        }
        if (emailTaken(finalEmail, null)) {
            model.addAttribute("error", "Email already registered.");
            return "register";
        }
        if (studentRepository.existsByRollNoAndCourseAndBranchAndYearAndDivision(rollNo, course, branch, year,
                division)) {
            model.addAttribute("error", "Student with this Roll No, Course, Branch, Year and Division already exists.");
            return "register";
        }

        Path filePath = null;
        try {
            // 2. Save file locally
            File uploadDirFile = new File(uploadDir + "/students/");
            if (!uploadDirFile.exists()) {
                uploadDirFile.mkdirs();
            }

            String originalFilename = photo.getOriginalFilename();
            String fileName = rollNo + "_" + System.currentTimeMillis() + "_" + originalFilename;
            filePath = Paths.get(uploadDir + "/students/" + fileName);
            Files.write(filePath, photo.getBytes());

            // 3. Call Python service
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("roll_no", rollNo);
            body.add("image", new FileSystemResource(filePath.toFile()));

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    faceServiceUrl + "/register-face",
                    requestEntity,
                    Map.class);

            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && Boolean.TRUE.equals(responseBody.get("success"))) {
                @SuppressWarnings("unchecked")
                List<Double> embeddingList = (List<Double>) responseBody.get("embedding");
                String embeddingString = embeddingList.toString();
                embeddingString = embeddingString.substring(1, embeddingString.length() - 1); // remove [ ]

                Student student = new Student();
                student.setName(name);
                student.setRollNo(rollNo);
                student.setCourse(course);
                student.setBranch(branch);
                student.setYear(year);
                student.setDivision(division);
                student.setEmail(finalEmail);
                student = studentRepository.save(student);

                FaceEmbedding faceEmbedding = new FaceEmbedding();
                faceEmbedding.setStudentId(student.getId());
                faceEmbedding.setEmbedding(embeddingString);
                faceEmbedding.setImagePath(filePath.toString());
                faceEmbeddingRepository.save(faceEmbedding);

                redirectAttributes.addFlashAttribute("name", name);
                redirectAttributes.addFlashAttribute("rollNo", rollNo);
                return "redirect:/registration-success";
            } else {
                Files.deleteIfExists(filePath);
                model.addAttribute("error", "No face detected in the photo, please try again");
                return "register";
            }

        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (filePath != null) {
                    Files.deleteIfExists(filePath);
                }
            } catch (Exception ignored) {
            }
            model.addAttribute("error", "Face recognition service is unavailable, please try again later.");
            return "register";
        }
    }

    @GetMapping("/registration-success")
    public String registrationSuccess() {
        return "registration-success";
    }

    // ---------- edit email (backfill old students) ----------

    @GetMapping("/edit-email/{id}")
    public String showEditEmailForm(@PathVariable("id") Long id, Model model) {
        Student student = studentRepository.findById(id).orElseThrow();
        model.addAttribute("student", student);
        return "edit-email";
    }

    @PostMapping("/edit-email/{id}")
    public String updateEmail(@PathVariable("id") Long id, @RequestParam("email") String email,
            RedirectAttributes redirectAttributes) {
        Student student = studentRepository.findById(id).orElseThrow();
        String finalEmail = normalizeEmail(email);

        if (!isValidCollegeEmail(finalEmail)) {
            redirectAttributes.addFlashAttribute("error", "Email must be a valid " + EMAIL_DOMAIN + " address.");
            return "redirect:/edit-email/" + id;
        }
        if (emailTaken(finalEmail, id)) {
            redirectAttributes.addFlashAttribute("error", "Email already in use.");
            return "redirect:/edit-email/" + id;
        }
        student.setEmail(finalEmail);
        studentRepository.save(student);
        redirectAttributes.addFlashAttribute("success", "Email updated successfully.");
        return "redirect:/edit-email/" + id;
    }
}