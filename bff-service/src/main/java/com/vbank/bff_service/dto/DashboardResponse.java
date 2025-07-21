package com.vbank.bff_service.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DashboardResponse {
    private String userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private List<Account> accounts;
}
