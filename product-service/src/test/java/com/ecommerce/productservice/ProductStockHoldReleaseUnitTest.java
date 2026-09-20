package com.ecommerce.productservice;

import com.ecommerce.productservice.dto.HeldItemDto;
import com.ecommerce.productservice.dto.HoldStockRequest;
import com.ecommerce.productservice.dto.HoldStockResponse;
import com.ecommerce.productservice.dto.ReleaseStockRequest;
import com.ecommerce.productservice.dto.ReleaseStockResponse;
import com.ecommerce.productservice.dto.StockItemRequest;
import com.ecommerce.productservice.exception.ApiException;
import com.ecommerce.productservice.exception.InsufficientStockException;
import com.ecommerce.productservice.exception.ProductNotFoundException;
import com.ecommerce.productservice.model.Category;
import com.ecommerce.productservice.model.Product;
import com.ecommerce.productservice.model.ProductOperationItem;
import com.ecommerce.productservice.model.ProductOperationMarker;
import com.ecommerce.productservice.model.ProductOperationStatus;
import com.ecommerce.productservice.repository.ProductOperationMarkerRepository;
import com.ecommerce.productservice.repository.ProductRepository;
import com.ecommerce.productservice.service.CategoryService;
import com.ecommerce.productservice.service.ProductOperationService;
import com.ecommerce.productservice.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductStockHoldReleaseUnitTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductOperationMarkerRepository markerRepository;

    @Mock
    private CategoryService categoryService;

    private ProductOperationService productOperationService;
    private ProductService productService;

    private Product productA;
    private Product productB;
    private Product productC;

    @BeforeEach
    void setUp() {
        productOperationService = new ProductOperationService(productRepository, markerRepository);
        productService = new ProductService(productRepository, categoryService, productOperationService);

        Category category = Category.builder().id(1L).name("Electronics").build();

        productA = Product.builder()
                .id(10L)
                .name("Product A")
                .sku("SKU-A")
                .price(new BigDecimal("25.00"))
                .stockQuantity(10)
                .category(category)
                .active(true)
                .build();

        productB = Product.builder()
                .id(20L)
                .name("Product B")
                .sku("SKU-B")
                .price(new BigDecimal("50.00"))
                .stockQuantity(2)
                .category(category)
                .active(true)
                .build();

        productC = Product.builder()
                .id(30L)
                .name("Product C")
                .sku("SKU-C")
                .price(new BigDecimal("15.00"))
                .stockQuantity(100)
                .category(category)
                .active(true)
                .build();
    }

    // -------------------------------------------------------------------------
    // 1. first HOLD -> HELD marker created and stock decremented
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("1. First HOLD decrements stock and commits durable HELD marker")
    void holdStock_firstHold_decrementsStockAndSavesHeldMarker() {
        HoldStockRequest request = HoldStockRequest.builder()
                .checkoutOperationKey("op-key-1")
                .items(List.of(new StockItemRequest(10L, 4)))
                .build();

        when(markerRepository.findByCheckoutOperationKey("op-key-1")).thenReturn(Optional.empty());
        when(productRepository.holdStock(10L, 4)).thenReturn(1);
        when(productRepository.findAllById(List.of(10L))).thenReturn(List.of(productA));

        HoldStockResponse response = productService.holdStock(request);

        assertThat(response).isNotNull();
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getProductId()).isEqualTo(10L);
        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(4);

        verify(productRepository).holdStock(10L, 4);
        verify(markerRepository).saveAndFlush(any(ProductOperationMarker.class));
    }

    // -------------------------------------------------------------------------
    // 2. repeated same-key HOLD -> replay without stock mutation
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("2. Repeated same-key HOLD with matching payload replays response with zero stock mutation")
    void holdStock_repeatedSameKey_replaysResponseWithoutMutatingStock() {
        String key = "op-key-replay";
        Map<Long, Integer> aggregated = Map.of(10L, 4);
        String fingerprint = ProductService.computeFingerprint(aggregated);

        ProductOperationMarker existingMarker = ProductOperationMarker.builder()
                .checkoutOperationKey(key)
                .status(ProductOperationStatus.HELD)
                .requestFingerprint(fingerprint)
                .items(List.of(ProductOperationItem.builder()
                        .productId(10L)
                        .productName("Product A")
                        .productSku("SKU-A")
                        .price(new BigDecimal("25.00"))
                        .quantity(4)
                        .build()))
                .build();

        when(markerRepository.findByCheckoutOperationKey(key)).thenReturn(Optional.of(existingMarker));

        HoldStockRequest retryRequest = HoldStockRequest.builder()
                .checkoutOperationKey(key)
                .items(List.of(new StockItemRequest(10L, 4)))
                .build();

        HoldStockResponse response = productService.holdStock(retryRequest);

        assertThat(response).isNotNull();
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getProductId()).isEqualTo(10L);
        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(4);

        verify(productRepository, never()).holdStock(anyLong(), anyInt());
        verify(markerRepository, never()).saveAndFlush(any());
    }

    // -------------------------------------------------------------------------
    // 3. same-key different-payload HOLD -> 409 Conflict
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("3. Same-key HOLD with conflicting payload throws 409 Conflict without stock mutation")
    void holdStock_sameKeyDifferentPayload_throwsConflict409() {
        String key = "op-key-conflict";
        String originalFingerprint = ProductService.computeFingerprint(Map.of(10L, 4));

        ProductOperationMarker existingMarker = ProductOperationMarker.builder()
                .checkoutOperationKey(key)
                .status(ProductOperationStatus.HELD)
                .requestFingerprint(originalFingerprint)
                .items(List.of(ProductOperationItem.builder().productId(10L).quantity(4).build()))
                .build();

        when(markerRepository.findByCheckoutOperationKey(key)).thenReturn(Optional.of(existingMarker));

        HoldStockRequest conflictingRequest = HoldStockRequest.builder()
                .checkoutOperationKey(key)
                .items(List.of(new StockItemRequest(10L, 7))) // different quantity!
                .build();

        ApiException ex = assertThrows(ApiException.class, () -> productService.holdStock(conflictingRequest));
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getMessage()).contains("Operation key conflict");

        verify(productRepository, never()).holdStock(anyLong(), anyInt());
    }

    // -------------------------------------------------------------------------
    // 4. same key already RELEASED + HOLD -> 409 Conflict
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("4. Same key already RELEASED throws 409 Conflict on subsequent HOLD")
    void holdStock_sameKeyAlreadyReleased_throwsConflict409() {
        String key = "op-key-released";
        ProductOperationMarker releasedMarker = ProductOperationMarker.builder()
                .checkoutOperationKey(key)
                .status(ProductOperationStatus.RELEASED)
                .requestFingerprint("some-hash")
                .build();

        when(markerRepository.findByCheckoutOperationKey(key)).thenReturn(Optional.of(releasedMarker));

        HoldStockRequest request = HoldStockRequest.builder()
                .checkoutOperationKey(key)
                .items(List.of(new StockItemRequest(10L, 2)))
                .build();

        ApiException ex = assertThrows(ApiException.class, () -> productService.holdStock(request));
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getMessage()).contains("already been released");

        verify(productRepository, never()).holdStock(anyLong(), anyInt());
    }

    // -------------------------------------------------------------------------
    // 5. failed HOLD -> no marker
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("5. Failed HOLD (insufficient stock) throws exception and never persists marker")
    void holdStock_insufficientStock_throwsExceptionAndNeverPersistsMarker() {
        HoldStockRequest request = HoldStockRequest.builder()
                .checkoutOperationKey("op-fail-1")
                .items(List.of(new StockItemRequest(10L, 15)))
                .build();

        when(markerRepository.findByCheckoutOperationKey("op-fail-1")).thenReturn(Optional.empty());
        when(productRepository.holdStock(10L, 15)).thenReturn(0);
        when(productRepository.findById(10L)).thenReturn(Optional.of(productA));

        assertThrows(InsufficientStockException.class, () -> productService.holdStock(request));

        verify(markerRepository, never()).saveAndFlush(any());
    }

    // -------------------------------------------------------------------------
    // 6. multi-item partial failure (CRITICAL ATOMICITY SCENARIO: A=10, B=2, req: A=4, B=3)
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("6. Multi-item partial failure throws exception, stops processing, and never persists marker")
    void holdStock_multiItemPartialFailure_stopsProcessingAndNeverPersistsMarker() {
        HoldStockRequest request = HoldStockRequest.builder()
                .checkoutOperationKey("op-atomic-fail")
                .items(List.of(
                        new StockItemRequest(10L, 4),
                        new StockItemRequest(20L, 3),
                        new StockItemRequest(30L, 5)
                ))
                .build();

        when(markerRepository.findByCheckoutOperationKey("op-atomic-fail")).thenReturn(Optional.empty());
        when(productRepository.holdStock(10L, 4)).thenReturn(1);
        when(productRepository.holdStock(20L, 3)).thenReturn(0);
        when(productRepository.findById(20L)).thenReturn(Optional.of(productB));

        assertThatThrownBy(() -> productService.holdStock(request))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Insufficient stock for product id 20");

        // Subsequent item 30 is never called
        verify(productRepository, never()).holdStock(eq(30L), anyInt());
        // Marker is never saved
        verify(markerRepository, never()).saveAndFlush(any());
    }

    // -------------------------------------------------------------------------
    // 7. RELEASE without marker -> 404 Not Found
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("7. RELEASE without existing marker is rejected with 404 Not Found")
    void releaseStock_withoutMarker_throwsNotFound404() {
        String key = "op-unknown";
        when(markerRepository.findByCheckoutOperationKeyForUpdate(key)).thenReturn(Optional.empty());

        ReleaseStockRequest request = new ReleaseStockRequest(key);

        ApiException ex = assertThrows(ApiException.class, () -> productService.releaseStock(request));
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getMessage()).contains("No hold operation found for key");

        verify(productRepository, never()).releaseStock(anyLong(), anyInt(), anyInt());
    }

    // -------------------------------------------------------------------------
    // 8. HELD -> RELEASED restores exact marker quantities
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("8. Successful RELEASE restores exact marker quantities and sets status to RELEASED")
    void releaseStock_fromHeld_restoresExactMarkerQuantitiesAndSetsReleased() {
        String key = "op-release-success";
        ProductOperationMarker marker = ProductOperationMarker.builder()
                .checkoutOperationKey(key)
                .status(ProductOperationStatus.HELD)
                .items(new ArrayList<>(List.of(
                        ProductOperationItem.builder().productId(10L).quantity(4).build(),
                        ProductOperationItem.builder().productId(20L).quantity(1).build()
                )))
                .build();

        when(markerRepository.findByCheckoutOperationKeyForUpdate(key)).thenReturn(Optional.of(marker));
        when(productRepository.releaseStock(10L, 4, Integer.MAX_VALUE)).thenReturn(1);
        when(productRepository.releaseStock(20L, 1, Integer.MAX_VALUE)).thenReturn(1);

        ReleaseStockRequest request = new ReleaseStockRequest(key);
        ReleaseStockResponse response = productService.releaseStock(request);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("RELEASED");
        assertThat(response.getReleasedItemCount()).isEqualTo(2);

        verify(productRepository).releaseStock(10L, 4, Integer.MAX_VALUE);
        verify(productRepository).releaseStock(20L, 1, Integer.MAX_VALUE);
        assertThat(marker.getStatus()).isEqualTo(ProductOperationStatus.RELEASED);
        verify(markerRepository).saveAndFlush(marker);
    }

    // -------------------------------------------------------------------------
    // 9. RELEASE uses marker quantities, ignores client quantities
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("9. RELEASE strictly restores marker-stored quantities, completely ignoring client-supplied quantities")
    void releaseStock_ignoresClientSuppliedQuantities_restoresExactMarkerQuantities() {
        String key = "op-ignore-client-qty";
        ProductOperationMarker marker = ProductOperationMarker.builder()
                .checkoutOperationKey(key)
                .status(ProductOperationStatus.HELD)
                .items(new ArrayList<>(List.of(
                        ProductOperationItem.builder().productId(10L).quantity(5).build(),
                        ProductOperationItem.builder().productId(20L).quantity(2).build()
                )))
                .build();

        when(markerRepository.findByCheckoutOperationKeyForUpdate(key)).thenReturn(Optional.of(marker));
        when(productRepository.releaseStock(10L, 5, Integer.MAX_VALUE)).thenReturn(1);
        when(productRepository.releaseStock(20L, 2, Integer.MAX_VALUE)).thenReturn(1);

        // Client passes arbitrary quantities (999 and 1)
        ReleaseStockRequest clientRequest = ReleaseStockRequest.builder()
                .checkoutOperationKey(key)
                .items(List.of(
                        new StockItemRequest(10L, 999),
                        new StockItemRequest(20L, 1)
                ))
                .build();

        ReleaseStockResponse response = productService.releaseStock(clientRequest);

        assertThat(response.getStatus()).isEqualTo("RELEASED");
        assertThat(response.getReleasedItemCount()).isEqualTo(2);

        // Authoritative quantities restored are 5 and 2 (from marker), NOT 999 or 1!
        verify(productRepository).releaseStock(10L, 5, Integer.MAX_VALUE);
        verify(productRepository).releaseStock(20L, 2, Integer.MAX_VALUE);
        verify(productRepository, never()).releaseStock(eq(10L), eq(999), anyInt());
    }

    // -------------------------------------------------------------------------
    // 10. RELEASED -> repeated RELEASE is no-op
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("10. Repeated RELEASE on already RELEASED marker is an idempotent no-op with zero stock mutation")
    void releaseStock_whenAlreadyReleased_isNoOpReplayWithoutMutatingStock() {
        String key = "op-already-released";
        ProductOperationMarker releasedMarker = ProductOperationMarker.builder()
                .checkoutOperationKey(key)
                .status(ProductOperationStatus.RELEASED)
                .items(List.of(ProductOperationItem.builder().productId(10L).quantity(4).build()))
                .build();

        when(markerRepository.findByCheckoutOperationKeyForUpdate(key)).thenReturn(Optional.of(releasedMarker));

        ReleaseStockResponse response = productService.releaseStock(new ReleaseStockRequest(key));

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("RELEASED");
        assertThat(response.getReleasedItemCount()).isEqualTo(1);

        verify(productRepository, never()).releaseStock(anyLong(), anyInt(), anyInt());
        verify(markerRepository, never()).saveAndFlush(any());
    }

    // -------------------------------------------------------------------------
    // 11. HOLD -> deactivate -> RELEASE restores stock
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("11. Full lifecycle: HOLD -> deactivate Product -> RELEASE restores exact stock on inactive product")
    void hold_deactivate_release_restoresExactStockOnInactiveProduct() {
        String key = "op-deactivate-lifecycle";
        Category category = Category.builder().id(1L).name("Electronics").build();
        Product product = Product.builder()
                .id(50L)
                .name("Lifecycle Item")
                .sku("LIFE-001")
                .price(new BigDecimal("25.00"))
                .stockQuantity(10)
                .category(category)
                .active(true)
                .build();

        // 1. Hold 5 units
        when(markerRepository.findByCheckoutOperationKey(key)).thenReturn(Optional.empty());
        when(productRepository.holdStock(50L, 5)).thenAnswer(inv -> {
            product.setStockQuantity(product.getStockQuantity() - 5);
            return 1;
        });
        when(productRepository.findAllById(List.of(50L))).thenAnswer(inv -> List.of(product));
        when(productRepository.findById(50L)).thenAnswer(inv -> Optional.of(product));

        HoldStockRequest holdRequest = HoldStockRequest.builder()
                .checkoutOperationKey(key)
                .items(List.of(new StockItemRequest(50L, 5)))
                .build();
        HoldStockResponse holdResponse = productService.holdStock(holdRequest);
        assertThat(holdResponse.getItems()).hasSize(1);
        assertThat(product.getStockQuantity()).isEqualTo(5);

        // 2. Soft-delete product
        productService.deleteProduct(50L);
        assertThat(product.isActive()).isFalse();
        assertThat(product.getStockQuantity()).isEqualTo(5);

        // 3. Release 5 units
        ProductOperationMarker marker = ProductOperationMarker.builder()
                .checkoutOperationKey(key)
                .status(ProductOperationStatus.HELD)
                .items(new ArrayList<>(List.of(
                        ProductOperationItem.builder().productId(50L).quantity(5).build()
                )))
                .build();
        when(markerRepository.findByCheckoutOperationKeyForUpdate(key)).thenReturn(Optional.of(marker));
        when(productRepository.releaseStock(50L, 5, Integer.MAX_VALUE)).thenAnswer(inv -> {
            product.setStockQuantity(product.getStockQuantity() + 5);
            return 1;
        });

        ReleaseStockResponse releaseResponse = productService.releaseStock(new ReleaseStockRequest(key));
        assertThat(releaseResponse.getStatus()).isEqualTo("RELEASED");
        assertThat(product.isActive()).isFalse();
        assertThat(product.getStockQuantity()).isEqualTo(10);
    }

    // -------------------------------------------------------------------------
    // 12. missing or blank operation key -> 400 Bad Request
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("12. Missing, empty, or blank checkoutOperationKey throws 400 Bad Request")
    void holdAndRelease_missingOrBlankOperationKey_throwsBadRequest400() {
        HoldStockRequest nullKeyHold = HoldStockRequest.builder()
                .checkoutOperationKey(null)
                .items(List.of(new StockItemRequest(10L, 1)))
                .build();
        HoldStockRequest blankKeyHold = HoldStockRequest.builder()
                .checkoutOperationKey("   ")
                .items(List.of(new StockItemRequest(10L, 1)))
                .build();

        ApiException ex1 = assertThrows(ApiException.class, () -> productService.holdStock(nullKeyHold));
        assertThat(ex1.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);

        ApiException ex2 = assertThrows(ApiException.class, () -> productService.holdStock(blankKeyHold));
        assertThat(ex2.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);

        ApiException ex3 = assertThrows(ApiException.class, () -> productService.releaseStock(new ReleaseStockRequest(null)));
        assertThat(ex3.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);

        ApiException ex4 = assertThrows(ApiException.class, () -> productService.releaseStock(new ReleaseStockRequest("  ")));
        assertThat(ex4.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);

        verifyNoInteractions(productRepository);
    }

    // -------------------------------------------------------------------------
    // 13. operation key exceeds max length -> 400 Bad Request
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("13. Operation key exceeding 128 characters throws 400 Bad Request")
    void holdAndRelease_keyExceedsMaxLength_throwsBadRequest400() {
        String longKey = "a".repeat(129);

        HoldStockRequest holdRequest = HoldStockRequest.builder()
                .checkoutOperationKey(longKey)
                .items(List.of(new StockItemRequest(10L, 1)))
                .build();

        ApiException exHold = assertThrows(ApiException.class, () -> productService.holdStock(holdRequest));
        assertThat(exHold.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exHold.getMessage()).contains("must not exceed 128 characters");

        ApiException exRel = assertThrows(ApiException.class, () -> productService.releaseStock(new ReleaseStockRequest(longKey)));
        assertThat(exRel.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exRel.getMessage()).contains("must not exceed 128 characters");
    }

    // -------------------------------------------------------------------------
    // 14. duplicate-key race recovery -> replay committed result
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("14. Duplicate-key race recovery: when concurrent insert fails, catches DataIntegrityViolationException and replays committed marker")
    void duplicateKeyRace_recoversFromCommittedMarker_replayingResult() {
        String key = "op-race-key";
        HoldStockRequest request = HoldStockRequest.builder()
                .checkoutOperationKey(key)
                .items(List.of(new StockItemRequest(10L, 2)))
                .build();

        String fingerprint = ProductService.computeFingerprint(Map.of(10L, 2));

        ProductOperationMarker committedMarker = ProductOperationMarker.builder()
                .checkoutOperationKey(key)
                .status(ProductOperationStatus.HELD)
                .requestFingerprint(fingerprint)
                .items(List.of(ProductOperationItem.builder()
                        .productId(10L)
                        .productName("Product A")
                        .productSku("SKU-A")
                        .price(new BigDecimal("25.00"))
                        .quantity(2)
                        .build()))
                .build();

        // Fast-path returns empty (simulating both threads seeing empty marker initially)
        // In executeHoldStock: stock updates succeed
        when(productRepository.holdStock(10L, 2)).thenReturn(1);
        when(productRepository.findAllById(List.of(10L))).thenReturn(List.of(productA));

        // When saving marker, Thread 2 hits duplicate-key unique constraint!
        when(markerRepository.saveAndFlush(any(ProductOperationMarker.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint uk_operation_key"));

        // During recovery in fresh read: marker committed by Thread 1 is found
        when(markerRepository.findByCheckoutOperationKey(key))
                .thenReturn(Optional.empty()) // initial check in holdStock
                .thenReturn(Optional.empty()) // re-check in executeHoldStock
                .thenReturn(Optional.of(committedMarker)); // recovery check after DataIntegrityViolationException!

        HoldStockResponse response = productService.holdStock(request);

        assertThat(response).isNotNull();
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getProductId()).isEqualTo(10L);
        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(2);
    }

    // -------------------------------------------------------------------------
    // 15. duplicate-key race with conflicting payload -> 409 Conflict
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("15. Duplicate-key race recovery with conflicting payload throws 409 Conflict")
    void duplicateKeyRace_withConflictingPayload_throwsConflict409() {
        String key = "op-race-conflict";
        HoldStockRequest request = HoldStockRequest.builder()
                .checkoutOperationKey(key)
                .items(List.of(new StockItemRequest(10L, 5))) // thread 2 requests 5
                .build();

        // Thread 1 committed 2 units with different fingerprint
        String thread1Fingerprint = ProductService.computeFingerprint(Map.of(10L, 2));
        ProductOperationMarker committedMarker = ProductOperationMarker.builder()
                .checkoutOperationKey(key)
                .status(ProductOperationStatus.HELD)
                .requestFingerprint(thread1Fingerprint)
                .items(List.of(ProductOperationItem.builder().productId(10L).quantity(2).build()))
                .build();

        when(productRepository.holdStock(10L, 5)).thenReturn(1);
        when(productRepository.findAllById(List.of(10L))).thenReturn(List.of(productA));

        when(markerRepository.saveAndFlush(any(ProductOperationMarker.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value"));

        when(markerRepository.findByCheckoutOperationKey(key))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(committedMarker));

        ApiException ex = assertThrows(ApiException.class, () -> productService.holdStock(request));
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getMessage()).contains("Operation key conflict");
    }

    // -------------------------------------------------------------------------
    // 16. duplicate line aggregation
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("16. Duplicate lines in request for same product are aggregated before hold")
    void holdStock_duplicateLines_areAggregated() {
        HoldStockRequest request = HoldStockRequest.builder()
                .checkoutOperationKey("op-dup-agg")
                .items(List.of(
                        new StockItemRequest(10L, 2),
                        new StockItemRequest(10L, 3)
                ))
                .build();

        when(markerRepository.findByCheckoutOperationKey("op-dup-agg")).thenReturn(Optional.empty());
        when(productRepository.holdStock(10L, 5)).thenReturn(1);
        when(productRepository.findAllById(List.of(10L))).thenReturn(List.of(productA));

        HoldStockResponse response = productService.holdStock(request);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(5);
        verify(productRepository).holdStock(10L, 5);
    }

    // -------------------------------------------------------------------------
    // 17. numeric overflow rejected
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("17. Numeric overflow during aggregation throws 400 Bad Request")
    void holdStock_numericOverflow_throwsBadRequest400() {
        HoldStockRequest request = HoldStockRequest.builder()
                .checkoutOperationKey("op-overflow")
                .items(List.of(
                        new StockItemRequest(10L, Integer.MAX_VALUE),
                        new StockItemRequest(10L, 1)
                ))
                .build();

        ApiException ex = assertThrows(ApiException.class, () -> productService.holdStock(request));
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getMessage()).contains("Quantity overflow");

        verifyNoInteractions(productRepository);
        verifyNoInteractions(markerRepository);
    }

    // -------------------------------------------------------------------------
    // 18. deterministic sorting order
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("18. Products are always processed in ascending sorted order of productId")
    void holdStock_deterministicProductProcessingOrder_processesInSortedOrder() {
        HoldStockRequest request = HoldStockRequest.builder()
                .checkoutOperationKey("op-sort-order")
                .items(List.of(
                        new StockItemRequest(30L, 1),
                        new StockItemRequest(10L, 2),
                        new StockItemRequest(20L, 3)
                ))
                .build();

        when(markerRepository.findByCheckoutOperationKey("op-sort-order")).thenReturn(Optional.empty());
        when(productRepository.holdStock(anyLong(), anyInt())).thenReturn(1);
        when(productRepository.findAllById(any())).thenReturn(List.of(productA, productB, productC));

        productService.holdStock(request);

        InOrder inOrder = inOrder(productRepository);
        inOrder.verify(productRepository).holdStock(10L, 2);
        inOrder.verify(productRepository).holdStock(20L, 3);
        inOrder.verify(productRepository).holdStock(30L, 1);
    }
}
