package com.ecommerce.productservice;

import com.ecommerce.productservice.controller.ProductController;
import com.ecommerce.productservice.dto.HeldItemDto;
import com.ecommerce.productservice.dto.HoldStockRequest;
import com.ecommerce.productservice.dto.HoldStockResponse;
import com.ecommerce.productservice.dto.ReleaseStockRequest;
import com.ecommerce.productservice.dto.ReleaseStockResponse;
import com.ecommerce.productservice.exception.ApiException;
import com.ecommerce.productservice.exception.GlobalExceptionHandler;
import com.ecommerce.productservice.exception.InsufficientStockException;
import com.ecommerce.productservice.exception.ProductNotFoundException;
import com.ecommerce.productservice.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductStockHoldReleaseContractTest {

    @Mock
    private ProductService productService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ProductController(productService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/products/hold with valid request returns 200 and HoldStockResponse")
    void hold_validRequest_returns200() throws Exception {
        String requestJson = """
                {
                    "checkoutOperationKey": "chk-op-123",
                    "items": [
                        { "productId": 10, "quantity": 2 },
                        { "productId": 20, "quantity": 1 }
                    ]
                }
                """;

        HoldStockResponse mockResponse = HoldStockResponse.builder()
                .items(List.of(
                        HeldItemDto.builder()
                                .productId(10L)
                                .name("Product 10")
                                .sku("SKU-10")
                                .price(new BigDecimal("20.00"))
                                .quantity(2)
                                .build(),
                        HeldItemDto.builder()
                                .productId(20L)
                                .name("Product 20")
                                .sku("SKU-20")
                                .price(new BigDecimal("50.00"))
                                .quantity(1)
                                .build()
                ))
                .build();

        when(productService.holdStock(any(HoldStockRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/products/hold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].productId").value(10))
                .andExpect(jsonPath("$.items[0].name").value("Product 10"))
                .andExpect(jsonPath("$.items[0].sku").value("SKU-10"))
                .andExpect(jsonPath("$.items[0].price").value(20.00))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[1].productId").value(20))
                .andExpect(jsonPath("$.items[1].quantity").value(1));
    }

    @Test
    @DisplayName("POST /api/v1/products/hold with missing operation key returns 400 Bad Request")
    void hold_missingOperationKey_returns400() throws Exception {
        String requestJson = """
                {
                    "items": [{ "productId": 10, "quantity": 2 }]
                }
                """;

        mockMvc.perform(post("/api/v1/products/hold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(productService);
    }

    @Test
    @DisplayName("POST /api/v1/products/hold with blank operation key returns 400 Bad Request")
    void hold_blankOperationKey_returns400() throws Exception {
        String requestJson = """
                {
                    "checkoutOperationKey": "   ",
                    "items": [{ "productId": 10, "quantity": 2 }]
                }
                """;

        mockMvc.perform(post("/api/v1/products/hold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(productService);
    }

    @Test
    @DisplayName("POST /api/v1/products/hold with empty items list returns 400 Bad Request")
    void hold_emptyItems_returns400() throws Exception {
        String requestJson = """
                {
                    "checkoutOperationKey": "chk-op-123",
                    "items": []
                }
                """;

        mockMvc.perform(post("/api/v1/products/hold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(productService);
    }

    @Test
    @DisplayName("POST /api/v1/products/hold with nonpositive quantity returns 400 Bad Request")
    void hold_nonPositiveQuantity_returns400() throws Exception {
        String zeroQtyJson = """
                {
                    "checkoutOperationKey": "chk-op-123",
                    "items": [{ "productId": 10, "quantity": 0 }]
                }
                """;
        String negativeQtyJson = """
                {
                    "checkoutOperationKey": "chk-op-123",
                    "items": [{ "productId": 10, "quantity": -5 }]
                }
                """;

        mockMvc.perform(post("/api/v1/products/hold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(zeroQtyJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(post("/api/v1/products/hold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(negativeQtyJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(productService);
    }

    @Test
    @DisplayName("POST /api/v1/products/hold with missing productId returns 400 Bad Request")
    void hold_missingProductId_returns400() throws Exception {
        String requestJson = """
                {
                    "checkoutOperationKey": "chk-op-123",
                    "items": [{ "quantity": 2 }]
                }
                """;

        mockMvc.perform(post("/api/v1/products/hold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(productService);
    }

    @Test
    @DisplayName("POST /api/v1/products/hold when insufficient stock returns 422 Unprocessable Entity")
    void hold_whenInsufficientStock_returns422() throws Exception {
        String requestJson = """
                {
                    "checkoutOperationKey": "chk-op-123",
                    "items": [{ "productId": 10, "quantity": 50 }]
                }
                """;

        when(productService.holdStock(any(HoldStockRequest.class)))
                .thenThrow(new InsufficientStockException("Insufficient stock for product id 10"));

        mockMvc.perform(post("/api/v1/products/hold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value("Insufficient stock for product id 10"));
    }

    @Test
    @DisplayName("POST /api/v1/products/hold when product not found returns 404 Not Found")
    void hold_whenProductNotFound_returns404() throws Exception {
        String requestJson = """
                {
                    "checkoutOperationKey": "chk-op-123",
                    "items": [{ "productId": 999, "quantity": 1 }]
                }
                """;

        when(productService.holdStock(any(HoldStockRequest.class)))
                .thenThrow(new ProductNotFoundException("Product not found with id: 999"));

        mockMvc.perform(post("/api/v1/products/hold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Product not found with id: 999"));
    }

    @Test
    @DisplayName("POST /api/v1/products/hold when product inactive returns 400 Bad Request")
    void hold_whenProductInactive_returns400() throws Exception {
        String requestJson = """
                {
                    "checkoutOperationKey": "chk-op-123",
                    "items": [{ "productId": 5, "quantity": 1 }]
                }
                """;

        when(productService.holdStock(any(HoldStockRequest.class)))
                .thenThrow(new ApiException("Product is not active with id: 5", HttpStatus.BAD_REQUEST));

        mockMvc.perform(post("/api/v1/products/hold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Product is not active with id: 5"));
    }

    @Test
    @DisplayName("POST /api/v1/products/hold when operation key conflicts returns 409 Conflict")
    void hold_whenPayloadConflict_returns409() throws Exception {
        String requestJson = """
                {
                    "checkoutOperationKey": "chk-op-conflict",
                    "items": [{ "productId": 10, "quantity": 5 }]
                }
                """;

        when(productService.holdStock(any(HoldStockRequest.class)))
                .thenThrow(new ApiException("Operation key conflict: payload does not match existing hold for key chk-op-conflict", HttpStatus.CONFLICT));

        mockMvc.perform(post("/api/v1/products/hold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Operation key conflict: payload does not match existing hold for key chk-op-conflict"));
    }

    @Test
    @DisplayName("POST /api/v1/products/release with valid request returns 200 OK")
    void release_validRequest_returns200() throws Exception {
        String requestJson = """
                {
                    "checkoutOperationKey": "chk-op-123",
                    "items": [{ "productId": 10, "quantity": 2 }]
                }
                """;

        when(productService.releaseStock(any(ReleaseStockRequest.class)))
                .thenReturn(new ReleaseStockResponse("RELEASED", 1));

        mockMvc.perform(post("/api/v1/products/release")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RELEASED"))
                .andExpect(jsonPath("$.releasedItemCount").value(1));
    }

    @Test
    @DisplayName("POST /api/v1/products/release with missing operation key returns 400 Bad Request")
    void release_missingOperationKey_returns400() throws Exception {
        String requestJson = """
                {
                    "items": [{ "productId": 10, "quantity": 2 }]
                }
                """;

        mockMvc.perform(post("/api/v1/products/release")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(productService);
    }
}
