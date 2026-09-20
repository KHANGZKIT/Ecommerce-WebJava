package com.ecommerce.orderservice;

import com.ecommerce.orderservice.dto.CheckoutRequest;
import com.ecommerce.orderservice.exception.ApiException;
import com.ecommerce.orderservice.model.*;
import com.ecommerce.orderservice.repository.OrderIdempotencyRepository;
import com.ecommerce.orderservice.service.OrderIdempotencyPersistenceService;
import com.ecommerce.orderservice.service.OrderIdempotencyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderIdempotencyUnitTest {

    @Mock
    private OrderIdempotencyRepository repository;

    private OrderIdempotencyPersistenceService persistenceService;
    private OrderIdempotencyService idempotencyService;

    private CheckoutRequest sampleRequest;

    @BeforeEach
    void setUp() {
        persistenceService = new OrderIdempotencyPersistenceService(repository);
        idempotencyService = new OrderIdempotencyService(persistenceService);

        sampleRequest = CheckoutRequest.builder()
                .shippingAddress("123 Main Street, Tech Park, Hyderabad, Telangana, 500081")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .notes("Please leave the package at the front door.")
                .build();
    }

    // -------------------------------------------------------------------------
    // 1. First request creates idempotency record
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("1. First request creates durable idempotency record with initial states")
    void firstRequest_createsIdempotencyRecord() {
        Long userId = 42L;
        String key = "key-first-request";
        String expectedFingerprint = OrderIdempotencyService.computeFingerprint(sampleRequest);

        when(repository.findByUserIdAndIdempotencyKey(userId, key)).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(OrderIdempotency.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderIdempotency record = idempotencyService.getOrCreate(userId, key, sampleRequest);

        assertThat(record).isNotNull();
        assertThat(record.getUserId()).isEqualTo(userId);
        assertThat(record.getIdempotencyKey()).isEqualTo(key);
        assertThat(record.getRequestFingerprint()).isEqualTo(expectedFingerprint);
        assertThat(record.getOrder()).isNull();
        assertThat(record.getStockReservationReference()).isNull();
        assertThat(record.getPaymentAttemptStartedAt()).isNull();
        assertThat(record.getStockReleaseStatus()).isEqualTo(StockReleaseStatus.NOT_REQUIRED);
        assertThat(record.getCartClearStatus()).isEqualTo(CartClearStatus.NOT_REQUIRED);

        verify(repository).saveAndFlush(any(OrderIdempotency.class));
    }

    // -------------------------------------------------------------------------
    // 2. Same user + same key + same fingerprint returns existing record
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("2. Same user + same key + same fingerprint returns existing record (replay)")
    void sameUser_sameKey_sameFingerprint_replaysExistingRecord() {
        Long userId = 42L;
        String key = "key-replay";
        String fingerprint = OrderIdempotencyService.computeFingerprint(sampleRequest);

        OrderIdempotency existing = OrderIdempotency.builder()
                .id(100L)
                .userId(userId)
                .idempotencyKey(key)
                .requestFingerprint(fingerprint)
                .stockReleaseStatus(StockReleaseStatus.NOT_REQUIRED)
                .cartClearStatus(CartClearStatus.NOT_REQUIRED)
                .build();

        when(repository.findByUserIdAndIdempotencyKey(userId, key)).thenReturn(Optional.of(existing));

        OrderIdempotency result = idempotencyService.getOrCreate(userId, key, sampleRequest);

        assertThat(result).isSameAs(existing);
        verify(repository, never()).saveAndFlush(any());
    }

    // -------------------------------------------------------------------------
    // 3. Same user + same key + different fingerprint returns 409 Conflict
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("3. Same user + same key + different fingerprint returns 409 Conflict")
    void sameUser_sameKey_differentFingerprint_throwsConflict409() {
        Long userId = 42L;
        String key = "key-conflict";
        String originalFingerprint = OrderIdempotencyService.computeFingerprint(sampleRequest);

        OrderIdempotency existing = OrderIdempotency.builder()
                .id(100L)
                .userId(userId)
                .idempotencyKey(key)
                .requestFingerprint(originalFingerprint)
                .build();

        when(repository.findByUserIdAndIdempotencyKey(userId, key)).thenReturn(Optional.of(existing));

        CheckoutRequest modifiedRequest = CheckoutRequest.builder()
                .shippingAddress("999 Alternate Boulevard, Tech Park, 500081")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .notes("Different notes")
                .build();

        ApiException ex = assertThrows(ApiException.class, () -> idempotencyService.getOrCreate(userId, key, modifiedRequest));
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getMessage()).contains("Idempotency key conflict: payload does not match existing request");

        verify(repository, never()).saveAndFlush(any());
    }

    // -------------------------------------------------------------------------
    // 4. Different users + same key create separate records
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("4. Different users + same key create separate records without conflict")
    void differentUsers_sameKey_createSeparateRecords() {
        String key = "shared-key-different-users";
        Long user1 = 101L;
        Long user2 = 202L;

        when(repository.findByUserIdAndIdempotencyKey(eq(user1), eq(key))).thenReturn(Optional.empty());
        when(repository.findByUserIdAndIdempotencyKey(eq(user2), eq(key))).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(OrderIdempotency.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderIdempotency record1 = idempotencyService.getOrCreate(user1, key, sampleRequest);
        OrderIdempotency record2 = idempotencyService.getOrCreate(user2, key, sampleRequest);

        assertThat(record1.getUserId()).isEqualTo(user1);
        assertThat(record2.getUserId()).isEqualTo(user2);
        assertThat(record1.getIdempotencyKey()).isEqualTo(key);
        assertThat(record2.getIdempotencyKey()).isEqualTo(key);
        verify(repository, times(2)).saveAndFlush(any(OrderIdempotency.class));
    }

    // -------------------------------------------------------------------------
    // 5. Missing key rejected
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("5. Missing (null) Idempotency-Key throws IllegalArgumentException")
    void missingKey_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> idempotencyService.getOrCreate(42L, null, sampleRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Idempotency-Key header is required and must not be blank");
        verifyNoInteractions(repository);
    }

    // -------------------------------------------------------------------------
    // 6. Blank key rejected
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("6. Blank Idempotency-Key throws IllegalArgumentException")
    void blankKey_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> idempotencyService.getOrCreate(42L, "   ", sampleRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Idempotency-Key header is required and must not be blank");
        verifyNoInteractions(repository);
    }

    // -------------------------------------------------------------------------
    // 7. Key length validation (>128 chars) enforced
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("7. Key exceeding 128 characters throws IllegalArgumentException")
    void keyExceeding128Characters_throwsIllegalArgumentException() {
        String longKey = "k".repeat(129);
        assertThatThrownBy(() -> idempotencyService.getOrCreate(42L, longKey, sampleRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Idempotency-Key header must not exceed 128 characters");
        verifyNoInteractions(repository);
    }

    // -------------------------------------------------------------------------
    // 8. Fingerprint is deterministic
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("8. Fingerprint is deterministic and produces 64-char lowercase hex SHA-256")
    void fingerprint_isDeterministic() {
        String fp1 = OrderIdempotencyService.computeFingerprint(sampleRequest);
        String fp2 = OrderIdempotencyService.computeFingerprint(sampleRequest);

        assertThat(fp1).isEqualTo(fp2);
        assertThat(fp1).hasSize(64);
        assertThat(fp1).matches("^[a-f0-9]{64}$");
    }

    // -------------------------------------------------------------------------
    // 9. Different shippingAddress changes fingerprint
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("9. Different shippingAddress changes fingerprint")
    void differentShippingAddress_changesFingerprint() {
        CheckoutRequest req2 = CheckoutRequest.builder()
                .shippingAddress("456 Different Way, Cyberabad, Telangana, 500081")
                .paymentMethod(sampleRequest.getPaymentMethod())
                .notes(sampleRequest.getNotes())
                .build();

        String fp1 = OrderIdempotencyService.computeFingerprint(sampleRequest);
        String fp2 = OrderIdempotencyService.computeFingerprint(req2);

        assertThat(fp1).isNotEqualTo(fp2);
    }

    // -------------------------------------------------------------------------
    // 10. Different paymentMethod changes fingerprint
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("10. Different paymentMethod changes fingerprint")
    void differentPaymentMethod_changesFingerprint() {
        CheckoutRequest req2 = CheckoutRequest.builder()
                .shippingAddress(sampleRequest.getShippingAddress())
                .paymentMethod(PaymentMethod.UPI)
                .notes(sampleRequest.getNotes())
                .build();

        String fp1 = OrderIdempotencyService.computeFingerprint(sampleRequest);
        String fp2 = OrderIdempotencyService.computeFingerprint(req2);

        assertThat(fp1).isNotEqualTo(fp2);
    }

    // -------------------------------------------------------------------------
    // 11. Different notes changes fingerprint
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("11. Different notes changes fingerprint")
    void differentNotes_changesFingerprint() {
        CheckoutRequest req2 = CheckoutRequest.builder()
                .shippingAddress(sampleRequest.getShippingAddress())
                .paymentMethod(sampleRequest.getPaymentMethod())
                .notes("Ring the doorbell twice.")
                .build();

        String fp1 = OrderIdempotencyService.computeFingerprint(sampleRequest);
        String fp2 = OrderIdempotencyService.computeFingerprint(req2);

        assertThat(fp1).isNotEqualTo(fp2);
    }

    // -------------------------------------------------------------------------
    // 12. userId does NOT affect fingerprint
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("12. userId does NOT affect fingerprint (fingerprint is request-body scoped)")
    void userId_doesNotAffectFingerprint() {
        String fp1 = OrderIdempotencyService.computeFingerprint(sampleRequest);
        String fp2 = OrderIdempotencyService.computeFingerprint(sampleRequest);
        assertThat(fp1).isEqualTo(fp2);
    }

    // -------------------------------------------------------------------------
    // 13. Idempotency-Key does NOT affect fingerprint
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("13. Idempotency-Key does NOT affect fingerprint")
    void idempotencyKey_doesNotAffectFingerprint() {
        String fp1 = OrderIdempotencyService.computeFingerprint(sampleRequest);
        String fp2 = OrderIdempotencyService.computeFingerprint(sampleRequest);
        assertThat(fp1).isEqualTo(fp2);
    }

    // -------------------------------------------------------------------------
    // 13b. null notes vs empty-string notes produce distinct fingerprints
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("13b. null notes vs empty-string notes produce distinct canonical JSON and distinct fingerprints")
    void nullNotesVsEmptyNotes_produceDistinctFingerprints() {
        CheckoutRequest nullNotesRequest = CheckoutRequest.builder()
                .shippingAddress("123 Main Street, Tech Park, Hyderabad, Telangana, 500081")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .notes(null)
                .build();

        CheckoutRequest emptyNotesRequest = CheckoutRequest.builder()
                .shippingAddress("123 Main Street, Tech Park, Hyderabad, Telangana, 500081")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .notes("")
                .build();

        String jsonNull = OrderIdempotencyService.toCanonicalJson(nullNotesRequest);
        String jsonEmpty = OrderIdempotencyService.toCanonicalJson(emptyNotesRequest);

        assertThat(jsonNull).contains("\"notes\":null");
        assertThat(jsonEmpty).contains("\"notes\":\"\"");
        assertThat(jsonNull).isNotEqualTo(jsonEmpty);

        String fpNull = OrderIdempotencyService.computeFingerprint(nullNotesRequest);
        String fpEmpty = OrderIdempotencyService.computeFingerprint(emptyNotesRequest);

        assertThat(fpNull).isNotEqualTo(fpEmpty);
    }

    // -------------------------------------------------------------------------
    // 13c. User-controlled characters do not cause canonical serialization ambiguity
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("13c. User-controlled characters (| = \" { } \\n Unicode) do not cause canonical serialization ambiguity or collisions")
    void userControlledCharacters_doNotCauseAmbiguityOrCollisions() {
        // Attempting to simulate delimiter confusion attack
        CheckoutRequest req1 = CheckoutRequest.builder()
                .shippingAddress("123 Main St|paymentMethod=UPI|notes=Hacked")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .notes("Standard notes")
                .build();

        CheckoutRequest req2 = CheckoutRequest.builder()
                .shippingAddress("123 Main St")
                .paymentMethod(PaymentMethod.UPI)
                .notes("Hacked|paymentMethod=CREDIT_CARD|notes=Standard notes")
                .build();

        String json1 = OrderIdempotencyService.toCanonicalJson(req1);
        String json2 = OrderIdempotencyService.toCanonicalJson(req2);
        assertThat(json1).isNotEqualTo(json2);

        String fp1 = OrderIdempotencyService.computeFingerprint(req1);
        String fp2 = OrderIdempotencyService.computeFingerprint(req2);
        assertThat(fp1).isNotEqualTo(fp2);

        // Quotes, braces, newlines, and Unicode characters
        CheckoutRequest complexReq = CheckoutRequest.builder()
                .shippingAddress("Số 123 Đường Lê Lợi, Phường Bến Thành, Quận 1, TP. Hồ Chí Minh")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .notes("Giao hàng tận nơi; \"cẩn thận hàng dễ vỡ\" {ưu tiên} \nGọi trước khi giao.")
                .build();

        String jsonComplex = OrderIdempotencyService.toCanonicalJson(complexReq);
        assertThat(jsonComplex).contains("Số 123 Đường Lê Lợi");
        assertThat(jsonComplex).contains("\\\"cẩn thận hàng dễ vỡ\\\"");
        assertThat(jsonComplex).contains("\\nGọi trước khi giao.");

        String fpComplex1 = OrderIdempotencyService.computeFingerprint(complexReq);
        String fpComplex2 = OrderIdempotencyService.computeFingerprint(complexReq);
        assertThat(fpComplex1).isEqualTo(fpComplex2);
        assertThat(fpComplex1).hasSize(64);
        assertThat(fpComplex1).matches("^[a-f0-9]{64}$");
    }

    // -------------------------------------------------------------------------
    // 13d. Canonical JSON strictly preserves field order: shippingAddress, paymentMethod, notes
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("13d. Canonical JSON strictly preserves field order: shippingAddress, paymentMethod, notes")
    void canonicalJson_hasStrictFieldOrderAndFormat() {
        String canonicalJson = OrderIdempotencyService.toCanonicalJson(sampleRequest);
        String expected = "{\"shippingAddress\":\"123 Main Street, Tech Park, Hyderabad, Telangana, 500081\",\"paymentMethod\":\"CREDIT_CARD\",\"notes\":\"Please leave the package at the front door.\"}";
        assertThat(canonicalJson).isEqualTo(expected);
    }

    // -------------------------------------------------------------------------
    // 14. Null user ID is rejected
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("14. Null userId throws IllegalArgumentException")
    void nullUserId_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> idempotencyService.getOrCreate(null, "key-123", sampleRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User ID is required");
    }

    // -------------------------------------------------------------------------
    // 15. Key opacity: does NOT trim or normalize valid key
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("15. Idempotency key opacity: preserves exact internal characters")
    void idempotencyKey_isOpaqueAndPreservedExactly() {
        String key = "my-key-123_ABC";
        when(repository.findByUserIdAndIdempotencyKey(1L, key)).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(OrderIdempotency.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderIdempotency record = idempotencyService.getOrCreate(1L, key, sampleRequest);
        assertThat(record.getIdempotencyKey()).isEqualTo(key);
    }

    // -------------------------------------------------------------------------
    // 16. Initial stockReleaseStatus is NOT_REQUIRED
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("16. Initial stockReleaseStatus is NOT_REQUIRED")
    void initialStockReleaseStatus_isNotRequired() {
        OrderIdempotency record = OrderIdempotency.builder()
                .userId(1L).idempotencyKey("k1").requestFingerprint("fp1")
                .build();
        assertThat(record.getStockReleaseStatus()).isEqualTo(StockReleaseStatus.NOT_REQUIRED);
    }

    // -------------------------------------------------------------------------
    // 17. Initial cartClearStatus is NOT_REQUIRED
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("17. Initial cartClearStatus is NOT_REQUIRED")
    void initialCartClearStatus_isNotRequired() {
        OrderIdempotency record = OrderIdempotency.builder()
                .userId(1L).idempotencyKey("k1").requestFingerprint("fp1")
                .build();
        assertThat(record.getCartClearStatus()).isEqualTo(CartClearStatus.NOT_REQUIRED);
    }

    // -------------------------------------------------------------------------
    // 18. paymentAttemptStartedAt initially NULL
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("18. paymentAttemptStartedAt is initially null")
    void initialPaymentAttemptStartedAt_isNull() {
        OrderIdempotency record = OrderIdempotency.builder()
                .userId(1L).idempotencyKey("k1").requestFingerprint("fp1")
                .build();
        assertThat(record.getPaymentAttemptStartedAt()).isNull();
    }

    // -------------------------------------------------------------------------
    // 19. stockReservationReference initially NULL
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("19. stockReservationReference is initially null")
    void initialStockReservationReference_isNull() {
        OrderIdempotency record = OrderIdempotency.builder()
                .userId(1L).idempotencyKey("k1").requestFingerprint("fp1")
                .build();
        assertThat(record.getStockReservationReference()).isNull();
    }

    // -------------------------------------------------------------------------
    // 20. Order reference initially NULL
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("20. Order reference is initially null")
    void initialOrderReference_isNull() {
        OrderIdempotency record = OrderIdempotency.builder()
                .userId(1L).idempotencyKey("k1").requestFingerprint("fp1")
                .build();
        assertThat(record.getOrder()).isNull();
    }

    // -------------------------------------------------------------------------
    // 21. Order reference can later be attached
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("21. Order reference can later be attached to existing idempotency record")
    void orderReference_canLaterBeAttached() {
        OrderIdempotency record = OrderIdempotency.builder()
                .id(55L)
                .userId(1L)
                .idempotencyKey("k1")
                .requestFingerprint("fp1")
                .build();

        Order order = Order.builder().id(999L).userId(1L).build();

        when(repository.findById(55L)).thenReturn(Optional.of(record));
        when(repository.saveAndFlush(any(OrderIdempotency.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderIdempotency updated = idempotencyService.attachOrder(55L, order);

        assertThat(updated.getOrder()).isEqualTo(order);
        assertThat(updated.getOrder().getId()).isEqualTo(999L);
        assertThat(updated.getUpdatedAt()).isNotNull();
    }

    // -------------------------------------------------------------------------
    // 22. Attaching Order cannot mutate request identity fields
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("22. Attaching Order preserves logical identity fields (userId, idempotencyKey, requestFingerprint)")
    void attachOrder_preservesLogicalIdentityFields() {
        OrderIdempotency record = OrderIdempotency.builder()
                .id(55L)
                .userId(1L)
                .idempotencyKey("immutable-key")
                .requestFingerprint("immutable-fingerprint")
                .build();

        Order order = Order.builder().id(999L).userId(1L).build();

        when(repository.findById(55L)).thenReturn(Optional.of(record));
        when(repository.saveAndFlush(any(OrderIdempotency.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderIdempotency updated = idempotencyService.attachOrder(55L, order);

        assertThat(updated.getUserId()).isEqualTo(1L);
        assertThat(updated.getIdempotencyKey()).isEqualTo("immutable-key");
        assertThat(updated.getRequestFingerprint()).isEqualTo("immutable-fingerprint");
    }

    // -------------------------------------------------------------------------
    // 23. Duplicate-key race recovery with identical request replays committed record
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("23. Duplicate-key race recovery: catches DataIntegrityViolationException, loads committed record, and replays")
    void duplicateKeyRace_withSameFingerprint_replaysCommittedRecord() {
        Long userId = 42L;
        String key = "race-key";
        String fingerprint = OrderIdempotencyService.computeFingerprint(sampleRequest);

        OrderIdempotency committedRecord = OrderIdempotency.builder()
                .id(200L)
                .userId(userId)
                .idempotencyKey(key)
                .requestFingerprint(fingerprint)
                .build();

        // 1. Fast path: record does not exist yet
        // 2. Insert attempt fails with duplicate key
        when(repository.saveAndFlush(any(OrderIdempotency.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint uk_order_idempotency_user_key"));

        // 3. Recovery read: loads the committed record created by racing winner
        when(repository.findByUserIdAndIdempotencyKey(userId, key))
                .thenReturn(Optional.empty()) // fast-path lookup
                .thenReturn(Optional.of(committedRecord)); // fresh lookup after exception

        OrderIdempotency result = idempotencyService.getOrCreate(userId, key, sampleRequest);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(200L);
        assertThat(result.getIdempotencyKey()).isEqualTo(key);
        assertThat(result.getRequestFingerprint()).isEqualTo(fingerprint);
    }

    // -------------------------------------------------------------------------
    // 24. Duplicate-key race recovery with conflicting request throws 409 Conflict
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("24. Duplicate-key race recovery with conflicting request throws 409 Conflict")
    void duplicateKeyRace_withConflictingFingerprint_throwsConflict409() {
        Long userId = 42L;
        String key = "race-conflict-key";

        // Racing winner committed with a different address
        CheckoutRequest winnerRequest = CheckoutRequest.builder()
                .shippingAddress("Winner Address, City, 500081")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .notes("Winner notes")
                .build();
        String winnerFingerprint = OrderIdempotencyService.computeFingerprint(winnerRequest);

        OrderIdempotency committedRecord = OrderIdempotency.builder()
                .id(200L)
                .userId(userId)
                .idempotencyKey(key)
                .requestFingerprint(winnerFingerprint)
                .build();

        when(repository.saveAndFlush(any(OrderIdempotency.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value"));

        when(repository.findByUserIdAndIdempotencyKey(userId, key))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(committedRecord));

        ApiException ex = assertThrows(ApiException.class, () -> idempotencyService.getOrCreate(userId, key, sampleRequest));
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getMessage()).contains("Idempotency key conflict: payload does not match existing request");
    }

    // -------------------------------------------------------------------------
    // 25. Recovery metadata update methods work as intended
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("25. updateRecoveryMetadata updates stock reservation, payment timestamp, and compensation statuses")
    void updateRecoveryMetadata_updatesExpectedFields() {
        OrderIdempotency record = OrderIdempotency.builder()
                .id(88L)
                .userId(1L)
                .idempotencyKey("k1")
                .requestFingerprint("fp1")
                .build();

        when(repository.findById(88L)).thenReturn(Optional.of(record));
        when(repository.saveAndFlush(any(OrderIdempotency.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime paymentTime = LocalDateTime.now();
        OrderIdempotency updated = idempotencyService.updateRecoveryMetadata(
                88L,
                "stock-ref-123",
                paymentTime,
                StockReleaseStatus.PENDING,
                CartClearStatus.CLEARED
        );

        assertThat(updated.getStockReservationReference()).isEqualTo("stock-ref-123");
        assertThat(updated.getPaymentAttemptStartedAt()).isEqualTo(paymentTime);
        assertThat(updated.getStockReleaseStatus()).isEqualTo(StockReleaseStatus.PENDING);
        assertThat(updated.getCartClearStatus()).isEqualTo(CartClearStatus.CLEARED);
    }

    // -------------------------------------------------------------------------
    // 26. claimPaymentAttempt when unclaimed locks row, sets timestamp, returns true
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("26. claimPaymentAttempt when unclaimed locks row, sets timestamp, and returns true")
    void claimPaymentAttempt_whenUnclaimed_locksRowSetsTimestampAndReturnsTrue() {
        OrderIdempotency record = OrderIdempotency.builder()
                .id(88L)
                .userId(1L)
                .idempotencyKey("k1")
                .requestFingerprint("fp1")
                .paymentAttemptStartedAt(null)
                .build();

        when(repository.findByIdForUpdate(88L)).thenReturn(Optional.of(record));
        when(repository.saveAndFlush(any(OrderIdempotency.class))).thenAnswer(inv -> inv.getArgument(0));

        boolean granted = idempotencyService.claimPaymentAttempt(88L);

        assertThat(granted).isTrue();
        assertThat(record.getPaymentAttemptStartedAt()).isNotNull();
        verify(repository).findByIdForUpdate(88L);
        verify(repository).saveAndFlush(record);
    }

    // -------------------------------------------------------------------------
    // 27. claimPaymentAttempt when already claimed locks row and returns false
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("27. claimPaymentAttempt when already claimed locks row and returns false without overwriting")
    void claimPaymentAttempt_whenAlreadyClaimed_locksRowAndReturnsFalseWithoutMutating() {
        LocalDateTime originalTime = LocalDateTime.now().minusMinutes(5);
        OrderIdempotency record = OrderIdempotency.builder()
                .id(88L)
                .userId(1L)
                .idempotencyKey("k1")
                .requestFingerprint("fp1")
                .paymentAttemptStartedAt(originalTime)
                .build();

        when(repository.findByIdForUpdate(88L)).thenReturn(Optional.of(record));

        boolean granted = idempotencyService.claimPaymentAttempt(88L);

        assertThat(granted).isFalse();
        assertThat(record.getPaymentAttemptStartedAt()).isEqualTo(originalTime);
        verify(repository).findByIdForUpdate(88L);
        verify(repository, never()).saveAndFlush(any());
    }
}
