package com.vbank.transaction_service.model; // Corrected package name

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID transactionId;

    private UUID fromAccountId;
    private UUID toAccountId;
    private BigDecimal amount;
    private String description;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    private Instant timestamp;

    @PrePersist
    protected void onCreate() {
        if (this.transactionId == null) {
            this.transactionId = UUID.randomUUID();
        }
        if (this.timestamp == null) {
            this.timestamp = Instant.now();
        }
    }
}