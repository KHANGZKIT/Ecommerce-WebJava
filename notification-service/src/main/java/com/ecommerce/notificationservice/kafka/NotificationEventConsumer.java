package com.ecommerce.notificationservice.kafka;

import com.ecommerce.notificationservice.dto.NotificationRequest;
import com.ecommerce.notificationservice.model.NotificationType;
import com.ecommerce.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Consumes order lifecycle events from Kafka topics and dispatches
 * the appropriate notification via {@link NotificationService}.
 *
 * Topics:
 *  - {@code order.confirmed}      → order confirmation email
 *  - {@code order.payment-failed} → payment failure email
 *
 * Consumer group {@code notification-service-group} ensures each message
 * is processed exactly once even when multiple instances are running.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
        topics = "order.confirmed",
        groupId = "notification-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onOrderConfirmed(@Payload Map<String, Object> event,
                                 @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {
            Long orderId  = toLong(event.get("orderId"));
            Long userId   = toLong(event.get("userId"));
            String txnId  = String.valueOf(event.get("transactionId"));
            String total  = String.valueOf(event.get("totalAmount"));

            log.info("[Kafka] order.confirmed received — orderId={}, userId={}", orderId, userId);

            NotificationRequest request = new NotificationRequest();
            request.setRecipient("user-" + userId + "@ecommerce.com");
            request.setSubject("Order #" + orderId + " Confirmed!");
            request.setBody("Your order has been confirmed. Transaction ID: " + txnId
                    + ". Total: ₹" + total);
            request.setType(NotificationType.EMAIL);
            request.setReferenceId(orderId);
            notificationService.send(request);
        } catch (Exception ex) {
            log.error("[Kafka] Failed to process order.confirmed event: {}", ex.getMessage(), ex);
            // In production: publish to a dead-letter topic or raise an alert
        }
    }

    @KafkaListener(
        topics = "order.payment-failed",
        groupId = "notification-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onOrderFailed(@Payload Map<String, Object> event,
                              @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {
            Long orderId  = toLong(event.get("orderId"));
            Long userId   = toLong(event.get("userId"));
            String reason = String.valueOf(event.get("reason"));

            log.info("[Kafka] order.payment-failed received — orderId={}, userId={}", orderId, userId);

            NotificationRequest request = new NotificationRequest();
            request.setRecipient("user-" + userId + "@ecommerce.com");
            request.setSubject("Payment Failed for Order #" + orderId);
            request.setBody("Unfortunately your payment failed. Reason: " + reason);
            request.setType(NotificationType.EMAIL);
            request.setReferenceId(orderId);
            notificationService.send(request);
        } catch (Exception ex) {
            log.error("[Kafka] Failed to process order.payment-failed event: {}", ex.getMessage(), ex);
        }
    }

    /** Safe conversion: Kafka deserialises numbers as Integer or Long. */
    private Long toLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        return Long.parseLong(String.valueOf(value));
    }
}
