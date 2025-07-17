package com.vbank.user_service.service;

import com.vbank.user_service.model.User;
import com.vbank.user_service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public ResponseEntity<?> registerUser(User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent() ||
                userRepository.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.status(409).body(Map.of(
                    "status", 409,
                    "error", "Conflict",
                    "message", "Username or email already exists."
            ));
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User savedUser = userRepository.save(user);

        return ResponseEntity.status(201).body(Map.of(
                "userId", savedUser.getUserId(),
                "username", savedUser.getUsername(),
                "message", "User registered successfully."
        ));
    }

    @Override
    public ResponseEntity<?> loginUser(String username, String rawPassword) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                    "status", 401,
                    "error", "Unauthorized",
                    "message", "Invalid username or password."
            ));
        }

        User user = userOptional.get();
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            return ResponseEntity.status(401).body(Map.of(
                    "status", 401,
                    "error", "Unauthorized",
                    "message", "Invalid username or password."
            ));
        }

        return ResponseEntity.ok(Map.of(
                "userId", user.getUserId(),
                "username", user.getUsername()
        ));
    }

    @Override
    public ResponseEntity<?> getUserProfile(UUID userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of(
                    "status", 404,
                    "error", "Not Found",
                    "message", "User with ID " + userId + " not found."
            ));
        }

        User user = userOptional.get();
        return ResponseEntity.ok(Map.of(
                "userId", user.getUserId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "firstName", user.getFirstName(),
                "lastName", user.getLastName()
        ));
    }
}
