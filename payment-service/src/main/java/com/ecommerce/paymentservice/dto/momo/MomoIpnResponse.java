package com.ecommerce.paymentservice.dto.momo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MomoIpnResponse {
    @Builder.Default
    private Integer resultCode = 0;
    @Builder.Default
    private String message = "Success";
}
