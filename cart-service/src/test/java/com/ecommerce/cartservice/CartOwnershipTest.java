package com.ecommerce.cartservice;

import com.ecommerce.cartservice.client.ProductClient;
import com.ecommerce.cartservice.controller.CartController;
import com.ecommerce.cartservice.dto.*;
import com.ecommerce.cartservice.exception.GlobalExceptionHandler;
import com.ecommerce.cartservice.repository.CartRedisRepository;
import com.ecommerce.cartservice.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import com.ecommerce.cartservice.security.*;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.io.Encoders;
import org.springframework.context.annotation.*;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import jakarta.servlet.Filter;
import java.security.Key;
import java.util.Date;
import java.util.stream.Stream;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Exercises the production security chain, controller and service with isolated storage. */
class CartOwnershipTest {
    private Map<Long, CartDto> carts;
    private MockMvc mvc;
    private AnnotationConfigWebApplicationContext context;
    private CartRedisRepository repository;
    private ProductClient products;
    private CartService service;
    private static final Key KEY = Keys.secretKeyFor(SignatureAlgorithm.HS384);

    @Configuration
    @EnableWebMvc
    @Import({CartSecurityConfig.class, CartController.class, GlobalExceptionHandler.class})
    static class TestConfig {
        @Bean CartJwtVerifier verifier() { return new CartJwtVerifier(Encoders.BASE64.encode(KEY.getEncoded())); }
        @Bean CartRedisRepository repository() { return mock(CartRedisRepository.class); }
        @Bean ProductClient products() { return mock(ProductClient.class); }
        @Bean CartService service(CartRedisRepository repository, ProductClient products) {
            return spy(new CartService(repository, products));
        }
    }

    @AfterEach
    void closeContext() { if (context != null) context.close(); }

    private static String token(Object id, String subject, Date expiration, Key key, SignatureAlgorithm algorithm) {
        return Jwts.builder().claim("userId", id).setSubject(subject).setExpiration(expiration)
                .signWith(key, algorithm).compact();
    }

    private static String bearer(long id) {
        return "Bearer " + token(id, "user" + id + "@example.com", new Date(System.currentTimeMillis() + 300000), KEY, SignatureAlgorithm.HS256);
    }

