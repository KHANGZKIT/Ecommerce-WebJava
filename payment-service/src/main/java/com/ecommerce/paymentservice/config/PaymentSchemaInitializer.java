package com.ecommerce.paymentservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ensures payments table check constraints and column types are up to date and reproducible
 * across all database environments (development, testing, docker containers).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void ensureSchemaCompatibility() {
        try {
            log.info("Ensuring payments table check constraints and column types are up to date...");

            // 1. Update payment_method check constraint to include MOMO
            jdbcTemplate.execute("ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_payment_method_check");
            jdbcTemplate.execute("ALTER TABLE payments ADD CONSTRAINT payments_payment_method_check " +
                    "CHECK (payment_method::text = ANY (ARRAY['CREDIT_CARD', 'DEBIT_CARD', 'UPI', 'NET_BANKING', 'WALLET', 'MOMO']))");

            // 2. Update status check constraint to include CANCELLED
            jdbcTemplate.execute("ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_status_check");
            jdbcTemplate.execute("ALTER TABLE payments ADD CONSTRAINT payments_status_check " +
                    "CHECK (status::text = ANY (ARRAY['PENDING', 'SUCCESS', 'FAILED', 'CANCELLED', 'REFUNDED']))");

            // 3. Ensure large string columns use TEXT
            jdbcTemplate.execute("ALTER TABLE payments ALTER COLUMN message TYPE text");
            jdbcTemplate.execute("ALTER TABLE payments ALTER COLUMN pay_url TYPE text");
            jdbcTemplate.execute("ALTER TABLE payments ALTER COLUMN qr_code_url TYPE text");
            jdbcTemplate.execute("ALTER TABLE payments ALTER COLUMN deeplink TYPE text");

            log.info("Payments schema check constraints and column types initialized successfully.");
        } catch (Exception ex) {
            log.warn("Non-fatal notice during payments schema compatibility initialization: {}", ex.getMessage());
        }
    }
}
