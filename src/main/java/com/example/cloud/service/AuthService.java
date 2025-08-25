package com.example.cloud.service;

import com.example.cloud.model.dto.request.UserAuthDto;
import com.example.cloud.model.dto.response.UserResponseDto;
import com.example.cloud.model.entity.User;
import com.example.cloud.security.CustomUserDetails;
import com.example.cloud.repository.UserRepository;
import jakarta.persistence.EntityExistsException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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

    public UserResponseDto authenticateUser(UserAuthDto userAuthDto, HttpServletRequest request, HttpServletResponse response) {
        try {

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            userAuthDto.getUsername(),
                            userAuthDto.getPassword()
                    ));

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            contextRepository.saveContext(context, request, response);

            log.info("User {} authenticated", userAuthDto.getUsername());

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            return new UserResponseDto(userDetails.getUsername());

        } catch (BadCredentialsException e) {
            throw new UsernameNotFoundException("Invalid username or password");
        }
    }

    public User registerUser(UserAuthDto userDto) {

        User user = new User();
        user.setUsername(userDto.getUsername());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new EntityExistsException("User " + userDto.getUsername() + " already exists");
        }

        log.info("User {} registered", userDto.getUsername());
        return user;
    }
}
