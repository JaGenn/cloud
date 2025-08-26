package com.example.cloud.util;

import com.example.cloud.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;


public class UserContext {

    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        return userDetails.getId();
    }

    public static String getUserFolder(Long id) {
        String userPath = "user-" + id + "-files";
        return userPath.endsWith("/") ? userPath : userPath + "/";
    }

}
