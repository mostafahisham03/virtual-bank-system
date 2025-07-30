package com.vbank.transaction_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KafkaLogger {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${logging.kafka.topic:vbank-logs}")
    private String topic;

    public void sendLog(String message, String type) {
        try {
            Map<String, Object> envelope = new HashMap<>();
            envelope.put("messageType", type);
            envelope.put("dateTime", Instant.now().toString());
            envelope.put("message", message);
            kafkaTemplate.send(topic, objectMapper.writeValueAsString(envelope));
        } catch (Exception ignored) {

        }
    }

    /**
     * New: accept any payload (Map/DTO). We serialize it into a JSON string
     * internally.
     */
    public void sendLog(Object payload, String type) {
        try {
            Map<String, Object> envelope = new HashMap<>();
            envelope.put("messageType", type);
            envelope.put("dateTime", Instant.now().toString());
            envelope.put("message", payload.toString());
            kafkaTemplate.send(topic, objectMapper.writeValueAsString(envelope));
        } catch (Exception ignored) {
            // intentionally swallow to avoid recursive logging failures
        }
    }
}
