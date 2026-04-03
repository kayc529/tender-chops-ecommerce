package com.kaycheung.payment_service.repository;

import com.kaycheung.payment_service.entity.PaymentAttempt;
import com.kaycheung.payment_service.entity.PaymentAttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, UUID> {
    Optional<PaymentAttempt> findByCheckoutSessionId(String checkoutSessionId);

    Optional<PaymentAttempt> findByPaymentIntentId(String paymentIntentId);

    Optional<PaymentAttempt> findByPaymentIdAndAttemptNo(UUID paymentId, int attemptNo);

    @Query(value = """
        insert into payment_attempts (
            id, payment_id, attempt_no, payment_attempt_status, idempotency_key,
            checkout_session_id, payment_intent_id, redirect_url, created_at, updated_at
        )
        values (
            :id, :paymentId, :attemptNo, :status, :idempotencyKey,
            null, null, null, now(), now()
        )
        on conflict (payment_id, attempt_no) do nothing
        returning id
        """, nativeQuery = true)
    UUID tryInsertAttemptReturningId(
            @Param("id") UUID id,
            @Param("paymentId") UUID paymentId,
            @Param("attemptNo") int attemptNo,
            @Param("status") String status,
            @Param("idempotencyKey") String idempotencyKey
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update PaymentAttempt pa
            set pa.paymentAttemptStatus = :to
            where pa.id = :id and pa.paymentAttemptStatus = :from
            """)
    int transitionStatus(@Param("id") UUID id,
                         @Param("from") PaymentAttemptStatus from,
                         @Param("to") PaymentAttemptStatus to);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update PaymentAttempt pa
            set pa.paymentAttemptStatus = :toStatus
            where pa.id = :id and pa.paymentAttemptStatus != :unlessStatus
            """)
    int setStatusUnless(@Param("id") UUID id,
                        @Param("unlessStatus") PaymentAttemptStatus unlessStatus,
                        @Param("toStatus") PaymentAttemptStatus toStatus);

    @Query("""
            select pa from PaymentAttempt pa
            where pa.paymentId = :paymentId and pa.paymentAttemptStatus not in :terminalStatuses and pa.id != :exceptAttemptId
            """)
    List<PaymentAttempt> getAllNonTerminalPaymentAttemptsExcept(
            @Param("paymentId") UUID paymentId,
            @Param("exceptAttemptId") UUID exceptAttemptId,
            @Param("terminalStatuses") List<PaymentAttemptStatus> terminalStatuses
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update PaymentAttempt pa
            set pa.paymentAttemptStatus = :failedStatus
            where pa.id = :id and pa.paymentAttemptStatus not in :terminalStatuses
            """)
    int failNonTerminalPaymentAttempt(
            @Param("id") UUID paymentAttemptId,
            @Param("failedStatus") PaymentAttemptStatus failedStatus,
            @Param("terminalStatuses") List<PaymentAttemptStatus> terminalStatuses
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update PaymentAttempt pa
            set pa.paymentAttemptStatus = :canceledStatus
            where pa.id = :id and pa.paymentAttemptStatus not in :terminalStatuses
            """)
    int cancelNonTerminalPaymentAttempt(
            @Param("id") UUID paymentAttemptId,
            @Param("canceledStatus") PaymentAttemptStatus canceledStatus,
            @Param("terminalStatuses") List<PaymentAttemptStatus> terminalStatuses
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update PaymentAttempt pa
            set pa.paymentIntentId = :paymentIntentId
            where pa.id = :id and pa.paymentIntentId is null
            """)
    int updatePaymentAttemptPaymentIntentIdIfNull(
            @Param("id") UUID paymentAttemptId,
            @Param("paymentIntentId") String paymentIntentId);
}
