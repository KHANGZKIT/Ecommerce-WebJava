package com.ecommerce.paymentservice.repository;

import com.ecommerce.paymentservice.model.Payment;
import com.ecommerce.paymentservice.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByTransactionId(String transactionId);
    Optional<Payment> findByProviderOrderId(String providerOrderId);
    Optional<Payment> findByRequestId(String requestId);
    Optional<Payment> findByMomoTransId(Long momoTransId);
    List<Payment> findByOrderId(Long orderId);
    List<Payment> findByUserId(Long userId);
    List<Payment> findByOrderIdAndStatus(Long orderId, PaymentStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = com.ecommerce.paymentservice.model.PaymentStatus.SUCCESS AND p.momoTransId IS NOT NULL")
    java.math.BigDecimal sumVerifiedRevenue();

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(p) FROM Payment p WHERE p.status = com.ecommerce.paymentservice.model.PaymentStatus.SUCCESS AND p.momoTransId IS NOT NULL")
    long countVerifiedSuccessfulPayments();

    long countByStatus(PaymentStatus status);
}
