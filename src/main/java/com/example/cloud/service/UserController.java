package com.example.cloud.service;

import com.example.cloud.model.dto.UserResponseDto;
import com.example.cloud.util.UserContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/user")
public class UserController {

    @GetMapping("/me")
    public ResponseEntity<?> currentUser() {
        String userName = UserContext.getCurrentUserName();
        return ResponseEntity.ok(new UserResponseDto(userName));
    }
}
