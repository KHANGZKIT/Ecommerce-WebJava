package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.model.CartClearStatus;
import com.ecommerce.orderservice.model.Order;
import com.ecommerce.orderservice.model.OrderIdempotency;
import com.ecommerce.orderservice.model.StockReleaseStatus;
import com.ecommerce.orderservice.model.OrderItem;
import com.ecommerce.orderservice.repository.OrderIdempotencyRepository;
import com.ecommerce.orderservice.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class OrderIdempotencyPersistenceService {

    private final OrderIdempotencyRepository repository;
    private final OrderRepository orderRepository;

    public OrderIdempotencyPersistenceService(OrderIdempotencyRepository repository) {
        this(repository, null);
    }

    @Autowired
    public OrderIdempotencyPersistenceService(OrderIdempotencyRepository repository,
                                              @Autowired(required = false) OrderRepository orderRepository) {
        this.repository = repository;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public Optional<OrderIdempotency> find(Long userId, String idempotencyKey) {
        return repository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderIdempotency create(Long userId, String idempotencyKey, String requestFingerprint) {
        OrderIdempotency record = OrderIdempotency.builder()
                .userId(userId)
                .idempotencyKey(idempotencyKey)
                .requestFingerprint(requestFingerprint)
                .stockReleaseStatus(StockReleaseStatus.NOT_REQUIRED)
                .cartClearStatus(CartClearStatus.NOT_REQUIRED)
                .build();
        return repository.saveAndFlush(record);
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderIdempotency attachOrder(Long recordId, Order order) {
        OrderIdempotency record = repository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("Idempotency record not found with id: " + recordId));
        record.attachOrder(order);
        return repository.saveAndFlush(record);
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderIdempotency updateRecoveryMetadata(
            Long recordId,
            String stockReservationReference,
            LocalDateTime paymentAttemptStartedAt,
            StockReleaseStatus stockReleaseStatus,
            CartClearStatus cartClearStatus) {
        OrderIdempotency record = repository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("Idempotency record not found with id: " + recordId));

        if (stockReservationReference != null) {
            record.setStockReservationReference(stockReservationReference);
        }
        if (paymentAttemptStartedAt != null) {
            record.setPaymentAttemptStartedAt(paymentAttemptStartedAt);
        }
        if (stockReleaseStatus != null) {
            record.setStockReleaseStatus(stockReleaseStatus);
        }
        if (cartClearStatus != null) {
            record.setCartClearStatus(cartClearStatus);
        }
        record.setUpdatedAt(LocalDateTime.now());
        return repository.saveAndFlush(record);
    }

    @Transactional(rollbackFor = Exception.class)
    public Order persistPendingOrderAndLink(
            Order order,
            List<OrderItem> items,
            Long recordId,
            String stockReservationReference) {
        if (orderRepository == null) {
            throw new IllegalStateException("OrderRepository is required for order persistence");
        }
        order = orderRepository.save(order);
        if (items != null) {
            for (OrderItem item : items) {
                item.setOrder(order);
            }
            order.setItems(items);
            order = orderRepository.save(order);
        }

        OrderIdempotency record = repository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("Idempotency record not found with id: " + recordId));
        record.attachOrder(order);
        record.setStockReservationReference(stockReservationReference);
        record.setStockReleaseStatus(StockReleaseStatus.NOT_REQUIRED);
        record.setCartClearStatus(CartClearStatus.NOT_REQUIRED);
        repository.saveAndFlush(record);

        return order;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean claimPaymentAttempt(Long recordId) {
        OrderIdempotency record = repository.findByIdForUpdate(recordId)
                .orElseThrow(() -> new IllegalArgumentException("Idempotency record not found with id: " + recordId));

        if (record.getPaymentAttemptStartedAt() != null) {
            log.info("Payment attempt claim denied for idempotency record {}: already claimed at {}",
                    recordId, record.getPaymentAttemptStartedAt());
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        record.setPaymentAttemptStartedAt(now);
        record.setUpdatedAt(now);
        repository.saveAndFlush(record);
        log.info("Payment attempt claim granted for idempotency record {}", recordId);
        return true;
    }
}
