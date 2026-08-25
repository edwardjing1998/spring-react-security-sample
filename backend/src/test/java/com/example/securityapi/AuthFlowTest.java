package com.example.securityapi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:securitytest",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.profiles.active=test"
})
@AutoConfigureMockMvc
class AuthFlowTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void signupLoginAndProtectedEndpoint() throws Exception {
        String signup = """
                {"name":"Test User","email":"test@example.com","password":"StrongPass123!"}
                """;

        String result = mvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON).content(signup))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("USER"))
                .andReturn().getResponse().getContentAsString();

        JsonNode response = objectMapper.readTree(result);
        String token = response.get("accessToken").asText();

        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"));

        mvc.perform(get("/api/admin/summary").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedEndpointRejectsAnonymousRequest() throws Exception {
        mvc.perform(get("/api/users/me")).andExpect(status().isForbidden());
    }
}

