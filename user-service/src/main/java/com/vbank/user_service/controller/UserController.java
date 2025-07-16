package com.vbank.user_service.controller;
import com.virtualbank.userservice.model.User;
import com.virtualbank.userservice.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        if (userService.findByUsername(req.getUsername()).isPresent() ||
                userService.findByEmail(req.getEmail()).isPresent()) {
            return ResponseEntity.status(409).body(Map.of("message", "Username or email already exists"));
        }
        User user = userService.register(req.getUsername(), req.getEmail(), req.getPassword(), req.getFirstName(), req.getLastName());
        return ResponseEntity.status(201).body(Map.of("userId", user.getId(), "username", user.getUsername(), "message", "User registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        Optional<User> userOpt = userService.findByUsername(req.getUsername());
        if (userOpt.isEmpty() || !userService.checkPassword(req.getPassword(), userOpt.get().getPasswordHash())) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid username or password"));
        }
        User user = userOpt.get();
        return ResponseEntity.ok(Map.of("userId", user.getId(), "username", user.getUsername()));
    }

    @GetMapping("/{userId}/profile")
    public ResponseEntity<?> getProfile(@PathVariable UUID userId) {
        return userService.findById(userId)
                .map(u -> ResponseEntity.ok(Map.of(
                        "userId", u.getId(),
                        "username", u.getUsername(),
                        "email", u.getEmail(),
                        "firstName", u.getFirstName(),
                        "lastName", u.getLastName()
                )))
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("message", "User not found")));
    }

    @Data
    static class RegisterRequest {
        private String username;
        private String email;
        private String password;
        private String firstName;
        private String lastName;
    }

    @Data
    static class LoginRequest {
        private String username;
        private String password;
    }
}
