package com.ecommerce.notificationservice.kafka;

import com.ecommerce.notificationservice.dto.NotificationRequest;
import com.ecommerce.notificationservice.model.NotificationType;
import com.ecommerce.notificationservice.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.math.BigDecimal;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationEventConsumerTest {
    @Test
    void mapsConfirmationToExistingDtoContract() {
        NotificationService service = mock(NotificationService.class);
        new NotificationEventConsumer(service).onOrderConfirmed(Map.of(
                "orderId", 12, "userId", 7L, "transactionId", "txn-12",
                "totalAmount", new BigDecimal("25.50")), "order.confirmed");
        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(service).send(captor.capture());
        NotificationRequest request = captor.getValue();
        assertEquals(12L, request.getReferenceId());
        assertEquals("user-7@ecommerce.com", request.getRecipient());
        assertEquals(NotificationType.EMAIL, request.getType());
        assertEquals("Order #12 Confirmed!", request.getSubject());
        assertEquals("Your order has been confirmed. Transaction ID: txn-12. Total: ₹25.50", request.getBody());
    }

    @Test
    void mapsPaymentFailureToExistingDtoContract() {
        NotificationService service = mock(NotificationService.class);
        new NotificationEventConsumer(service).onOrderFailed(Map.of(
                "orderId", 12L, "userId", 7, "reason", "Declined"), "order.payment-failed");
        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(service).send(captor.capture());
        NotificationRequest request = captor.getValue();
        assertEquals(12L, request.getReferenceId());
        assertEquals("user-7@ecommerce.com", request.getRecipient());
        assertEquals(NotificationType.EMAIL, request.getType());
        assertEquals("Payment Failed for Order #12", request.getSubject());
        assertEquals("Unfortunately your payment failed. Reason: Declined", request.getBody());
    }
}