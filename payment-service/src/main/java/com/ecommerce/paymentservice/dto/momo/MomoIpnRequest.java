package com.ecommerce.paymentservice.dto.momo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MomoIpnRequest {
    @NotNull(message = "partnerCode is required")
    private String partnerCode;

    @NotNull(message = "orderId is required")
    private String orderId;

    @NotNull(message = "requestId is required")
    private String requestId;

    @NotNull(message = "amount is required")
    private Long amount;

    private String orderInfo;
    private String orderType;

    private Long transId;

    @NotNull(message = "resultCode is required")
    private Integer resultCode;

    private String message;
    private String payType;
    private Long responseTime;
    private String extraData;

    @NotNull(message = "signature is required")
    private String signature;
}
