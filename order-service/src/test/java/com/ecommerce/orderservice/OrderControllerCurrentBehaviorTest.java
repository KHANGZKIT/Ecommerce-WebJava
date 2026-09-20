package com.ecommerce.orderservice;

import com.ecommerce.orderservice.controller.OrderController;
import com.ecommerce.orderservice.dto.CreateOrderRequest;
import com.ecommerce.orderservice.dto.OrderDto;
import com.ecommerce.orderservice.exception.GlobalExceptionHandler;
import com.ecommerce.orderservice.exception.OrderNotFoundException;
import com.ecommerce.orderservice.exception.OrderStatusException;
import com.ecommerce.orderservice.model.OrderStatus;
import com.ecommerce.orderservice.model.PaymentMethod;
import com.ecommerce.orderservice.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrderControllerCurrentBehaviorTest {

    @Mock
    private OrderService orderService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new OrderController(orderService))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createOrder_acceptsUserIdFromRequestBodyWithoutSecurityContext() throws Exception {
        String requestJson = """
                {
                    "userId": 999,
                    "shippingAddress": "123 Main Street, Suite 400, Tech Park, 500081",
                    "paymentMethod": "CREDIT_CARD",
                    "items": [
                        {
                            "productId": 10,
                            "quantity": 2
                        }
                    ],
                    "notes": "Direct client userId test"
                }
                """;

        OrderDto responseDto = OrderDto.builder()
                .id(1L)
                .userId(999L)
                .status(OrderStatus.CONFIRMED)
                .totalAmount(new BigDecimal("100.00"))
                .shippingAddress("123 Main Street, Suite 400, Tech Park, 500081")
                .build();

        when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(responseDto);

        // Documenting current behavior: userId is taken straight from JSON body without JWT or auth validation
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(999))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        verify(orderService).createOrder(any(CreateOrderRequest.class));
    }

    @Test
    void createOrder_whenValidationFails_returns400WithDetailedFieldErrors() throws Exception {
        // Missing userId, blank shippingAddress, missing paymentMethod, empty items
        String invalidJson = """
                {
                    "shippingAddress": "",
                    "items": []
                }
                """;

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.userId").exists())
                .andExpect(jsonPath("$.errors.shippingAddress").exists())
                .andExpect(jsonPath("$.errors.paymentMethod").exists())
                .andExpect(jsonPath("$.errors.items").exists());
    }

    @Test
    void getOrdersByUser_allowsQueryingAnyUserIdViaPathVariable() throws Exception {
        OrderDto dto = OrderDto.builder().id(20L).userId(888L).status(OrderStatus.CONFIRMED).build();
        PageRequest pageRequest = PageRequest.of(0, 10);
        when(orderService.getOrdersByUserId(eq(888L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dto), pageRequest, 1));

        // Documenting current behavior: any caller can query any userId's orders
        mockMvc.perform(get("/api/v1/orders/user/888"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(20))
                .andExpect(jsonPath("$.content[0].userId").value(888));
    }

    @Test
    void getOrderById_whenFound_returns200() throws Exception {
        OrderDto dto = OrderDto.builder().id(15L).userId(101L).status(OrderStatus.CONFIRMED).build();
        when(orderService.getOrderById(15L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/orders/15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(15))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void getOrderById_whenNotFound_returns404() throws Exception {
        when(orderService.getOrderById(999L)).thenThrow(new OrderNotFoundException("Order not found: 999"));

        mockMvc.perform(get("/api/v1/orders/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Order not found: 999"));
    }

    @Test
    void cancelOrder_returnsOkWithCancelledOrderDto() throws Exception {
        OrderDto dto = OrderDto.builder().id(30L).status(OrderStatus.CANCELLED).build();
        when(orderService.cancelOrder(30L)).thenReturn(dto);

        mockMvc.perform(post("/api/v1/orders/30/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(30))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void cancelOrder_whenStatusInvalid_returns422UnprocessableEntity() throws Exception {
        when(orderService.cancelOrder(31L))
                .thenThrow(new OrderStatusException("Cannot cancel order in status: DELIVERED"));

        mockMvc.perform(post("/api/v1/orders/31/cancel"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value("Cannot cancel order in status: DELIVERED"));
    }

    @Test
    void updateStatus_acceptsStatusRequestParamAndReturnsOk() throws Exception {
        OrderDto dto = OrderDto.builder().id(40L).status(OrderStatus.SHIPPED).build();
        when(orderService.updateOrderStatus(40L, OrderStatus.SHIPPED)).thenReturn(dto);

        mockMvc.perform(patch("/api/v1/orders/40/status").param("status", "SHIPPED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(40))
                .andExpect(jsonPath("$.status").value("SHIPPED"));
    }
}
