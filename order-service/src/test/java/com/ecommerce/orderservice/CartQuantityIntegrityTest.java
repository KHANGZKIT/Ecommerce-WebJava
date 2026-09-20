package com.ecommerce.orderservice;

import com.ecommerce.orderservice.client.ProductClient;
import com.ecommerce.orderservice.config.JacksonConfig;
import com.ecommerce.orderservice.controller.CartController;
import com.ecommerce.orderservice.dto.*;
import com.ecommerce.orderservice.exception.ApiException;
import com.ecommerce.orderservice.exception.CartNotFoundException;
import com.ecommerce.orderservice.exception.GlobalExceptionHandler;
import com.ecommerce.orderservice.repository.CartRedisRepository;
import com.ecommerce.orderservice.security.OrderJwtVerifier;
import com.ecommerce.orderservice.security.OrderSecurityConfig;
import com.ecommerce.orderservice.service.CartService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.math.BigDecimal;
import java.security.Key;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CartQuantityIntegrityTest {

    private Map<Long, CartDto> carts;
    private MockMvc mvc;
    private AnnotationConfigWebApplicationContext context;
    private CartRedisRepository repository;
    private ProductClient products;
    private CartService service;
    private static final Key KEY = Keys.secretKeyFor(SignatureAlgorithm.HS384);

    @Configuration
    @EnableWebMvc
    @Import({OrderSecurityConfig.class, CartController.class, GlobalExceptionHandler.class, JacksonConfig.class})
    static class TestConfig {
        @Bean OrderJwtVerifier verifier() { return new OrderJwtVerifier(Encoders.BASE64.encode(KEY.getEncoded())); }
        @Bean CartRedisRepository repository() { return mock(CartRedisRepository.class); }
        @Bean ProductClient products() { return mock(ProductClient.class); }
        @Bean CartService service(CartRedisRepository repository, ProductClient products) {
            return spy(new CartService(repository, products));
        }
    }

    private static String bearer(long id) {
        String token = Jwts.builder()
                .claim("userId", id)
                .setSubject("user" + id + "@example.com")
                .setExpiration(new Date(System.currentTimeMillis() + 300000))
                .signWith(KEY, SignatureAlgorithm.HS256)
                .compact();
        return "Bearer " + token;
    }

    private CartDto cart(long userId, long itemId, int quantity) {
        var item = CartItemDto.builder()
                .id(itemId)
                .productId(3L)
                .productName("Mouse")
                .productSku("MOUSE")
                .unitPrice(BigDecimal.TEN)
                .quantity(quantity)
                .subtotal(BigDecimal.TEN.multiply(BigDecimal.valueOf(quantity)))
                .build();
        return CartRedisRepository.recalculate(CartDto.builder()
                .userId(userId)
                .items(new ArrayList<>(List.of(item)))
                .build());
    }

    @BeforeEach
    void setUp() {
        carts = new HashMap<>();
        carts.put(1L, cart(1L, 11L, 2));
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(TestConfig.class);
        context.refresh();
        repository = context.getBean(CartRedisRepository.class);
        products = context.getBean(ProductClient.class);
        service = context.getBean(CartService.class);

        when(repository.findByUserId(anyLong())).thenAnswer(call -> Optional.ofNullable(carts.get(call.getArgument(0))));
        doAnswer(call -> {
            CartDto cart = call.getArgument(0);
            carts.put(cart.getUserId(), cart);
            return null;
        }).when(repository).save(any());
        doAnswer(call -> { carts.remove(call.getArgument(0)); return null; }).when(repository).deleteByUserId(anyLong());

        when(products.getProductById(3L)).thenReturn(ProductResponse.builder().id(3L)
                .name("Mouse").sku("MOUSE").price(BigDecimal.TEN).active(true).stockQuantity(100).build());

        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean("springSecurityFilterChain", Filter.class)).build();
    }

    @AfterEach
    void closeContext() {
        if (context != null) context.close();
    }

    // 1. negative quantity: -1 -> 400
    @Test
    void negativeQuantity_returns400() throws Exception {
        mvc.perform(post("/api/v1/cart/items")
                .header("Authorization", bearer(1))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":3,\"quantity\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mvc.perform(patch("/api/v1/cart/items/11")
                .header("Authorization", bearer(1))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // 2. fractional positive quantity: 3.5 -> 400
    @Test
    void fractionalPositiveQuantity_returns400() throws Exception {
        mvc.perform(post("/api/v1/cart/items")
                .header("Authorization", bearer(1))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":3,\"quantity\":3.5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mvc.perform(patch("/api/v1/cart/items/11")
                .header("Authorization", bearer(1))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":3.5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // 3. fractional negative quantity: -0.5 -> 400 AND existing cart item remains unchanged
    @Test
    void fractionalNegativeQuantity_returns400_andItemRemainsUnchanged() throws Exception {
        // Initial item quantity is 2
        assertEquals(2, carts.get(1L).getItems().get(0).getQuantity());

        // Attempting to update with -0.5 must be rejected with 400 and NOT remove or modify the item
        mvc.perform(patch("/api/v1/cart/items/11")
                .header("Authorization", bearer(1))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":-0.5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        // Cart item MUST still be present with quantity 2
        assertNotNull(carts.get(1L));
        assertEquals(1, carts.get(1L).getItems().size());
        assertEquals(2, carts.get(1L).getItems().get(0).getQuantity());
    }

    // 4. quantity above maximum: 10000 -> 400
    @Test
    void quantityAboveMaximum_returns400() throws Exception {
        mvc.perform(post("/api/v1/cart/items")
                .header("Authorization", bearer(1))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":3,\"quantity\":10000}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mvc.perform(patch("/api/v1/cart/items/11")
                .header("Authorization", bearer(1))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":10000}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // 5. integer overflow: total quantity exceeds MAX_QUANTITY or overflows
    @Test
    void integerOverflow_resultingQuantityExceedsMax_returns400() throws Exception {
        // Set item quantity in cart to 9000
        carts.put(1L, cart(1L, 11L, 9000));

        // Adding 2000 would result in 11000 > 9999
        mvc.perform(post("/api/v1/cart/items")
                .header("Authorization", bearer(1))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":3,\"quantity\":2000}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        // Adding large quantity causing 32-bit integer overflow
        carts.put(1L, cart(1L, 11L, Integer.MAX_VALUE - 5));
        AddItemRequest overflowRequest = new AddItemRequest();
        overflowRequest.setProductId(3L);
        overflowRequest.setQuantity(10);

        ApiException ex = assertThrows(ApiException.class, () -> service.addItem(1L, overflowRequest));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("Total item quantity cannot exceed"));
    }

    // 6. missing cart: verify CartNotFoundException is used
    @Test
    void missingCart_throwsCartNotFoundException() throws Exception {
        long nonExistentUserId = 999L;
        UpdateQuantityRequest updateReq = new UpdateQuantityRequest();
        updateReq.setQuantity(5);

        // Service direct verification
        assertThrows(CartNotFoundException.class,
                () -> service.updateItemQuantity(nonExistentUserId, 1L, updateReq));
        assertThrows(CartNotFoundException.class,
                () -> service.removeItem(nonExistentUserId, 1L));

        // HTTP verification: returns 404
        mvc.perform(patch("/api/v1/cart/items/1")
                .header("Authorization", bearer(nonExistentUserId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":5}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // 7. regression test: adding existing product refreshes snapshot and recalculates totals
    @Test
    void addItem_existingProductRefreshesSnapshotAndRecalculatesTotals() throws Exception {
        // Initial cart item has price = 10, name = "Mouse", sku = "MOUSE", qty = 2, subtotal = 20
        assertEquals(new BigDecimal("10"), carts.get(1L).getItems().get(0).getUnitPrice());
        assertEquals("Mouse", carts.get(1L).getItems().get(0).getProductName());

        // Product service updates product 3: price = 150.00, name = "Mouse Updated", sku = "MOUSE-NEW", image = "http://images/mouse-new.png"
        when(products.getProductById(3L)).thenReturn(ProductResponse.builder()
                .id(3L)
                .name("Mouse Updated")
                .sku("MOUSE-NEW")
                .price(new BigDecimal("150.00"))
                .imageUrl("http://images/mouse-new.png")
                .active(true)
                .stockQuantity(50)
                .build());

        // Add 1 more of product 3 (current qty was 2 -> new qty is 3)
        mvc.perform(post("/api/v1/cart/items")
                .header("Authorization", bearer(1))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":3,\"quantity\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(3))
                .andExpect(jsonPath("$.items[0].unitPrice").value(150.00))
                .andExpect(jsonPath("$.items[0].productName").value("Mouse Updated"))
                .andExpect(jsonPath("$.items[0].productSku").value("MOUSE-NEW"))
                .andExpect(jsonPath("$.items[0].imageUrl").value("http://images/mouse-new.png"))
                .andExpect(jsonPath("$.items[0].subtotal").value(450.00))
                .andExpect(jsonPath("$.totalPrice").value(450.00));

        // Verify storage reflects the updated snapshot
        CartItemDto savedItem = carts.get(1L).getItems().get(0);
        assertEquals(3, savedItem.getQuantity());
        assertEquals(new BigDecimal("150.00"), savedItem.getUnitPrice());
        assertEquals("Mouse Updated", savedItem.getProductName());
        assertEquals("MOUSE-NEW", savedItem.getProductSku());
        assertEquals("http://images/mouse-new.png", savedItem.getImageUrl());
        assertEquals(new BigDecimal("450.00"), savedItem.getSubtotal());
        assertEquals(new BigDecimal("450.00"), carts.get(1L).getTotalPrice());
    }
}