    @BeforeEach
    void setUp() {
        carts = new HashMap<>();
        carts.put(1L, cart(1L, 11L));
        carts.put(2L, cart(2L, 22L));
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
                .name("Mouse").sku("MOUSE").price(BigDecimal.TEN).active(true).stockQuantity(10).build());
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean("springSecurityFilterChain", Filter.class)).build();
        clearInvocations(repository, products, service);
    }

    private CartDto cart(long userId, long itemId) {
        var item = CartItemDto.builder().id(itemId).productId(3L).productName("Mouse")
                .productSku("MOUSE").unitPrice(BigDecimal.TEN).quantity(1).subtotal(BigDecimal.TEN).build();
        return CartRedisRepository.recalculate(CartDto.builder().userId(userId)
                .items(new ArrayList<>(java.util.List.of(item))).build());
    }

    @Test
    void readsOnlyJwtSelectedCartDespiteOtherIdentityInputs() throws Exception {
        mvc.perform(get("/api/v1/cart").header("Authorization", bearer(1)).header("X-User-Id", "2")
                .header("X-User-Email", "b@example.com").param("userId", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.items[0].id").value(11));
    }

    @Test
    void addUpdatesOnlySelectedCart() throws Exception {
        mvc.perform(post("/api/v1/cart/items").header("Authorization", bearer(1)).header("X-User-Id", "2")
                .contentType(MediaType.APPLICATION_JSON).content("{\"productId\":3,\"quantity\":2}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(1));
        assertEquals(3, carts.get(1L).getItems().get(0).getQuantity());
        assertEquals(1, carts.get(2L).getItems().get(0).getQuantity());
    }

    @Test
    void cannotUpdateOrRemoveAnotherCartsItem() throws Exception {
        mvc.perform(patch("/api/v1/cart/items/22").header("Authorization", bearer(1)).header("X-User-Id", "2")
                .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":9}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/cart/items/22").header("Authorization", bearer(1)).header("X-User-Id", "2"))
                .andExpect(status().isNotFound());
        assertEquals(1, carts.get(2L).getItems().get(0).getQuantity());
        assertEquals(22L, carts.get(2L).getItems().get(0).getId());
    }

    @Test
    void ownItemUpdateAndRemovalStillWork() throws Exception {
        mvc.perform(patch("/api/v1/cart/items/11").header("Authorization", bearer(1)).header("X-User-Id", "2")
                .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":2}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].quantity").value(2));
        mvc.perform(delete("/api/v1/cart/items/11").header("Authorization", bearer(1)).header("X-User-Id", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.itemCount").value(0));
        assertEquals(1, carts.get(2L).getItems().size());
    }

    @Test
    void clearDeletesOnlySelectedCart() throws Exception {
        mvc.perform(delete("/api/v1/cart").header("Authorization", bearer(1)).header("X-User-Id", "2").param("userId", "2"))
                .andExpect(status().isNoContent());
        assertFalse(carts.containsKey(1L));
        assertTrue(carts.containsKey(2L));
    }

    @Test
    void directHeaderOnlyAccessIsRejectedBeforeBusinessWork() throws Exception {
        mvc.perform(get("/api/v1/cart").header("X-User-Id", "2")
                .header("X-User-Email", "b@example.com").header("X-Auth-Token", bearer(2).substring(7)))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
        verifyNoInteractions(service, repository, products);
    }

    static Stream<String> invalidAuthorization() {
        Date future = new Date(System.currentTimeMillis() + 300000);
        var tokens = new ArrayList<String>();
        tokens.add("Basic abc");
        tokens.add("Bearer ");
        tokens.add("Bearer nonsense");
        tokens.add(bearer(1) + " ");
        tokens.add("Bearer  " + bearer(1).substring(7));
        tokens.add(bearer(1) + ", " + bearer(2));
        tokens.add("Bearer " + token(1, "a", new Date(System.currentTimeMillis() - 60000), KEY, SignatureAlgorithm.HS256));
        tokens.add("Bearer " + token(1, "a", future, Keys.secretKeyFor(SignatureAlgorithm.HS256), SignatureAlgorithm.HS256));
        // Same key can verify HS384: rejection must enforce the algorithm, not just signature strength.
        tokens.add("Bearer " + token(1, "a", future, KEY, SignatureAlgorithm.HS384));
        tokens.add("Bearer " + Jwts.builder().setSubject("a").claim("userId", 1).setExpiration(future).compact());
        for (Object id : new Object[]{null, 0, -1, 1.5, "1", true, new java.math.BigInteger("9223372036854775808")}) {
            tokens.add("Bearer " + token(id, "a", future, KEY, SignatureAlgorithm.HS256));
        }
        for (String subject : new String[]{null, "", "   "}) {
            tokens.add("Bearer " + token(1, subject, future, KEY, SignatureAlgorithm.HS256));
        }
        tokens.add("Bearer " + token(1, "a", null, KEY, SignatureAlgorithm.HS256));
        return tokens.stream();
    }

    @ParameterizedTest
    @MethodSource("invalidAuthorization")
    void invalidAuthenticationStopsBeforeBusinessWork(String authorization) throws Exception {
        mvc.perform(get("/api/v1/cart").header("Authorization", authorization))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401))
                .andExpect(header().string("WWW-Authenticate", "Bearer"));
        verifyNoInteractions(service, repository, products);
    }

    @Test
    void multipleAuthorizationValuesAreRejected() throws Exception {
        mvc.perform(get("/api/v1/cart").header("Authorization", bearer(1), bearer(2)))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service, repository, products);
    }

    @Test
    void legacyTokenCannotOverrideBearerAndIdentityDoesNotLeakAcrossRequests() throws Exception {
        mvc.perform(get("/api/v1/cart").header("Authorization", bearer(1)).header("X-Auth-Token", bearer(2).substring(7)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(1))
                .andExpect(cookie().doesNotExist("JSESSIONID"));
        clearInvocations(service, repository, products);
        mvc.perform(get("/api/v1/cart")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service, repository, products);
    }

    @Test
    void everyBusinessMethodRequiresBearer() throws Exception {
        for (var request : java.util.List.of(get("/api/v1/cart"), post("/api/v1/cart/items"),
                patch("/api/v1/cart/items/11"), delete("/api/v1/cart/items/11"), delete("/api/v1/cart"))) {
            mvc.perform(request).andExpect(status().isUnauthorized());
        }
        verifyNoInteractions(service, repository, products);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/actuator/health", "/actuator/health/readiness", "/v3/api-docs", "/v3/api-docs/swagger-config",
            "/v3/api-docs.yaml", "/swagger-ui.html", "/swagger-ui/index.html"})
    void explicitPublicGetPathsPassSecurity(String path) throws Exception {
        // MVC-only context has no actuator/springdoc handlers: 404 proves security allowed dispatch.
        mvc.perform(get(path)).andExpect(status().isNotFound());
        mvc.perform(post(path)).andExpect(status().isUnauthorized());
        verifyNoInteractions(service, repository, products);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/actuator/info", "/actuator/env", "/other", "/api/v1/cart/anything"})
    void unlistedPathsAreNotPublic(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        verifyNoInteractions(service, repository, products);
    }

    @Test
    void unsupportedAlgorithmRejectedEvenWithMatchingKey() {
        Key key = Keys.secretKeyFor(SignatureAlgorithm.HS384);
        CartJwtVerifier verifier = new CartJwtVerifier(Encoders.BASE64.encode(key.getEncoded()));
        assertThrows(IllegalArgumentException.class, () -> verifier.verify(token(1, "a",
                new Date(System.currentTimeMillis() + 300000), key, SignatureAlgorithm.HS384)));
    }
}