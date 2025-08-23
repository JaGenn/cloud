package com.example.cloud.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FrontendController {

    @GetMapping(value = {"/registration", "/login", "/files/**"})
    public String handleRefresh() {
        return "forward:/index.html";
    }

}