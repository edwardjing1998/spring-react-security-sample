package com.example.securityapi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.securityapi.security.JwtService;
import com.example.securityapi.profile.UserProfileRepository;
import com.example.securityapi.user.AppUser;
import com.example.securityapi.user.Role;
import com.example.securityapi.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:profiletest",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.profiles.active=test"
})
@AutoConfigureMockMvc
class UserProfileAdminTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired UserProfileRepository profiles;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;

    private AppUser admin;
    private AppUser regularUser;

    @BeforeEach
    void setUp() {
        profiles.deleteAll();
        users.deleteAll();
        admin = users.save(new AppUser("Admin", "profile-admin@example.com",
                passwordEncoder.encode("StrongPass123!"), Role.ADMIN));
        regularUser = users.save(new AppUser("Student", "profile-student@example.com",
                passwordEncoder.encode("StrongPass123!"), Role.USER));
    }

    @Test
    void adminCanCreateUpdateListAndDeleteProfile() throws Exception {
        String token = jwtService.createToken(admin);

        mvc.perform(post("/api/admin/user-profiles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": %d,
                                  "firstName": "Jamie",
                                  "lastName": "Student",
                                  "displayName": "Jamie S.",
                                  "preferredLanguage": "en-US",
                                  "timezone": "America/Los_Angeles",
                                  "avatarUrl": "https://example.com/avatar.png"
                                }
                                """.formatted(regularUser.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(regularUser.getId()))
                .andExpect(jsonPath("$.userEmail").value("profile-student@example.com"));

        mvc.perform(put("/api/admin/user-profiles/{userId}", regularUser.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Jamie",
                                  "lastName": "Student",
                                  "displayName": "Jamie Updated",
                                  "preferredLanguage": "zh-CN",
                                  "timezone": "America/New_York",
                                  "avatarUrl": null
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Jamie Updated"));

        mvc.perform(get("/api/admin/user-profiles")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(regularUser.getId()));

        mvc.perform(delete("/api/admin/user-profiles/{userId}", regularUser.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/admin/user-profiles/{userId}", regularUser.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void regularUserCannotManageProfiles() throws Exception {
        String token = jwtService.createToken(regularUser);
        mvc.perform(get("/api/admin/user-profiles")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
