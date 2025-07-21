package com.vbank.logging_service.repository;

import com.vbank.logging_service.model.LogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.LocalDateTime;

@Repository
public interface LogRepository extends JpaRepository<LogEntry, Long> {
    List<LogEntry> findByMessageType(String messageType);

    List<LogEntry> findByDateTimeBetween(LocalDateTime start, LocalDateTime end);
    // You can add more query methods as needed for auditing/debugging
}