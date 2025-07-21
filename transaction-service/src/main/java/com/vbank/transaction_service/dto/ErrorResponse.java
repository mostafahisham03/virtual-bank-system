package com.vbank.transaction_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor // This generates the constructor (int status, String error, String message)
public class ErrorResponse {
    private int status; // Change this to int
    private String error; // This should be String (for reason phrase)
    private String message; // This should be String (for custom message)
}