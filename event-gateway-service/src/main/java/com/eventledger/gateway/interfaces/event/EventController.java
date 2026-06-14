package com.eventledger.gateway.interfaces.event;

import com.eventledger.gateway.interfaces.event.dto.EventRequest;
import com.eventledger.gateway.interfaces.event.dto.EventResponse;
import com.eventledger.gateway.interfaces.event.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Description: EventController.java is the public-facing REST controller for event lifecycle
 * operations in the Event Gateway Service. It exposes endpoints for submitting transaction
 * events, retrieving a single event by ID, and listing all events for a given account. It
 * delegates all business logic to EventService and maps service response statuses to the
 * appropriate HTTP status codes.
 */
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    /**
     * Accepts a transaction event submission. Returns 201 for a newly accepted event and
     * 200 if the eventId was already processed (idempotency).
     *
     * @param request the validated event payload from the request body
     * @return 201 with EventResponse on new event, 200 with original EventResponse on duplicate
     */
    @PostMapping
    public ResponseEntity<EventResponse> submitEvent(
            @Valid @RequestBody EventRequest request
    ) {
        EventResponse response = eventService.submitEvent(request);

        HttpStatus status = "DUPLICATE_IGNORED".equals(response.getStatus())
                ? HttpStatus.OK
                : HttpStatus.CREATED;

        return ResponseEntity.status(status).body(response);
    }

    /**
     * Retrieves a single event from the gateway's local database by its eventId.
     *
     * @param eventId the unique identifier of the event to retrieve
     * @return 200 with EventResponse if found, 404 if no event exists with the given ID
     */
    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponse> getEventById(
            @PathVariable String eventId
    ) {
        return ResponseEntity.ok(eventService.getEventById(eventId));
    }

    /**
     * Returns all events for the specified account ordered by eventTimestamp ascending.
     * Reads from the gateway's local database and is unaffected by Account Service availability.
     *
     * @param accountId the account ID to filter events by
     * @return 200 with a list of EventResponse objects, empty list if no events exist
     */
    @GetMapping
    public ResponseEntity<List<EventResponse>> getEventsByAccount(
            @RequestParam("account") String accountId
    ) {
        return ResponseEntity.ok(eventService.getEventsByAccountId(accountId));
    }
}