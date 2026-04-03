package com.kaycheung.payment_service.repository;

import com.kaycheung.payment_service.entity.Payment;
import com.kaycheung.payment_service.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByOrderId(UUID orderId);

    @Query(value = """
        insert into payment (
            id, order_id, user_id, payment_status, amount, currency, provider, current_attempt_id, created_at, updated_at
        )
        values (
            :id, :orderId, :userId, :status, :amount, :currency, :provider, null, now(), now()
        )
        on conflict (order_id) do nothing
        returning id
        """, nativeQuery = true)
    UUID tryInsertPaymentReturningId(
            @Param("id") UUID id,
            @Param("orderId") UUID orderId,
            @Param("userId") UUID userId,
            @Param("status") String status,
            @Param("amount") long amount,
            @Param("currency") String currency,
            @Param("provider") String provider
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
             update Payment p
             set p.paymentStatus = :to
             where p.id = :id and p.paymentStatus= :from
            """
    )
    int transitionStatus(@Param("id") UUID id,
                         @Param("from") PaymentStatus from,
                         @Param("to") PaymentStatus to);


    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
             update Payment p
             set p.paymentStatus = :toStatus
             where p.id = :id and p.paymentStatus!= :unlessStatus
            """
    )
    int setStatusUnless(@Param("id") UUID id,
                        @Param("unlessStatus") PaymentStatus unlessStatus,
                        @Param("toStatus") PaymentStatus toStatus);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
             update Payment p
             set p.currentAttemptId = :newAttemptId
             where p.id = :id
            """
    )
    int updateCurrentAttemptId(
            @Param("id") UUID id,
            @Param("newAttemptId") UUID newAttemptId
    );
}
