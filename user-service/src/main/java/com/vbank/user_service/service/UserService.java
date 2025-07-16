package com.vbank.user_service.service;

import com.virtualbank.userservice.model.User;
import com.virtualbank.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    public User register(String username, String email, String password, String firstName, String lastName) {
        User user = User.builder()
                .id(UUID.randomUUID())
                .username(username)
                .email(email)
                .passwordHash(encoder.encode(password))
                .firstName(firstName)
                .lastName(lastName)
                .createdAt(new Timestamp(System.currentTimeMillis()))
                .build();
        return userRepository.save(user);
    }

    public boolean checkPassword(String raw, String hash) {
        return encoder.matches(raw, hash);
    }
}
