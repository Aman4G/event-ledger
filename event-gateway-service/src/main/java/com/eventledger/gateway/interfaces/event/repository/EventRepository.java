package com.eventledger.gateway.interfaces.event.repository;


import com.eventledger.gateway.interfaces.event.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, String> {

    List<Event> findByAccountIdOrderByEventTimestampAsc(String accountId);
}