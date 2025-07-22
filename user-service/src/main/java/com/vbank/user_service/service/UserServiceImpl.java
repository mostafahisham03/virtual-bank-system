package com.vbank.user_service.service;

import com.vbank.user_service.config.KafkaLogger;
import com.vbank.user_service.model.User;
import com.vbank.user_service.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final KafkaLogger kafkaLogger;

    @Override
    public ResponseEntity<?> registerUser(User user) {
        kafkaLogger.sendLog("Registering user: " + user.getUsername(), "Request");
        if (userRepository.findByUsername(user.getUsername()).isPresent() ||
                userRepository.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.status(409).body(Map.of(
                    "status", 409,
                    "error", "Conflict",
                    "message", "Username or email already exists."));
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User savedUser = userRepository.save(user);

        kafkaLogger.sendLog("User registered successfully: " + savedUser.getUsername(), "Response");
        return ResponseEntity.status(201).body(Map.of(
                "userId", savedUser.getUserId(),
                "username", savedUser.getUsername(),
                "message", "User registered successfully."));
    }

    @Override
    public ResponseEntity<?> loginUser(String username, String rawPassword) {
        kafkaLogger.sendLog("User login attempt: " + username, "Request");
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                    "status", 401,
                    "error", "Unauthorized",
                    "message", "Invalid username or password."));
        }

        User user = userOptional.get();
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            return ResponseEntity.status(401).body(Map.of(
                    "status", 401,
                    "error", "Unauthorized",
                    "message", "Invalid username or password."));
        }
        kafkaLogger.sendLog("User logged in successfully: " + user.getUsername(), "Response");
        return ResponseEntity.ok(Map.of(
                "userId", user.getUserId(),
                "username", user.getUsername()));
    }

    @Override
    public ResponseEntity<?> getUserProfile(UUID userId) {
        kafkaLogger.sendLog("Fetching user profile for user ID: " + userId, "Request");
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of(
                    "status", 404,
                    "error", "Not Found",
                    "message", "User with ID " + userId + " not found."));
        }

        User user = userOptional.get();
        kafkaLogger.sendLog("User profile fetched successfully for user ID: " + userId, "Response");
        return ResponseEntity.ok(Map.of(
                "userId", user.getUserId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "firstName", user.getFirstName(),
                "lastName", user.getLastName()));
    }
}
