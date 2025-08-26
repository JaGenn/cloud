package com.example.cloud.service;

import com.example.cloud.BaseIntegrationTest;
import com.example.cloud.model.dto.request.UserAuthDto;
import com.example.cloud.model.dto.response.UserResponseDto;
import com.example.cloud.model.entity.User;
import com.example.cloud.repository.UserRepository;
import jakarta.persistence.EntityExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.Assert.assertEquals;


public class AuthServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Mock
    private MockHttpServletRequest request;

    @Mock
    private MockHttpServletResponse response;

    @BeforeEach
    void clearDatabase() {
        userRepository.deleteAll();
    }

    @Test
    void registerUser_success() {
        UserAuthDto registrationDto = new UserAuthDto("testuser", "password123");

        User registeredUser = authService.registerUser(registrationDto, request, response);

        assertThat(registeredUser).isNotNull();
        assertThat(registeredUser.getUsername()).isEqualTo("testuser");
        assertThat(passwordEncoder.matches("password123", registeredUser.getPassword())).isTrue();

        Optional<User> savedUser = userRepository.findByUsername("testuser");
        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().getUsername()).isEqualTo("testuser");
    }

    @Test
    void shouldFailRegistrationWhenUserAlreadyExists() {
        UserAuthDto firstUser = new UserAuthDto("testuser", "password123");
        UserAuthDto secondUser = new UserAuthDto("testuser", "password123");

        authService.registerUser(firstUser, request, response);

        assertThatThrownBy(() -> authService.registerUser(secondUser, request, response))
                .isInstanceOf(EntityExistsException.class)
                .hasMessageContaining("User testuser already exists");
    }


    @Test
    void authenticateUser_success() {
        UserAuthDto registrationDto = new UserAuthDto("testuser", "password123");
        authService.registerUser(registrationDto, request, response);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        UserResponseDto responseDto = authService.authenticateUser(registrationDto, request, response);
        assertThat(responseDto).isNotNull();
        assertEquals("testuser", responseDto.username());
    }

    @Test
    void authenticateUser_invalidUserData() {
        UserAuthDto registrationDto = new UserAuthDto("testuser", "password123");
        authService.registerUser(registrationDto, request, response);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        UserAuthDto wrongPasswordDto = new UserAuthDto("testuser", "wrongPassword");
        assertThatThrownBy(() -> authService.authenticateUser(wrongPasswordDto, request, response))
                .isInstanceOf(BadCredentialsException.class);
    }


}
