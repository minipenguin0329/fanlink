
package com.fanlink.domain.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

@Component
public class TossPaymentClient {

    private static final String CONFIRM_URL =
            "https://api.tosspayments.com/v1/payments/confirm";

    private final ObjectMapper objectMapper;
    private final String secretKey;
    private final HttpClient httpClient;

    public TossPaymentClient(
            ObjectMapper objectMapper,
            @Value("${TOSS_TEST_SECRET_KEY:}") String secretKey
    ) {
        this.objectMapper = objectMapper;
        this.secretKey = secretKey;

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public TossPaymentResult confirm(
            String paymentKey,
            String orderId,
            int amount
    ) {
        // 테스트 키만 허용
        if (secretKey == null
                || !secretKey.startsWith("test_sk")) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "토스페이먼츠 테스트 시크릿 키가 필요합니다."
            );
        }

        String basicAuth = Base64.getEncoder()
                .encodeToString(
                        (secretKey + ":")
                                .getBytes(StandardCharsets.UTF_8)
                );

        try {
            String requestJson =
                    objectMapper.writeValueAsString(
                            Map.of(
                                    "paymentKey", paymentKey,
                                    "orderId", orderId,
                                    "amount", amount
                            )
                    );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CONFIRM_URL))
                    .timeout(Duration.ofSeconds(12))
                    .header(
                            "Authorization",
                            "Basic " + basicAuth
                    )
                    .header(
                            "Content-Type",
                            "application/json"
                    )
                    .header(
                            "Idempotency-Key",
                            "confirm_" + orderId
                    )
                    .POST(
                            HttpRequest.BodyPublishers.ofString(
                                    requestJson,
                                    StandardCharsets.UTF_8
                            )
                    )
                    .build();

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString(
                                    StandardCharsets.UTF_8
                            )
                    );

            if (response.statusCode() != 200) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "결제 승인에 실패했거나 결과 확인이 필요합니다."
                );
            }

            JsonNode result =
                    objectMapper.readTree(response.body());

            boolean verified =
                    "DONE".equals(
                            result.path("status").asText()
                    )
                            && paymentKey.equals(
                            result.path("paymentKey").asText()
                    )
                            && orderId.equals(
                            result.path("orderId").asText()
                    )
                            && amount ==
                            result.path("totalAmount").asInt(-1);

            if (!verified) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "PG 결제 응답이 주문 정보와 일치하지 않습니다."
                );
            }

            // An ordinary successful charge is NOT escrow protection.
            boolean useEscrow = result.path("useEscrow").asBoolean(false);
            String escrowKey = result.path("escrowKey").asText("");
            return new TossPaymentResult(useEscrow, escrowKey);

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "결제 승인 결과를 확인하지 못했습니다.",
                    ex
            );

        } catch (IOException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "결제 서버 통신 결과를 확인하지 못했습니다.",
                    ex
            );
        }
    }
}
