package com.ecommerce.paymentservice.controller;

import com.ecommerce.paymentservice.dto.PaymentRequest;
import com.ecommerce.paymentservice.dto.PaymentResponse;
import com.ecommerce.paymentservice.dto.RefundRequest;
import com.ecommerce.paymentservice.exception.ApiException;
import com.ecommerce.paymentservice.security.PaymentPrincipal;
import com.ecommerce.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    private PaymentPrincipal getAuthenticatedPrincipal() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof PaymentPrincipal principal) {
            return principal;
        }
        throw new ApiException("Authentication required", HttpStatus.UNAUTHORIZED);
    }

    @PostMapping("/charge")
    public ResponseEntity<PaymentResponse> processPayment(
            @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.processPayment(request));
    }

    @PostMapping("/refund")
    public ResponseEntity<PaymentResponse> processRefund(
            @Valid @RequestBody RefundRequest request) {
        PaymentPrincipal principal = getAuthenticatedPrincipal();
        if (!principal.isAdmin()) {
            throw new ApiException("Access denied to refund", HttpStatus.FORBIDDEN);
        }
        return ResponseEntity.ok(paymentService.processRefund(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable Long id) {
        PaymentPrincipal principal = getAuthenticatedPrincipal();
        PaymentResponse payment = paymentService.getPaymentById(id);
        if (!principal.isAdmin() && (payment.getUserId() == null || !payment.getUserId().equals(principal.userId()))) {
            throw new ApiException("Access denied to payment", HttpStatus.FORBIDDEN);
        }
        return ResponseEntity.ok(payment);
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<PaymentResponse>> getByOrderId(@PathVariable Long orderId) {
        PaymentPrincipal principal = getAuthenticatedPrincipal();
        List<PaymentResponse> payments = paymentService.getPaymentsByOrderId(orderId);
        if (!principal.isAdmin()) {
            boolean hasOtherUserPayment = payments.stream()
                    .anyMatch(p -> p.getUserId() != null && !p.getUserId().equals(principal.userId()));
            if (hasOtherUserPayment) {
                throw new ApiException("Access denied to order payments", HttpStatus.FORBIDDEN);
            }
        }
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PaymentResponse>> getByUserId(@PathVariable Long userId) {
        PaymentPrincipal principal = getAuthenticatedPrincipal();
        if (!principal.isAdmin() && !userId.equals(principal.userId())) {
            throw new ApiException("Access denied to user payments", HttpStatus.FORBIDDEN);
        }
        return ResponseEntity.ok(paymentService.getPaymentsByUserId(userId));
    }
}
