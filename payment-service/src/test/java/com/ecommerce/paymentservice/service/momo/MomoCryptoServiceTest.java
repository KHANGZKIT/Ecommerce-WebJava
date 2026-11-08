package com.ecommerce.paymentservice.service.momo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MomoCryptoServiceTest {

    private MomoCryptoService cryptoService;

    @BeforeEach
    void setUp() {
        cryptoService = new MomoCryptoService();
    }

    @Test
    @DisplayName("Should correctly generate HMAC-SHA256 hex signature")
    void shouldGenerateCorrectHmacSha256() {
        String data = "accessKey=F8BBA842ECF85&amount=50000&extraData=&ipnUrl=http://localhost:8080/api/v1/payments/momo/ipn&orderId=TG_101&orderInfo=Test&partnerCode=MOMO&redirectUrl=http://localhost:3000/payment-result&requestId=req-1&requestType=captureWallet";
        String secretKey = "K951B6PE1waDMi640xX0qPDp5Jm0nhBR";

        String signature = cryptoService.signHmacSha256(data, secretKey);

        assertThat(signature).isNotNull();
        assertThat(signature).matches("^[a-f0-9]{64}$");
    }

    @Test
    @DisplayName("Should build create payment canonical raw signature string with correct alphabetical key order")
    void shouldBuildCreatePaymentRawSignature() {
        String raw = cryptoService.buildCreatePaymentRawSignature(
                "F8BBA842ECF85",
                100000L,
                "orderId=101",
                "http://localhost:8080/api/v1/payments/momo/ipn",
                "TG_ORD_101",
                "Thanh toán đơn hàng #101",
                "MOMO",
                "http://localhost:3000/payment-result?orderId=101",
                "req-uuid-1",
                "captureWallet"
        );

        String expected = "accessKey=F8BBA842ECF85&amount=100000&extraData=orderId=101&ipnUrl=http://localhost:8080/api/v1/payments/momo/ipn&orderId=TG_ORD_101&orderInfo=Thanh toán đơn hàng #101&partnerCode=MOMO&redirectUrl=http://localhost:3000/payment-result?orderId=101&requestId=req-uuid-1&requestType=captureWallet";
        assertThat(raw).isEqualTo(expected);
    }

    @Test
    @DisplayName("Should build IPN canonical raw signature string with correct alphabetical key order")
    void shouldBuildIpnRawSignature() {
        String raw = cryptoService.buildIpnRawSignature(
                "F8BBA842ECF85",
                100000L,
                "orderId=101",
                "Successful.",
                "TG_ORD_101",
                "Thanh toán",
                "momo_wallet",
                "MOMO",
                "qr",
                "req-uuid-1",
                1713456789000L,
                0,
                2345678901L
        );

        String expected = "accessKey=F8BBA842ECF85&amount=100000&extraData=orderId=101&message=Successful.&orderId=TG_ORD_101&orderInfo=Thanh toán&orderType=momo_wallet&partnerCode=MOMO&payType=qr&requestId=req-uuid-1&responseTime=1713456789000&resultCode=0&transId=2345678901";
        assertThat(raw).isEqualTo(expected);
    }

    @Test
    @DisplayName("Should verify signature in constant time: true for valid, false for invalid")
    void shouldVerifySignature() {
        String data = "test-raw-data-string";
        String secretKey = "K951B6PE1waDMi640xX0qPDp5Jm0nhBR";
        String validSignature = cryptoService.signHmacSha256(data, secretKey);

        assertThat(cryptoService.verifySignature(data, secretKey, validSignature)).isTrue();
        assertThat(cryptoService.verifySignature(data, secretKey, validSignature.toUpperCase())).isTrue();
        assertThat(cryptoService.verifySignature(data, secretKey, "invalid-signature-hex-1234567890")).isFalse();
        assertThat(cryptoService.verifySignature(data, "wrongSecretKey", validSignature)).isFalse();
        assertThat(cryptoService.verifySignature(null, secretKey, validSignature)).isFalse();
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when signing with null data or key")
    void shouldThrowWhenNullInput() {
        assertThatThrownBy(() -> cryptoService.signHmacSha256(null, "key"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> cryptoService.signHmacSha256("data", null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
