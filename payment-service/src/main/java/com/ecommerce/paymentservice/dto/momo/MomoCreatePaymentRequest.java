package com.ecommerce.paymentservice.dto.momo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MomoCreatePaymentRequest {
    private String partnerCode;
    private String requestType;
    private String ipnUrl;
    private String redirectUrl;
    private String orderId;
    private Long amount;
    private String orderInfo;
    private String requestId;
    private String extraData;
    @Builder.Default
    private Boolean autoCapture = true;
    @Builder.Default
    private String lang = "vi";
    private String signature;
}
