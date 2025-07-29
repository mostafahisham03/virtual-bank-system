package com.vbank.user_service.service;

import com.vbank.user_service.config.KafkaLogger;
import com.vbank.user_service.dto.UserLoginResponse;
import com.vbank.user_service.dto.UserProfileResponse;
import com.vbank.user_service.dto.UserRegisterRequest;
import com.vbank.user_service.dto.UserRegisterResponse;
import com.vbank.user_service.exception.BadRequestException;
import com.vbank.user_service.exception.ResourceNotFoundException;
import com.vbank.user_service.model.User;
import com.vbank.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
        public UserRegisterResponse registerUser(UserRegisterRequest request) {
                kafkaLogger.sendLog("Registering user: " + request.getUsername(), "Request");

                if (userRepository.findByUsername(request.getUsername()).isPresent() ||
                        userRepository.findByEmail(request.getEmail()).isPresent()) {
                        kafkaLogger.sendLog("Registration failed: Username or email already exists.", "Response");
                        throw new BadRequestException("Username or email already exists.");
                }

                User user = new User();
                user.setUsername(request.getUsername());
                user.setEmail(request.getEmail());
                user.setFirstName(request.getFirstName());
                user.setLastName(request.getLastName());
                user.setPassword(passwordEncoder.encode(request.getPassword()));

                User savedUser = userRepository.save(user);

                kafkaLogger.sendLog("User registered successfully: " + savedUser.getUsername(), "Response");

                return new UserRegisterResponse(
                        savedUser.getUserId(),
                        savedUser.getUsername(),
                        "User registered successfully."
                );
        }

        @Override
        public UserLoginResponse loginUser(String username, String rawPassword) {
                kafkaLogger.sendLog("User login attempt: " + username, "Request");

                Optional<User> userOptional = userRepository.findByUsername(username);
                if (userOptional.isEmpty()) {
                        kafkaLogger.sendLog("Login failed for user: " + username, "Response");
                        throw new BadRequestException("Invalid username or password.");
                }

                User user = userOptional.get();
                if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
                        kafkaLogger.sendLog("Login failed for user: " + username, "Response");
                        throw new BadRequestException("Invalid username or password.");
                }

                kafkaLogger.sendLog("User logged in successfully: " + user.getUsername(), "Response");

                return new UserLoginResponse(user.getUserId(), user.getUsername());
        }

        @Override
        public UserProfileResponse getUserProfile(UUID userId) {
                kafkaLogger.sendLog("Fetching user profile for user ID: " + userId, "Request");

                Optional<User> userOptional = userRepository.findById(userId);
                if (userOptional.isEmpty()) {
                        kafkaLogger.sendLog("User profile not found for user ID: " + userId, "Response");
                        throw new ResourceNotFoundException("User with ID " + userId + " not found.");
                }

                User user = userOptional.get();

                kafkaLogger.sendLog("User profile fetched successfully for user ID: " + userId, "Response");

                return new UserProfileResponse(
                        user.getUserId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getFirstName(),
                        user.getLastName()
                );
        }
}
