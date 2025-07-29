package com.vbank.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class UserProfileResponse {
    private UUID userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
}
