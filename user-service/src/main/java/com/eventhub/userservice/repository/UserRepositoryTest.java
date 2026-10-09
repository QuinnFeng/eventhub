package com.eventhub.userservice.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;

import org.springframework.test.context.TestPropertySource;

import com.eventhub.userservice.entity.User;
import com.eventhub.userservice.entity.UserRole;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/postgres",
        "spring.datasource.username=postgres",
        "spring.datasource.password=mercury",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User createTestUser(String name, String email) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword("encoded-test-password");
        user.setRole(UserRole.USER);
        return user;
    }

    @Test
    void saveUserSuccess() {
        User user = createTestUser("John Doe", "john@example.com");

        User savedUser = userRepository.saveAndFlush(user);

        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getName()).isEqualTo("John Doe");
        assertThat(savedUser.getEmail()).isEqualTo("john@example.com");
        assertThat(savedUser.getRole()).isEqualTo(UserRole.USER);
    }

    @Test
    void findByEmailSuccess() {
        User user = createTestUser("John Doe", "john@example.com");
        userRepository.saveAndFlush(user);

        Optional<User> result =
                userRepository.findByEmail("john@example.com");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("John Doe");
    }

    @Test
    void findByEmailNotFound() {
        Optional<User> result =
                userRepository.findByEmail("missing@example.com");

        assertThat(result).isEmpty();
    }

    @Test
    void existsByEmailReturnsTrue() {
        userRepository.saveAndFlush(
                createTestUser("John Doe", "john@example.com"));

        assertThat(userRepository.existsByEmail("john@example.com"))
                .isTrue();
    }

    @Test
    void existsByEmailReturnsFalse() {
        assertThat(userRepository.existsByEmail("missing@example.com"))
                .isFalse();
    }

    @Test
    void deleteUserSuccess() {
        User savedUser = userRepository.saveAndFlush(
                createTestUser("John Doe", "john@example.com"));

        userRepository.deleteById(savedUser.getId());
        userRepository.flush();

        assertThat(userRepository.findById(savedUser.getId())).isEmpty();
    }
}
