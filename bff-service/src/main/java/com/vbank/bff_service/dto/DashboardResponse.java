package com.vbank.bff_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    private String userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private List<Account> accounts;
}
