package com.eventhub.userservice.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.eventhub.userservice.dto.AuthResponse;
import com.eventhub.userservice.dto.CreateUserRequest;
import com.eventhub.userservice.dto.LoginRequest;
import com.eventhub.userservice.dto.UpdateUserRequest;
import com.eventhub.userservice.dto.UpdateUserRoleRequest;
import com.eventhub.userservice.dto.UserResponse;
import com.eventhub.userservice.entity.User;
import com.eventhub.userservice.entity.UserRole;
import com.eventhub.userservice.exception.DuplicateEmailException;
import com.eventhub.userservice.exception.InvalidCredentialsException;
import com.eventhub.userservice.exception.UserNotFoundByEmailException;
import com.eventhub.userservice.exception.UserNotFoundException;
import com.eventhub.userservice.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {

        user = new User();

        user.setId(1L);
        user.setName("John Doe");
        user.setEmail("john@example.com");
        user.setPassword("encoded-password");
        user.setRole(UserRole.USER);
    }

    // ---------------------------------------------------------
    // createUser()
    // ---------------------------------------------------------

    @Test
    void createUser_shouldCreateUserWithUserRole() {

        CreateUserRequest request = new CreateUserRequest();

        request.setName("John Doe");
        request.setEmail("john@example.com");
        request.setPassword("password123");

        when(userRepository.existsByEmail("john@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("password123"))
                .thenReturn("encoded-password");

        when(userRepository.save(any(User.class)))
                .thenReturn(user);

        UserResponse response =
                userService.createUser(request);

        assertNotNull(response);

        assertEquals(1L, response.getId());
        assertEquals("John Doe", response.getName());
        assertEquals("john@example.com", response.getEmail());
        assertEquals(UserRole.USER, response.getRole());

        verify(userRepository)
                .existsByEmail("john@example.com");

        verify(passwordEncoder)
                .encode("password123");

        verify(userRepository)
                .save(any(User.class));
    }

    @Test
    void createUser_shouldAlwaysAssignUserRole() {

        CreateUserRequest request = new CreateUserRequest();

        request.setName("John Doe");
        request.setEmail("john@example.com");
        request.setPassword("password123");

        when(userRepository.existsByEmail(anyString()))
                .thenReturn(false);

        when(passwordEncoder.encode(anyString()))
                .thenReturn("encoded-password");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        userService.createUser(request);

        ArgumentCaptor<User> captor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository).save(captor.capture());

        User savedUser = captor.getValue();

        assertEquals(UserRole.USER, savedUser.getRole());
        assertEquals("John Doe", savedUser.getName());
        assertEquals("john@example.com", savedUser.getEmail());
        assertEquals("encoded-password", savedUser.getPassword());
    }

    @Test
    void createUser_shouldThrowWhenEmailAlreadyExists() {

        CreateUserRequest request = new CreateUserRequest();

        request.setName("John Doe");
        request.setEmail("john@example.com");
        request.setPassword("password123");

        when(userRepository.existsByEmail("john@example.com"))
                .thenReturn(true);

        assertThrows(
                DuplicateEmailException.class,
                () -> userService.createUser(request)
        );

        verify(userRepository, never())
                .save(any(User.class));

        verify(passwordEncoder, never())
                .encode(anyString());
    }

    // ---------------------------------------------------------
    // getAllUsers()
    // ---------------------------------------------------------

    @Test
    void getAllUsers_shouldReturnAllUsers() {

        User secondUser = new User();

        secondUser.setId(2L);
        secondUser.setName("Jane Doe");
        secondUser.setEmail("jane@example.com");
        secondUser.setPassword("encoded-password");
        secondUser.setRole(UserRole.ORGANIZER);

        when(userRepository.findAll())
                .thenReturn(List.of(user, secondUser));

        List<UserResponse> responses =
                userService.getAllUsers();

        assertEquals(2, responses.size());

        assertEquals(
                "john@example.com",
                responses.get(0).getEmail()
        );

        assertEquals(
                "jane@example.com",
                responses.get(1).getEmail()
        );

        assertEquals(
                UserRole.ORGANIZER,
                responses.get(1).getRole()
        );

        verify(userRepository).findAll();
    }

    // ---------------------------------------------------------
    // getUserById()
    // ---------------------------------------------------------

    @Test
    void getUserById_shouldReturnUser() {

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        UserResponse response =
                userService.getUserById(1L);

        assertEquals(1L, response.getId());
        assertEquals("John Doe", response.getName());
        assertEquals("john@example.com", response.getEmail());
        assertEquals(UserRole.USER, response.getRole());

        verify(userRepository).findById(1L);
    }

    @Test
    void getUserById_shouldThrowWhenUserDoesNotExist() {

        when(userRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> userService.getUserById(999L)
        );

        verify(userRepository).findById(999L);
    }

    // ---------------------------------------------------------
    // getUserByEmail()
    // ---------------------------------------------------------

    @Test
    void getUserByEmail_shouldReturnUser() {

        when(userRepository.findByEmail("john@example.com"))
                .thenReturn(Optional.of(user));

        UserResponse response =
                userService.getUserByEmail("john@example.com");

        assertEquals(
                "john@example.com",
                response.getEmail()
        );

        verify(userRepository)
                .findByEmail("john@example.com");
    }

    @Test
    void getUserByEmail_shouldThrowWhenUserDoesNotExist() {

        when(userRepository.findByEmail("missing@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundByEmailException.class,
                () -> userService.getUserByEmail("missing@example.com")
        );

        verify(userRepository)
                .findByEmail("missing@example.com");
    }

    // ---------------------------------------------------------
    // deleteUser()
    // ---------------------------------------------------------

    @Test
    void deleteUser_shouldDeleteExistingUser() {

        when(userRepository.existsById(1L))
                .thenReturn(true);

        userService.deleteUser(1L);

        verify(userRepository)
                .existsById(1L);

        verify(userRepository)
                .deleteById(1L);
    }

    @Test
    void deleteUser_shouldThrowWhenUserDoesNotExist() {

        when(userRepository.existsById(999L))
                .thenReturn(false);

        assertThrows(
                UserNotFoundException.class,
                () -> userService.deleteUser(999L)
        );

        verify(userRepository, never())
                .deleteById(anyLong());
    }

    // ---------------------------------------------------------
    // updateUser()
    // ---------------------------------------------------------

    @Test
    void updateUser_shouldUpdateUser() {

        UpdateUserRequest request =
                new UpdateUserRequest();

        request.setName("John Updated");
        request.setEmail("john.updated@example.com");
        request.setPassword("newPassword");

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(userRepository.existsByEmail(
                "john.updated@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("newPassword"))
                .thenReturn("new-encoded-password");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response =
                userService.updateUser(1L, request);

        assertEquals(
                "John Updated",
                response.getName()
        );

        assertEquals(
                "john.updated@example.com",
                response.getEmail()
        );

        assertEquals(
                UserRole.USER,
                response.getRole()
        );

        verify(passwordEncoder)
                .encode("newPassword");

        verify(userRepository)
                .save(user);
    }

    @Test
    void updateUser_shouldAllowSameEmail() {

        UpdateUserRequest request =
                new UpdateUserRequest();

        request.setName("John Updated");
        request.setEmail("john@example.com");
        request.setPassword("newPassword");

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.encode("newPassword"))
                .thenReturn("new-encoded-password");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response =
                userService.updateUser(1L, request);

        assertEquals(
                "john@example.com",
                response.getEmail()
        );

        verify(userRepository, never())
                .existsByEmail(anyString());

        verify(userRepository)
                .save(user);
    }

    @Test
    void updateUser_shouldThrowWhenNewEmailAlreadyExists() {

        UpdateUserRequest request =
                new UpdateUserRequest();

        request.setName("John Updated");
        request.setEmail("another@example.com");
        request.setPassword("newPassword");

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(userRepository.existsByEmail(
                "another@example.com"))
                .thenReturn(true);

        assertThrows(
                DuplicateEmailException.class,
                () -> userService.updateUser(1L, request)
        );

        verify(userRepository, never())
                .save(any(User.class));

        verify(passwordEncoder, never())
                .encode(anyString());
    }

    @Test
    void updateUser_shouldThrowWhenUserDoesNotExist() {

        UpdateUserRequest request =
                new UpdateUserRequest();

        request.setName("John Updated");
        request.setEmail("john.updated@example.com");
        request.setPassword("newPassword");

        when(userRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> userService.updateUser(999L, request)
        );

        verify(userRepository, never())
                .save(any(User.class));
    }

    // ---------------------------------------------------------
    // login()
    // ---------------------------------------------------------

    @Test
    void login_shouldReturnJwtToken() {

        LoginRequest request =
                new LoginRequest();

        request.setEmail("john@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("john@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "password123",
                "encoded-password"))
                .thenReturn(true);

        when(jwtService.generateToken(user))
                .thenReturn("jwt-token");

        AuthResponse response =
                userService.login(request);

        assertNotNull(response);

        assertEquals(
                "jwt-token",
                response.getToken()
        );

        assertNotNull(response.getUser());

        assertEquals(
                "john@example.com",
                response.getUser().getEmail()
        );

        assertEquals(
                UserRole.USER,
                response.getUser().getRole()
        );

        verify(passwordEncoder)
                .matches(
                        "password123",
                        "encoded-password"
                );

        verify(jwtService)
                .generateToken(user);
    }

    @Test
    void login_shouldThrowWhenEmailDoesNotExist() {

        LoginRequest request =
                new LoginRequest();

        request.setEmail("missing@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("missing@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidCredentialsException.class,
                () -> userService.login(request)
        );

        verify(passwordEncoder, never())
                .matches(anyString(), anyString());

        verify(jwtService, never())
                .generateToken(any(User.class));
    }

    @Test
    void login_shouldThrowWhenPasswordIsWrong() {

        LoginRequest request =
                new LoginRequest();

        request.setEmail("john@example.com");
        request.setPassword("wrong-password");

        when(userRepository.findByEmail("john@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "wrong-password",
                "encoded-password"))
                .thenReturn(false);

        assertThrows(
                InvalidCredentialsException.class,
                () -> userService.login(request)
        );

        verify(jwtService, never())
                .generateToken(any(User.class));
    }

    // ---------------------------------------------------------
    // updateUserRole()
    // ---------------------------------------------------------

    @Test
    void updateUserRole_shouldChangeRole() {

        UpdateUserRoleRequest request =
                new UpdateUserRoleRequest();

        request.setRole(UserRole.ORGANIZER);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response =
                userService.updateUserRole(1L, request);

        assertEquals(
                UserRole.ORGANIZER,
                response.getRole()
        );

        assertEquals(
                UserRole.ORGANIZER,
                user.getRole()
        );

        verify(userRepository)
                .save(user);
    }

    @Test
    void updateUserRole_shouldThrowWhenUserDoesNotExist() {

        UpdateUserRoleRequest request =
                new UpdateUserRoleRequest();

        request.setRole(UserRole.ADMIN);

        when(userRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> userService.updateUserRole(999L, request)
        );

        verify(userRepository, never())
                .save(any(User.class));
    }
}