package com.eventledger.gateway.interfaces.event.service;

import com.eventledger.gateway.interfaces.event.dto.EventRequest;
import com.eventledger.gateway.interfaces.event.dto.EventResponse;

import java.util.List;

/**
 * Description: EventService.java defines the contract for all event lifecycle operations in the
 * Event Gateway Service. It handles event submission with idempotency enforcement, individual
 * event retrieval, and account-scoped event listing. Implementations are responsible for
 * persisting events, delegating transactions to the Account Service, and returning consistent
 * responses ordered by event timestamp.
 */
public interface EventService {

    /**
     * Submits a new transaction event to the gateway. If the eventId already exists, returns
     * the original event without re-processing or calling the Account Service.
     *
     * @param request the event payload containing eventId, accountId, type, amount, currency,
     *                eventTimestamp, and optional metadata
     * @return EventResponse with status ACCEPTED for new events or DUPLICATE_IGNORED for duplicates
     */
    EventResponse submitEvent(EventRequest request);

    /**
     * Retrieves a single event from the gateway's local database by its unique eventId.
     *
     * @param eventId the unique identifier of the event to retrieve
     * @return EventResponse with status FOUND containing the stored event data
     * @throws com.eventledger.gateway.common.exception.ResourceNotFoundException if no event
     *         exists with the given eventId
     */
    EventResponse getEventById(String eventId);

    /**
     * Retrieves all events associated with the given accountId from the gateway's local database,
     * ordered by eventTimestamp ascending. This method works independently of the Account Service.
     *
     * @param accountId the account whose events should be listed
     * @return list of EventResponse objects ordered by eventTimestamp ascending, empty if none found
     */
    List<EventResponse> getEventsByAccountId(String accountId);
}