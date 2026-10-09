package com.eventhub.userservice;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.eventhub.userservice.entity.User;
import com.eventhub.userservice.entity.UserRole;
import com.eventhub.userservice.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class JwtAuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void loginAndAccessProtectedEndpointWithJwt() throws Exception {

        // 1. Register a user through the actual REST endpoint.
        String registrationJson = """
                {
                    "name": "JWT Test User",
                    "email": "jwt-test@example.com",
                    "password": "TestPassword123!"
                }
                """;

        MvcResult registrationResult = mockMvc.perform(
                post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson)
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email")
                .value("jwt-test@example.com"))
        .andExpect(jsonPath("$.role").value("USER"))
        .andReturn();

        JsonNode registeredUser = objectMapper.readTree(
                registrationResult.getResponse().getContentAsString());

        long userId = registeredUser.get("id").asLong();

        // 2. Log in through the actual authentication endpoint.
        String loginJson = """
                {
                    "email": "jwt-test@example.com",
                    "password": "TestPassword123!"
                }
                """;

        MvcResult loginResult = mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isNotEmpty())
        .andExpect(jsonPath("$.user.email")
                .value("jwt-test@example.com"))
        .andReturn();

        JsonNode loginResponse = objectMapper.readTree(
                loginResult.getResponse().getContentAsString());

        String userToken = loginResponse.get("token").asText();

        // 3. A USER token must not access the ADMIN-only endpoint.
        mockMvc.perform(
                put("/api/users/{id}/role", userId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"USER"}
                                """)
        )
        .andExpect(status().isForbidden());

        // 4. Promote the user directly in the test database.
        // This avoids using the ADMIN-only endpoint to create an admin.
        User user = userRepository.findById(userId).orElseThrow();
        user.setRole(UserRole.ADMIN);
        userRepository.saveAndFlush(user);

        // 5. Log in again to obtain a token containing the ADMIN role.
        MvcResult adminLoginResult = mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user.role").value("ADMIN"))
        .andReturn();

        JsonNode adminLoginResponse = objectMapper.readTree(
                adminLoginResult.getResponse().getContentAsString());

        String adminToken = adminLoginResponse.get("token").asText();

        // 6. An ADMIN token must be allowed to access the endpoint.
        mockMvc.perform(
                put("/api/users/{id}/role", userId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"USER"}
                                """)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void protectedEndpointRejectsMissingToken() throws Exception {

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointRejectsInvalidToken() throws Exception {

        mockMvc.perform(
                get("/api/users")
                        .header("Authorization", "Bearer invalid.token.value")
        )
        .andExpect(status().isUnauthorized());
    }
}
