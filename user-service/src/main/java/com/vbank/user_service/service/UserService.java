package com.vbank.user_service.service;

import com.vbank.user_service.model.User;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public interface UserService {

    ResponseEntity<?> registerUser(User user);
    ResponseEntity<?> loginUser(String username, String password);
    ResponseEntity<?> getUserProfile(UUID userId);
}
