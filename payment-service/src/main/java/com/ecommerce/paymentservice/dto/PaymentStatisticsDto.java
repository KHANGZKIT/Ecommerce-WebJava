package com.ecommerce.paymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatisticsDto {
    private BigDecimal totalVerifiedRevenue;
    private long verifiedSuccessCount;
    private long failedCount;
    private long pendingCount;
    private long totalCount;
}
