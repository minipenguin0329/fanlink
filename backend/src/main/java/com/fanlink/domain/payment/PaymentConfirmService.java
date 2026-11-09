
package com.fanlink.domain.payment;

import com.fanlink.domain.trade.Trade;
import com.fanlink.domain.trade.TradeStatus;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PaymentConfirmService {

    private final PaymentOrderRepository orderRepository;
    private final PaymentTradeLockRepository tradeLockRepository;
    private final TossPaymentClient tossPaymentClient;

    @Transactional
    public PaymentOrderResponse confirm(
            PaymentConfirmRequest request,
            Long buyerId
    ) {
        // 1. 인증 확인
        if (buyerId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
        }

        // 2. 요청 형식 검사
        if (request == null
                || request.orderId() == null
                || request.orderId().isBlank()
                || request.paymentKey() == null
                || request.paymentKey().isBlank()
                || request.paymentKey().length() > 200
                || request.amount() <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "결제 승인 요청값이 올바르지 않습니다."
            );
        }

        // 3. 서버 DB에 저장한 주문 조회
        PaymentOrder initialOrder = orderRepository
                .findByOrderId(request.orderId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "결제 주문을 찾을 수 없습니다."
                ));

        // 4. 거래 잠금
        Trade trade = tradeLockRepository
                .findByIdForUpdate(
                        initialOrder.getTrade().getId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "거래를 찾을 수 없습니다."
                ));

        // 5. 결제 주문 잠금
        PaymentOrder order = orderRepository
                .findByOrderIdForUpdate(request.orderId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "결제 주문을 찾을 수 없습니다."
                ));

        // 6. 구매자 본인 확인
        if (!trade.getBuyer().getId().equals(buyerId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "구매자만 결제를 승인할 수 있습니다."
            );
        }

        // 7. 서버 주문번호 및 금액 검증
        if (!order.getOrderId().equals(request.orderId())
                || order.getAmount() != request.amount()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "주문번호 또는 결제 금액이 일치하지 않습니다."
            );
        }

        // 8. 이미 성공한 같은 결제 요청은 결과 반환
        if (order.getStatus() == PaymentOrderStatus.APPROVED) {
            if (request.paymentKey().equals(order.getPaymentKey())
                    && trade.getStatus() == TradeStatus.PAID) {
                return PaymentOrderResponse.from(order);
            }

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 다른 결제로 처리된 주문입니다."
            );
        }

        // 9. 결제 가능한 상태인지 확인
        if (order.getStatus() != PaymentOrderStatus.READY
                || trade.getStatus() != TradeStatus.ACCEPTED) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "현재 결제할 수 없는 거래입니다."
            );
        }

        // 10. 토스페이먼츠 실제 승인 API 호출
        // 이 메서드는 PG 응답의 DONE, 금액, 주문번호,
        // 결제 식별자까지 검증한 경우에만 정상 반환한다.
        tossPaymentClient.confirm(
                request.paymentKey(),
                order.getOrderId(),
                order.getAmount()
        );

        // 11. PG 승인 성공 후 DB 상태 변경
        order.markApproved(request.paymentKey());
        trade.changeStatus(TradeStatus.PAID);

        return PaymentOrderResponse.from(order);
    }
}
