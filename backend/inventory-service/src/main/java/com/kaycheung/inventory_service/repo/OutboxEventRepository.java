package com.kaycheung.inventory_service.repo;

import com.kaycheung.inventory_service.entity.OutboxEvent;
import com.kaycheung.inventory_service.messaging.contract.EventType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    boolean existsByEventTypeAndAggregateId(EventType eventType, UUID aggregateId);
}
