
package com.fanlink.domain.payment;

import java.time.LocalDateTime;

public record PaymentOrderResponse(
        Long paymentOrderId,
        Long tradeId,
        String orderId,
        int amount,
        PaymentOrderStatus status,
        boolean escrowVerified,
        LocalDateTime createdAt
) {
    public static PaymentOrderResponse from(
            PaymentOrder order
    ) {
        return new PaymentOrderResponse(
                order.getId(),
                order.getTrade().getId(),
                order.getOrderId(),
                order.getAmount(),
                order.getStatus(),
                order.getStatus() == PaymentOrderStatus.APPROVED
                        && order.getEscrowKey() != null,
                order.getCreatedAt()
        );
    }
}
