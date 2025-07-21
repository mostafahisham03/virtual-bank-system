package com.vbank.logging_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogMessageDto {
    private String message; // The escaped JSON request or response
    private String messageType; // "Request" | "Response"

    // Use ISO format for LocalDateTime, consistent with JSON timestamps
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private LocalDateTime dateTime;
}