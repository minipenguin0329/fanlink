
package com.fanlink.domain.trade;

import java.time.LocalDateTime;

public record TradeResponse(
        Long tradeId,
        Long postId,
        Long buyerId,
        Long sellerId,
        int unitFacePrice,
        TradeStatus status,
        LocalDateTime createdAt
) {
    public static TradeResponse from(Trade trade) {
        return new TradeResponse(
                trade.getId(),
                trade.getPost().getId(),
                trade.getBuyer().getId(),
                trade.getPost().getAuthor().getId(),
                trade.getUnitFacePrice(),
                trade.getStatus(),
                trade.getCreatedAt()
        );
    }
}
