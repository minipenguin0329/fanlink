package com.fanlink.domain.payment;

public record EscrowStatusResponse(
        Long tradeId,
        String status,
        String confirmType,
        String cancelType,
        int productCount,
        int registeredDeliveryCount
) { }
