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

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

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

    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponse> getEventById(
            @PathVariable String eventId
    ) {
        return ResponseEntity.ok(eventService.getEventById(eventId));
    }

    @GetMapping
    public ResponseEntity<List<EventResponse>> getEventsByAccount(
            @RequestParam("account") String accountId
    ) {
        return ResponseEntity.ok(eventService.getEventsByAccountId(accountId));
    }
}