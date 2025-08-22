package com.example.cloud.controller;

import com.example.cloud.model.dto.UserResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@Slf4j
@RestController
@RequestMapping("/api/user")
@Tag(name = "User Controller", description = "User API")
public class UserController {

    @GetMapping("/me")
    @Operation(summary = "Get current user name")
    public ResponseEntity<UserResponseDto> currentUser(Principal principal) {
        log.info("GET /api/user/me username: {}", principal.getName());
        return ResponseEntity.ok(new UserResponseDto(principal.getName()));
    }
}
