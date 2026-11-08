package com.ecommerce.paymentservice.dto.momo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MomoQueryResponse {
    private String partnerCode;
    private String orderId;
    private String requestId;
    private String extraData;
    private Long amount;
    private Long transId;
    private String payType;
    private Integer resultCode;
    private String message;
    private Long responseTime;
}
