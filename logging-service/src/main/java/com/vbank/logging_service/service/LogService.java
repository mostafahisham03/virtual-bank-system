package com.vbank.logging_service.service;

import com.vbank.logging_service.dto.LogEntryResponseDto;
import com.vbank.logging_service.dto.LogMessageDto;

import java.util.List;

public interface LogService {
    LogEntryResponseDto saveLogEntry(LogMessageDto logMessageDto);
    List<LogEntryResponseDto> getAllLogEntries();
    // Add methods for searching logs, e.g., by messageType, by date range
    List<LogEntryResponseDto> getLogEntriesByMessageType(String messageType);
}