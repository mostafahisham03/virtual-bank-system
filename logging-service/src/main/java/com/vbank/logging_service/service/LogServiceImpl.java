package com.vbank.logging_service.service;

import com.vbank.logging_service.dto.LogEntryResponseDto;
import com.vbank.logging_service.dto.LogMessageDto;
import com.vbank.logging_service.model.LogEntry;
import com.vbank.logging_service.repository.LogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogServiceImpl implements LogService {

    private final LogRepository logRepository;

    @Override
    public LogEntryResponseDto saveLogEntry(LogMessageDto logMessageDto) {
        log.info("Attempting to save log entry of type: {}", logMessageDto.getMessageType());

        // Map DTO to Entity
        LogEntry logEntry = LogEntry.builder()
                .message(logMessageDto.getMessage())
                .messageType(logMessageDto.getMessageType())
                .dateTime(logMessageDto.getDateTime())
                .build();

        LogEntry savedLogEntry = logRepository.save(logEntry);
        log.info("Log entry saved with ID: {}", savedLogEntry.getId());
        return mapToDto(savedLogEntry);
    }

    @Override
    public List<LogEntryResponseDto> getAllLogEntries() {
        log.info("Fetching all log entries from dump table.");
        return logRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<LogEntryResponseDto> getLogEntriesByMessageType(String messageType) {
        log.info("Fetching log entries by message type: {}", messageType);
        return logRepository.findByMessageType(messageType).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    // Helper method to map LogEntry model to LogEntryResponseDto
    private LogEntryResponseDto mapToDto(LogEntry logEntry) {
        return LogEntryResponseDto.builder()
                .id(logEntry.getId())
                .message(logEntry.getMessage())
                .messageType(logEntry.getMessageType())
                .dateTime(logEntry.getDateTime())
                .build();
    }
}