
package com.vbank.user_service.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaLogger {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void sendLog(Object payload, String type) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send("user-service-logs", String.format("[%s] %s", type.toUpperCase(), jsonMessage));
        } catch (JsonProcessingException e) {
            kafkaTemplate.send("user-service-logs", String.format("[%s] Failed to serialize object: %s", type.toUpperCase(), e.getMessage()));
        }
    }
}
