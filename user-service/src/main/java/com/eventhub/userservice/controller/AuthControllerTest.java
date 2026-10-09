package com.eventhub.userservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.eventhub.userservice.dto.AuthResponse;
import com.eventhub.userservice.dto.LoginRequest;
import com.eventhub.userservice.dto.UserResponse;
import com.eventhub.userservice.entity.UserRole;
import com.eventhub.userservice.service.JwtService;
import com.eventhub.userservice.service.UserService;

import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void loginSuccess() throws Exception {

        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        UserResponse userResponse = new UserResponse(
                1L,
                "Test User",
                "test@example.com",
                UserRole.USER
        );

        AuthResponse authResponse =
                new AuthResponse(
                        "fake-jwt-token",
                        userResponse
                );

        when(userService.login(any(LoginRequest.class)))
                .thenReturn(authResponse);

        mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("fake-jwt-token"))
        .andExpect(jsonPath("$.user.id").value(1))
        .andExpect(jsonPath("$.user.name").value("Test User"))
        .andExpect(jsonPath("$.user.email")
                .value("test@example.com"))
        .andExpect(jsonPath("$.user.role").value("USER"));
    }

    @Test
    void loginInvalidCredentials() throws Exception {

        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrong-password");

        when(userService.login(any(LoginRequest.class)))
                .thenThrow(new com.eventhub.userservice.exception.InvalidCredentialsException());

        mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isUnauthorized());
    }

    @Test
    void loginValidationFailure() throws Exception {

        LoginRequest request = new LoginRequest();
        request.setEmail("");
        request.setPassword("");

        mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest());
    }
}