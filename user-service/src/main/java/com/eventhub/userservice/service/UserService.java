package com.eventhub.userservice.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.eventhub.userservice.dto.CreateUserRequest;
import com.eventhub.userservice.dto.UserResponse;
import com.eventhub.userservice.entity.User;
import com.eventhub.userservice.entity.UserRole;
import com.eventhub.userservice.exception.UserNotFoundException;
import com.eventhub.userservice.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse createUser(CreateUserRequest request) {

        User user = new User();
        
        user.setName(request.getName());
        user.setEmail(request.getEmail());

        if (request.getRole() == null) {
            user.setRole(UserRole.USER);
        } else {
            user.setRole(request.getRole());
        }

        User savedUser = userRepository.save(user);

        return toResponse(savedUser);
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
                        () -> new RuntimeException(
                                "User not found with email: " + email
                        )
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
}