package com.ecommerce.orderservice.client;

import com.ecommerce.orderservice.dto.NotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @deprecated Notifications are now sent asynchronously via Kafka.
 *             The {@code KafkaTemplate} in {@code OrderService} publishes
 *             {@code OrderConfirmedEvent} / {@code OrderFailedEvent} to the
 *             {@code order.confirmed} and {@code order.payment-failed} topics,
 *             which are consumed by notification-service.
 *
 *             This class is kept as a compile-safe no-op stub so that any
 *             test code referencing it still compiles.  It will be removed in
 *             a future clean-up PR.
 */
@Slf4j
@Deprecated(since = "2.0", forRemoval = true)
@Component
public class NotificationClient {

    /**
     * No-op — always logs a warning and returns immediately.
     * Use Kafka events instead.
     */
    public void sendNotification(NotificationRequest request) {
        log.warn("[DEPRECATED] NotificationClient.sendNotification() called — " +
                 "this is a no-op; notifications are sent via Kafka. ref={}",
                 request.getReferenceId());
    }
}
