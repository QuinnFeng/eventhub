package com.eventhub.userservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.eventhub.userservice.dto.CreateUserRequest;
import com.eventhub.userservice.dto.UpdateUserRequest;
import com.eventhub.userservice.dto.UpdateUserRoleRequest;
import com.eventhub.userservice.dto.UserResponse;
import com.eventhub.userservice.entity.UserRole;
import com.eventhub.userservice.service.JwtService;
import com.eventhub.userservice.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;


    // ---------------------------------------------------------
    // POST /api/users
    // ---------------------------------------------------------

    @Test
    void createUserSuccess() throws Exception {

        CreateUserRequest request = new CreateUserRequest();
        request.setName("John Doe");
        request.setEmail("john@example.com");
        request.setPassword("password123");

        UserResponse response = new UserResponse(
                1L,
                "John Doe",
                "john@example.com",
                UserRole.USER
        );

        when(userService.createUser(any(CreateUserRequest.class)))
                .thenReturn(response);

        mockMvc.perform(
                post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.name").value("John Doe"))
        .andExpect(jsonPath("$.email")
                .value("john@example.com"))
        .andExpect(jsonPath("$.role").value("USER"));
    }


    // ---------------------------------------------------------
    // GET /api/users
    // ---------------------------------------------------------

    @Test
    void getAllUsersSuccess() throws Exception {

        List<UserResponse> users = List.of(
                new UserResponse(
                        1L,
                        "John Doe",
                        "john@example.com",
                        UserRole.USER
                ),
                new UserResponse(
                        2L,
                        "Jane Doe",
                        "jane@example.com",
                        UserRole.ORGANIZER
                )
        );

        when(userService.getAllUsers())
                .thenReturn(users);

        mockMvc.perform(
                get("/api/users")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].name")
                .value("John Doe"))
        .andExpect(jsonPath("$[1].name")
                .value("Jane Doe"));
    }


    // ---------------------------------------------------------
    // GET /api/users/{id}
    // ---------------------------------------------------------

    @Test
    void getUserByIdSuccess() throws Exception {

        UserResponse response = new UserResponse(
                1L,
                "John Doe",
                "john@example.com",
                UserRole.USER
        );

        when(userService.getUserById(1L))
                .thenReturn(response);

        mockMvc.perform(
                get("/api/users/1")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.name")
                .value("John Doe"))
        .andExpect(jsonPath("$.email")
                .value("john@example.com"));
    }


    // ---------------------------------------------------------
    // GET /api/users/email/{email}
    // ---------------------------------------------------------

    @Test
    void getUserByEmailSuccess() throws Exception {

        UserResponse response = new UserResponse(
                1L,
                "John Doe",
                "john@example.com",
                UserRole.USER
        );

        when(userService.getUserByEmail("john@example.com"))
                .thenReturn(response);

        mockMvc.perform(
                get("/api/users/email/john@example.com")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.email")
                .value("john@example.com"));
    }


    // ---------------------------------------------------------
    // PUT /api/users/{id}
    // ---------------------------------------------------------

    @Test
    void updateUserSuccess() throws Exception {

        UpdateUserRequest request = new UpdateUserRequest();
        request.setName("John Updated");
        request.setEmail("john.updated@example.com");
        request.setPassword("newPassword123");

        UserResponse response = new UserResponse(
                1L,
                "John Updated",
                "john.updated@example.com",
                UserRole.USER
        );

        when(userService.updateUser(
                eq(1L),
                any(UpdateUserRequest.class)))
                .thenReturn(response);

        mockMvc.perform(
                put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.name")
                .value("John Updated"))
        .andExpect(jsonPath("$.email")
                .value("john.updated@example.com"));
    }


    // ---------------------------------------------------------
    // DELETE /api/users/{id}
    // ---------------------------------------------------------

    @Test
    void deleteUserSuccess() throws Exception {

        doNothing()
                .when(userService)
                .deleteUser(1L);

        mockMvc.perform(
                delete("/api/users/1")
        )
        .andExpect(status().isNoContent());

        verify(userService)
                .deleteUser(1L);
    }


    // ---------------------------------------------------------
    // PUT /api/users/{id}/role
    // ---------------------------------------------------------

    @Test
    void updateUserRoleSuccess() throws Exception {

        UpdateUserRoleRequest request =
                new UpdateUserRoleRequest();

        request.setRole(UserRole.ORGANIZER);

        UserResponse response = new UserResponse(
                1L,
                "John Doe",
                "john@example.com",
                UserRole.ORGANIZER
        );

        when(userService.updateUserRole(
                eq(1L),
                any(UpdateUserRoleRequest.class)))
                .thenReturn(response);

        mockMvc.perform(
                put("/api/users/1/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.role")
                .value("ORGANIZER"));
    }


    // ---------------------------------------------------------
    // Validation
    // ---------------------------------------------------------

    @Test
    void createUserValidationFailure() throws Exception {

        CreateUserRequest request = new CreateUserRequest();

        request.setName("");
        request.setEmail("not-an-email");
        request.setPassword("");

        mockMvc.perform(
                post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest());
    }
}