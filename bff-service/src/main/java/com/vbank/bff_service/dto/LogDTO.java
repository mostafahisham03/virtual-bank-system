package com.vbank.bff_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogDTO {
    private String message;
    private String messageType; // e.g., "Request", "Response", "Error"
    private String dateTime;
}
