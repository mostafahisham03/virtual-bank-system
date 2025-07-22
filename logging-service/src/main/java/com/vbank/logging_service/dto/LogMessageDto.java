package com.vbank.logging_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogMessageDto {

    @JsonProperty("message")
    private String message;

    @JsonProperty("messageType")
    private String messageType;

    private LocalDateTime dateTime;

    @JsonSetter("dateTime")
    public void setDateTime(String dateTimeStr) {
        try {
            // Remove any timezone information and truncate excessive nanoseconds
            String cleaned = cleanDateTimeString(dateTimeStr);
            this.dateTime = LocalDateTime.parse(cleaned);
        } catch (Exception e) {
            throw new RuntimeException("Unable to parse datetime: " + dateTimeStr, e);
        }
    }

    private String cleanDateTimeString(String dateTimeStr) {
        // Remove timezone indicators (Z, +XX:XX, -XX:XX)
        String cleaned = dateTimeStr.replaceAll("[+\\-]\\d{2}:?\\d{2}$|Z$", "");

        // Handle excessive nanosecond precision
        if (cleaned.contains(".")) {
            String[] parts = cleaned.split("\\.");
            if (parts.length == 2 && parts[1].length() > 9) {
                // Truncate to maximum 9 digits (nanosecond precision)
                String nanos = parts[1].length() > 6 ? parts[1].substring(0, 6) : parts[1];
                cleaned = parts[0] + "." + nanos;
            }
        }

        return cleaned;
    }
}