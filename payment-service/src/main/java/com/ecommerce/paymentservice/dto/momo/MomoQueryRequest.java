package com.ecommerce.paymentservice.dto.momo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MomoQueryRequest {
    private String partnerCode;
    private String orderId;
    private String requestId;
    @Builder.Default
    private String lang = "vi";
    private String signature;
}
