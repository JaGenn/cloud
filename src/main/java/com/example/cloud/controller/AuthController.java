package com.example.cloud.controller;

import com.example.cloud.model.dto.UserDto;
import com.example.cloud.model.entity.User;
import com.example.cloud.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/sign-in")
    public String signIn(@RequestParam(value = "error", required = false) String error,
                         Model model) {
        if (error != null) {
            model.addAttribute("error", "Неверный логин или пароль");
        }
        return "sign-in";
    }

    @GetMapping("/sign-up")
    public String signUp(Model model) {
        model.addAttribute("userDto", new UserDto());
        return "sign-up";
    }

    @PostMapping("/sign-up")
    public String signUp(@Valid @ModelAttribute("userDto") UserDto userDto, BindingResult result, Model model) {

        if (result.hasErrors()) {
            return "sign-up";
        }

        User user = new User();
        user.setUsername(userDto.getUsername());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            model.addAttribute("error", "Такой пользователь уже зарегистрирован");
            return "sign-up";
        }

        return "redirect:/";
    }
}
