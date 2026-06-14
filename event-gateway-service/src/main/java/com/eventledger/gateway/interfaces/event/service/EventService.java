package com.eventledger.gateway.interfaces.event.service;

import com.eventledger.gateway.interfaces.event.dto.EventRequest;
import com.eventledger.gateway.interfaces.event.dto.EventResponse;

import java.util.List;

public interface EventService {

    EventResponse submitEvent(EventRequest request);

    EventResponse getEventById(String eventId);

    List<EventResponse> getEventsByAccountId(String accountId);
}