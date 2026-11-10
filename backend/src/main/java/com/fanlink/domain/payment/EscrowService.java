package com.fanlink.domain.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fanlink.domain.trade.Trade;
import com.fanlink.domain.trade.TradeStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class EscrowService {
    private final PaymentOrderRepository orders;
    private final PaymentTradeLockRepository tradeLocks;
    private final TossEscrowClient toss;

    private PaymentOrder requireOrder(Long tradeId, Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        PaymentOrder order = orders.findByTrade_Id(tradeId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "결제 주문이 없습니다."));
        Trade trade = order.getTrade();
        if (!userId.equals(trade.getBuyer().getId())
                && !userId.equals(trade.getPost().getAuthor().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "거래 당사자만 조회할 수 있습니다.");
        }
        if (order.getStatus() != PaymentOrderStatus.APPROVED
                || order.getEscrowKey() == null || order.getEscrowKey().isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "PG 에스크로 보호가 확인되지 않은 주문입니다.");
        }
        return order;
    }

    private EscrowStatusResponse from(Long tradeId, JsonNode escrow) {
        String status = escrow.path("status").asText("");
        if (!java.util.Set.of("IN_PROGRESS", "CANCEL_REQUESTED", "CONFIRMED", "CANCELED")
                .contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "알 수 없는 PG 에스크로 상태입니다.");
        }
        return new EscrowStatusResponse(tradeId, status,
                escrow.path("confirmType").asText(""),
                escrow.path("cancelType").asText(""),
                escrow.path("products").size(),
                escrow.path("deliveryIds").size());
    }

    @Transactional(readOnly = true)
    public EscrowStatusResponse get(Long tradeId, Long userId) {
        PaymentOrder order = requireOrder(tradeId, userId);
        return from(tradeId, toss.fetch(order.getEscrowKey(), order.getPaymentKey()));
    }

    /** Only buyer can attest to receiving the ticket, after local receipt confirmation. */
    @Transactional
    public EscrowStatusResponse registerReceived(Long tradeId, Long buyerId,
                                                  String receiverName) {
        if (receiverName == null || receiverName.isBlank() || receiverName.length() > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "실제 수령인의 이름이 필요합니다.");
        }
        Trade trade = tradeLocks.findByIdForUpdate(tradeId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "거래가 없습니다."));
        if (buyerId == null || !buyerId.equals(trade.getBuyer().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "구매자만 직접 수령을 등록할 수 있습니다.");
        }
        if (trade.getStatus() != TradeStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "구매자 수령확정 후에만 PG 배송 등록을 할 수 있습니다.");
        }
        PaymentOrder order = requireOrder(tradeId, buyerId);
        JsonNode escrow = toss.fetch(order.getEscrowKey(), order.getPaymentKey());
        EscrowStatusResponse current = from(tradeId, escrow);
        if (current.registeredDeliveryCount() > 0) return current; // avoids duplicate POST
        if (!"IN_PROGRESS".equals(current.status()) || current.productCount() != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "현재 배송을 등록할 수 없는 에스크로 상태입니다.");
        }
        String productId = escrow.path("products").path(0).path("id").asText("");
        if (productId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "PG 상품 ID를 확인하지 못했습니다.");
        }
        // External POST can succeed even if the local response times out.
        // On retry, check PG deliveryIds before trying POST again.
        toss.registerDirectDelivery(productId, receiverName.trim());
        return from(tradeId, toss.fetch(order.getEscrowKey(), order.getPaymentKey()));
    }
}
