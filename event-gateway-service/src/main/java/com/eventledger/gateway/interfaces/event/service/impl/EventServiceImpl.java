package com.eventledger.gateway.interfaces.event.service.impl;

import com.eventledger.gateway.client.account.AccountServiceClient;
import com.eventledger.gateway.client.dto.AccountTransactionRequest;
import com.eventledger.gateway.common.exception.ResourceNotFoundException;
import com.eventledger.gateway.interfaces.event.dto.EventRequest;
import com.eventledger.gateway.interfaces.event.dto.EventResponse;
import com.eventledger.gateway.interfaces.event.entity.Event;
import com.eventledger.gateway.interfaces.event.repository.EventRepository;
import com.eventledger.gateway.interfaces.event.service.EventService;
import com.eventledger.gateway.observability.EventMetricsService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final AccountServiceClient accountServiceClient;
    private final ObjectMapper objectMapper;
    private final EventMetricsService eventMetricsService;

    @Override
    @Transactional
    public EventResponse submitEvent(EventRequest request) {
        log.info("Submitting event eventId={} accountId={}", request.getEventId(), request.getAccountId());

        return eventRepository.findById(request.getEventId())
                .map(existingEvent -> {
                    log.info("Duplicate event ignored eventId={} accountId={}",
                            existingEvent.getEventId(), existingEvent.getAccountId());

                    eventMetricsService.incrementDuplicateIgnored();

                    return buildResponse(existingEvent, "DUPLICATE_IGNORED");
                })
                .orElseGet(() -> createAndApplyEvent(request));
    }

    @Override
    @Transactional(readOnly = true)
    public EventResponse getEventById(String eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

        return buildResponse(event, "FOUND");
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> getEventsByAccountId(String accountId) {
        return eventRepository.findByAccountIdOrderByEventTimestampAsc(accountId)
                .stream()
                .map(event -> buildResponse(event, "FOUND"))
                .toList();
    }

    private EventResponse createAndApplyEvent(EventRequest request) {
        Event event = Event.builder()
                .eventId(request.getEventId())
                .accountId(request.getAccountId())
                .type(request.getType())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .eventTimestamp(request.getEventTimestamp())
                .metadata(toJson(request.getMetadata()))
                .createdAt(Instant.now())
                .build();

        Event savedEvent = eventRepository.save(event);

        accountServiceClient.applyTransaction(
                request.getAccountId(),
                AccountTransactionRequest.builder()
                        .eventId(request.getEventId())
                        .accountId(request.getAccountId())
                        .type(request.getType())
                        .amount(request.getAmount())
                        .currency(request.getCurrency())
                        .eventTimestamp(request.getEventTimestamp())
                        .build()
        );

        eventMetricsService.incrementEventAccepted();

        log.info("Event submitted and applied successfully eventId={} accountId={}",
                savedEvent.getEventId(), savedEvent.getAccountId());

        return buildResponse(savedEvent, "ACCEPTED");
    }

    private String toJson(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid metadata format");
        }
    }

    private Map<String, Object> fromJson(String metadata) {
        if (metadata == null || metadata.isBlank()) {
            return Collections.emptyMap();
        }

        try {
            return objectMapper.readValue(metadata, Map.class);
        } catch (JsonProcessingException exception) {
            return Collections.emptyMap();
        }
    }

    private EventResponse buildResponse(Event event, String status) {
        return EventResponse.builder()
                .eventId(event.getEventId())
                .accountId(event.getAccountId())
                .type(event.getType())
                .amount(event.getAmount())
                .currency(event.getCurrency())
                .eventTimestamp(event.getEventTimestamp())
                .metadata(fromJson(event.getMetadata()))
                .status(status)
                .build();
    }
}