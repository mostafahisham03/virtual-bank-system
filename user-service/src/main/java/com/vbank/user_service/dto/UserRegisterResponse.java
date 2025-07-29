package com.vbank.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class UserRegisterResponse {
    private UUID userId;
    private String username;
    private String message;
}
