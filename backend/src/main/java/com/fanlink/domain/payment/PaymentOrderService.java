
package com.fanlink.domain.payment;

import com.fanlink.domain.trade.Trade;
import com.fanlink.domain.trade.TradeStatus;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentOrderService {

    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentTradeLockRepository tradeLockRepository;

    @Transactional
    public PaymentOrderResponse prepareOrder(
            Long tradeId,
            Long buyerId
    ) {

        // 1. 로그인 확인
        if (buyerId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
        }

        // 2. 거래 조회 및 잠금
        Trade trade = tradeLockRepository
                .findByIdForUpdate(tradeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "거래를 찾을 수 없습니다."
                ));

        // 3. 구매자 본인 확인
        if (!trade.getBuyer().getId().equals(buyerId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "구매자만 결제 주문을 생성할 수 있습니다."
            );
        }

        // 4. 판매자가 수락한 거래만 결제 가능
        if (trade.getStatus() != TradeStatus.ACCEPTED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "판매자가 수락한 거래만 결제할 수 있습니다."
            );
        }

        // 5. 기존 결제 주문이 있으면 재사용
        Optional<PaymentOrder> existing =
                paymentOrderRepository.findByTrade_Id(tradeId);

        if (existing.isPresent()) {
            PaymentOrder order = existing.get();

            if (order.getStatus() != PaymentOrderStatus.READY) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "이미 처리된 결제 주문입니다."
                );
            }

            return PaymentOrderResponse.from(order);
        }

        // 6. 결제 금액을 서버에서 결정
        int amount = trade.getUnitFacePrice();

        if (amount <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "결제 금액이 올바르지 않습니다."
            );
        }

        // 7. 고유 주문번호 생성
        String orderId = "FL_" +
                UUID.randomUUID()
                        .toString()
                        .replace("-", "");

        // 8. 결제 주문 생성
        PaymentOrder order = new PaymentOrder(
                trade,
                orderId,
                amount
        );

        // 9. PostgreSQL에 저장
        PaymentOrder saved =
                paymentOrderRepository.saveAndFlush(order);

        return PaymentOrderResponse.from(saved);
    }
}
