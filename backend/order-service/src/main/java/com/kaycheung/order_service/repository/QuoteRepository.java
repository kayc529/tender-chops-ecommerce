package com.kaycheung.order_service.repository;

import com.kaycheung.order_service.entity.Quote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface QuoteRepository extends JpaRepository<Quote, UUID> {
    Optional<Quote> findByIdAndUserId(UUID quoteId, UUID userId);
}
