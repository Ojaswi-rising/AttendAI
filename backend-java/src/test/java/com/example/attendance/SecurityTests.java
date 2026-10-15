package com.example.attendance;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void unauthenticatedShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/student/dashboard"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "student@tcetmumbai.in", roles = {"STUDENT"})
    public void studentCannotAccessSessionCreation() throws Exception {
        mockMvc.perform(get("/session/new"))
               .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "student@tcetmumbai.in", roles = {"STUDENT"})
    public void studentCannotAccessRecords() throws Exception {
        mockMvc.perform(get("/records"))
               .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "teacher@tcetmumbai.in", roles = {"TEACHER"})
    public void teacherCannotAccessAdmin() throws Exception {
        mockMvc.perform(get("/admin/users"))
               .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@tcetmumbai.in", roles = {"ADMIN"})
    public void adminCanAccessAdmin() throws Exception {
        mockMvc.perform(get("/admin/users"))
               .andExpect(status().isOk());
    }
}
