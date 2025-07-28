package com.example.cloud.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorizeRequests -> authorizeRequests
                .requestMatchers("/","/api/auth/**", "/static/**", "/css/**", "/js/**").permitAll()
                .anyRequest().authenticated()
        ).formLogin(formLogin -> formLogin
                        .loginPage("/api/auth/sign-in")
                        .loginProcessingUrl("/api/auth/sign-in")
                        .defaultSuccessUrl("/")
                        .failureUrl("/api/auth/sign-in?error=true")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/api/auth/sign-out")
                        .permitAll())
                .sessionManagement(session -> session.maximumSessions(1));

        return http.build();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
