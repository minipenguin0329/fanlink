package com.fanlink.domain.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Map;

@Component
public class TossEscrowClient {
    private static final String BASE = "https://api.tosspayments.com";
    private final String secretKey;
    private final ObjectMapper mapper;
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    public TossEscrowClient(ObjectMapper mapper,
                            @Value("${TOSS_TEST_SECRET_KEY:}") String secretKey) {
        this.mapper = mapper;
        this.secretKey = secretKey;
    }

    private String auth() {
        if (secretKey == null || !secretKey.startsWith("test_sk")) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "에스크로 테스트 시크릿 키가 필요합니다.");
        }
        return "Basic " + Base64.getEncoder().encodeToString(
                (secretKey + ":").getBytes(StandardCharsets.UTF_8));
    }

    private JsonNode execute(String method, String url, Object data) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(12))
                    .header("Authorization", auth())
                    .header("Content-Type", "application/json");
            if ("POST".equals(method)) {
                builder.POST(HttpRequest.BodyPublishers.ofString(
                        mapper.writeValueAsString(data), StandardCharsets.UTF_8));
            } else {
                builder.GET();
            }
            HttpResponse<String> response = client.send(builder.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "PG 에스크로 요청에 실패했습니다. PG 거래 상태 확인이 필요합니다.");
            }
            return mapper.readTree(response.body());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "PG 요청이 중단되었습니다.", ex);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "PG 응답을 확인하지 못했습니다.", ex);
        }
    }

    public JsonNode fetch(String escrowKey, String paymentKey) {
        if (escrowKey == null || escrowKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "승인된 에스크로 거래가 없습니다.");
        }
        String encoded = URLEncoder.encode(escrowKey, StandardCharsets.UTF_8);
        JsonNode root = execute("GET", BASE + "/v2/escrows/" + encoded, null);
        JsonNode escrow = root.hasNonNull("entityBody") ? root.path("entityBody") : root;
        if (!escrowKey.equals(escrow.path("id").asText())
                || !paymentKey.equals(escrow.path("targetId").asText())
                || !"PAYMENT".equals(escrow.path("targetType").asText())) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "PG 에스크로 응답이 결제정보와 일치하지 않습니다.");
        }
        return escrow;
    }

    /** Only use DIRECT after the buyer attests actual receipt. */
    public JsonNode registerDirectDelivery(String productId, String receiverName) {
        String timestamp = OffsetDateTime.now().toString();
        Map<String,Object> body = Map.of(
                "targetType", "ESCROW_PRODUCT", "targetId", productId,
                "deliveryMethod", "DIRECT",
                "direct", Map.of("receivedAt", timestamp,
                        "receiverName", receiverName,
                        "receiverRelation", "BUYER"));
        return execute("POST", BASE + "/v2/deliveries", body);
    }
}
