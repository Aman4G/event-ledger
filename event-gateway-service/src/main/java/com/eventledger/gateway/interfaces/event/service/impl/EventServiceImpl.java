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

/**
 * Description: EventServiceImpl.java is the core service implementation for event lifecycle
 * management in the Event Gateway Service. It handles event submission with idempotency
 * enforcement using eventId as the primary key, delegates transaction application to the
 * Account Service client, manages metadata serialization and deserialization, emits observability
 * metrics, and provides event retrieval operations ordered by event timestamp.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final AccountServiceClient accountServiceClient;
    private final ObjectMapper objectMapper;
    private final EventMetricsService eventMetricsService;

    /**
     * Submits a transaction event to the gateway. Checks for an existing event with the same
     * eventId first — if found, returns it as a duplicate without calling the Account Service.
     * Otherwise, persists the event and forwards the transaction to the Account Service.
     *
     * @param request the event payload to submit
     * @return EventResponse with status ACCEPTED for new events or DUPLICATE_IGNORED for duplicates
     */
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

    /**
     * Retrieves a single event from the gateway's local database by its eventId.
     *
     * @param eventId the unique identifier of the event to retrieve
     * @return EventResponse with status FOUND
     * @throws com.eventledger.gateway.common.exception.ResourceNotFoundException if the event
     *         does not exist
     */
    @Override
    @Transactional(readOnly = true)
    public EventResponse getEventById(String eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

        return buildResponse(event, "FOUND");
    }

    /**
     * Returns all events for the given accountId ordered by eventTimestamp ascending.
     * Reads exclusively from the gateway's local database, independent of the Account Service.
     *
     * @param accountId the account whose events should be listed
     * @return list of EventResponse objects ordered by eventTimestamp ascending
     */
    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> getEventsByAccountId(String accountId) {
        return eventRepository.findByAccountIdOrderByEventTimestampAsc(accountId)
                .stream()
                .map(event -> buildResponse(event, "FOUND"))
                .toList();
    }

    /**
     * Persists a new event to the gateway database and calls the Account Service to apply
     * the corresponding transaction. Increments the accepted events metric on success.
     *
     * @param request the validated event request to persist and forward
     * @return EventResponse with status ACCEPTED
     */
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

    /**
     * Serializes the metadata map to a JSON string for storage. Returns null if metadata
     * is null or empty.
     *
     * @param metadata the key-value metadata map to serialize
     * @return JSON string representation of the metadata, or null if empty
     * @throws IllegalArgumentException if the metadata cannot be serialized
     */
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

    /**
     * Deserializes a JSON string back into a metadata map. Returns an empty map if the
     * input is null, blank, or cannot be parsed.
     *
     * @param metadata the JSON string to deserialize
     * @return the deserialized metadata map, or an empty map if input is invalid
     */
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

    /**
     * Builds an EventResponse from a stored Event entity and a status string.
     *
     * @param event  the Event entity to convert
     * @param status the status label to attach to the response (e.g. ACCEPTED, FOUND, DUPLICATE_IGNORED)
     * @return the populated EventResponse
     */
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