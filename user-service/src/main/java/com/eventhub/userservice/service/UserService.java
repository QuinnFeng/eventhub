package com.eventhub.userservice.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(UserRepository userRepository,PasswordEncoder passwordEncoder,JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public UserResponse createUser(CreateUserRequest request) {
    	
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException(request.getEmail());
        }

        User user = new User();
        
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.USER);
        User savedUser = userRepository.save(user);

        return toResponse(savedUser);
    }
    
    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {

            throw new InvalidCredentialsException();
        }

        String token = jwtService.generateToken(user);

        return new AuthResponse(
                token,
                toResponse(user)
        );
    }
    
    public UserResponse updateUser(Long id, UpdateUserRequest request) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        // Check whether the new email belongs to another user
        if (!user.getEmail().equals(request.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {

            throw new DuplicateEmailException(request.getEmail());
        }

        user.setName(request.getName());
        user.setEmail(request.getEmail());

        // Hash the new password before saving
        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        User updatedUser = userRepository.save(user);

        return toResponse(updatedUser);
    }

    public List<UserResponse> getAllUsers() {

        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public UserResponse getUserById(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(
                        () -> new UserNotFoundException(id)
                );

        return toResponse(user);
    }

    public UserResponse getUserByEmail(String email) {

    	   User user = userRepository.findByEmail(email)
    	            .orElseThrow(
    	                    () -> new UserNotFoundByEmailException(email)
    	            );

    	    return toResponse(user);
    }

    public void deleteUser(Long id) {

        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException(id);
        }

        userRepository.deleteById(id);
    }

    private UserResponse toResponse(User user) {

        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
    }
    
    public UserResponse updateUserRole(
            Long id,
            UpdateUserRoleRequest request) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        user.setRole(request.getRole());

        User updatedUser = userRepository.save(user);

        return toResponse(updatedUser);
    }
}