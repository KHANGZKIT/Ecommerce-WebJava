package com.ecommerce.paymentservice.client;

import com.ecommerce.paymentservice.config.MomoProperties;
import com.ecommerce.paymentservice.dto.momo.MomoCreatePaymentRequest;
import com.ecommerce.paymentservice.dto.momo.MomoCreatePaymentResponse;
import com.ecommerce.paymentservice.dto.momo.MomoQueryRequest;
import com.ecommerce.paymentservice.dto.momo.MomoQueryResponse;
import com.ecommerce.paymentservice.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Slf4j
@Component
public class MomoGatewayClient {

    private final RestClient restClient;
    private final MomoProperties momoProperties;

    public MomoGatewayClient(MomoProperties momoProperties) {
        this.momoProperties = momoProperties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) Duration.ofSeconds(6).toMillis());
        requestFactory.setReadTimeout((int) Duration.ofSeconds(6).toMillis());

        this.restClient = RestClient.builder()
                .baseUrl(momoProperties.getEndpoint())
                .requestFactory(requestFactory)
                .build();
    }

    public MomoCreatePaymentResponse createPayment(MomoCreatePaymentRequest request) {
        log.info("Dispatching create payment request to MoMo gateway for orderId: {}", request.getOrderId());
        try {
            MomoCreatePaymentResponse response = restClient.post()
                    .uri("/v2/gateway/api/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(MomoCreatePaymentResponse.class);

            if (response == null) {
                log.error("Received null response from MoMo create payment API for orderId: {}", request.getOrderId());
                throw new ApiException("Received null response from MoMo gateway", HttpStatus.BAD_GATEWAY);
            }
            log.info("MoMo create response for orderId: {}, resultCode: {}, message: {}",
                    request.getOrderId(), response.getResultCode(), response.getMessage());
            return response;
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error communicating with MoMo gateway for orderId {}: {}", request.getOrderId(), ex.getMessage(), ex);
            throw new ApiException("Failed to communicate with MoMo gateway: " + ex.getMessage(), HttpStatus.BAD_GATEWAY);
        }
    }

    public MomoQueryResponse queryPayment(MomoQueryRequest request) {
        log.info("Querying MoMo transaction status for orderId: {}", request.getOrderId());
        try {
            MomoQueryResponse response = restClient.post()
                    .uri("/v2/gateway/api/query")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(MomoQueryResponse.class);

            if (response == null) {
                throw new ApiException("Received null response from MoMo query API", HttpStatus.BAD_GATEWAY);
            }
            log.info("MoMo query response for orderId: {}, resultCode: {}", request.getOrderId(), response.getResultCode());
            return response;
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error querying MoMo gateway for orderId {}: {}", request.getOrderId(), ex.getMessage(), ex);
            throw new ApiException("Failed to query MoMo gateway: " + ex.getMessage(), HttpStatus.BAD_GATEWAY);
        }
    }
}
