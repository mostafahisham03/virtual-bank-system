package com.vbank.account_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vbank.account_service.dto.LogDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class KafkaLogger {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String topic = "vbank-logs"; // same logging topic

    public void sendLog(String message, String type) {
        try {
            LogDTO log = new LogDTO(message, type, LocalDateTime.now().toString());
            String json = objectMapper.writeValueAsString(log);
            kafkaTemplate.send(topic, json);
        } catch (Exception e) {
            e.printStackTrace(); // avoid recursive logging here
        }
    }
}
