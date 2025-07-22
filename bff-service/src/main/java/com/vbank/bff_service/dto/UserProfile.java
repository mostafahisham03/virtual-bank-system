
// UserProfile.java
package com.vbank.bff_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {
    private String userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
}