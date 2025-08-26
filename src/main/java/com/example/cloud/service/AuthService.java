package com.example.cloud.service;

import com.example.cloud.model.dto.request.UserAuthDto;
import com.example.cloud.model.dto.response.UserResponseDto;
import com.example.cloud.model.entity.User;
import com.example.cloud.repository.UserRepository;
import jakarta.persistence.EntityExistsException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contextRepository;

    public UserResponseDto authenticateUser(UserAuthDto userAuthDto,
                                            HttpServletRequest request, HttpServletResponse response) {

        authenticateUserAndSetContext(userAuthDto, request, response);

        log.info("User {} authenticated", userAuthDto.getUsername());

        return new UserResponseDto(userAuthDto.getUsername());
    }


    public User registerUser(UserAuthDto userDto,
                             HttpServletRequest request, HttpServletResponse response) {

        String encodedPassword = passwordEncoder.encode(userDto.getPassword());
        User user = new User(userDto.getUsername(), encodedPassword);

        try {
            userRepository.save(user);
            log.info("User {} registered", userDto.getUsername());

            authenticateUserAndSetContext(userDto, request, response);

            return user;
        } catch (DataIntegrityViolationException e) {
            throw new EntityExistsException("User " + userDto.getUsername() + " already exists");
        }


    }

    private void authenticateUserAndSetContext(UserAuthDto userAuthDto, HttpServletRequest request, HttpServletResponse response) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        userAuthDto.getUsername(),
                        userAuthDto.getPassword()
                ));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, request, response);
    }
}
