package com.example.attendance.controller;

import com.example.attendance.entity.FaceEmbedding;
import com.example.attendance.entity.Student;
import com.example.attendance.repository.FaceEmbeddingRepository;
import com.example.attendance.repository.StudentRepository;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Controller
public class RegistrationController {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private FaceEmbeddingRepository faceEmbeddingRepository;

    private static final String UPLOAD_DIR = "uploads/students/";

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
            @RequestParam("photo") MultipartFile photo,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (studentRepository.existsByRollNoAndCourseAndBranchAndYearAndDivision(rollNo, course, branch, year, division)) {
            model.addAttribute("error", "Student with this Roll No, Course, Branch, Year and Division already exists.");
            return "register";
        }

        try {
            // Save file locally
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            String originalFilename = photo.getOriginalFilename();
            String fileName = rollNo + "_" + System.currentTimeMillis() + "_" + originalFilename;
            Path filePath = Paths.get(UPLOAD_DIR + fileName);
            Files.write(filePath, photo.getBytes());

            // Call Python service
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("roll_no", rollNo);
            body.add("image", new FileSystemResource(filePath.toFile()));

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "http://localhost:8000/register-face",
                    requestEntity,
                    Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && Boolean.TRUE.equals(responseBody.get("success"))) {
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
                // Delete the photo if python fails
                Files.deleteIfExists(filePath);
                model.addAttribute("error", "No face detected in the photo, please try again");
                return "register";
            }

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Face recognition service is unavailable, please try again later.");
            return "register";
        }
    }

    @GetMapping("/registration-success")
    public String registrationSuccess() {
        return "registration-success";
    }
}
