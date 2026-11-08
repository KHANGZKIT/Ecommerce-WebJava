package com.ecommerce.paymentservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "momo")
public class MomoProperties {

    /** MoMo Partner Code provided in developer dashboard / sandbox */
    private String partnerCode = "MOMO";

    /** MoMo Access Key */
    private String accessKey = "F8BBA842ECF85";

    /** MoMo Secret Key used for HMAC-SHA256 signature generation and verification */
    private String secretKey = "K951B6PE1waDMi640xX08PD3vg6EkVlz";

    /** MoMo API Endpoint (Sandbox: https://test-payment.momo.vn) */
    private String endpoint = "https://test-payment.momo.vn";

    /** Frontend redirect URL where MoMo sends the customer after payment interaction */
    private String redirectUrl = "http://localhost:3000/payment-result";

    /** Server-to-server Instant Payment Notification (IPN) webhook URL */
    private String ipnUrl = "http://localhost:8080/api/v1/payments/momo/ipn";

    /** MoMo request type: captureWallet for all-in-one wallet payment */
    private String requestType = "captureWallet";
}
