package com.kaycheung.payment_service.repository;

import com.kaycheung.payment_service.entity.ProcessedProviderEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedProviderEventRepository extends JpaRepository<ProcessedProviderEvent, UUID> {
}
