package com.vbank.bff_service.dto;

import lombok.Data;

@Data
public class UserProfile {
    private String userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
}
