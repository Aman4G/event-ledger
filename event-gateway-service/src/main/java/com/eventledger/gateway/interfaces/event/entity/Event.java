package com.eventledger.gateway.interfaces.event.entity;

import com.eventledger.gateway.common.enums.EventType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    @Id
    private String eventId;

    private String accountId;

    @Enumerated(EnumType.STRING)
    private EventType type;

    private BigDecimal amount;

    private String currency;

    private Instant eventTimestamp;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    private Instant createdAt;
}
