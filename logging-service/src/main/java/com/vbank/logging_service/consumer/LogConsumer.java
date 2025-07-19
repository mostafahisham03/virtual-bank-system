package com.vbank.logging_service.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vbank.logging_service.dto.LogMessageDto;
import com.vbank.logging_service.service.LogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LogConsumer {

    private final LogService logService;
    private final ObjectMapper objectMapper; // Use Spring's injected ObjectMapper

    // @RequiredArgsConstructor from Lombok handles constructor injection for final fields
    // We add a public constructor to ensure ObjectMapper is configured correctly if not already.
    public LogConsumer(LogService logService, ObjectMapper objectMapper) {
        this.logService = logService;
        this.objectMapper = objectMapper;
        // Ensure ObjectMapper can handle Java 8 Date/Time types if not already configured globally
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }


    @KafkaListener(topics = "${kafka.topic.logs}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeLogMessage(String logMessageJson) {
        log.info("Received raw log message from Kafka: {}", logMessageJson);
        try {
            // Deserialize the JSON string into our LogMessageDto
            LogMessageDto logMessageDto = objectMapper.readValue(logMessageJson, LogMessageDto.class);
            log.info("Parsed log message from Kafka: Type={}, Timestamp={}",
                    logMessageDto.getMessageType(), logMessageDto.getDateTime());

            logService.saveLogEntry(logMessageDto);
            log.info("Successfully processed and saved log entry from Kafka.");
        } catch (JsonProcessingException e) {
            log.error("Error parsing JSON log message from Kafka: {}. Message content: {}", e.getMessage(), logMessageJson);
            // Consider sending to a dead-letter topic for reprocessing or manual inspection
        } catch (Exception e) {
            log.error("An unexpected error occurred while processing/saving log entry: {}", e.getMessage(), e);
        }
    }
}