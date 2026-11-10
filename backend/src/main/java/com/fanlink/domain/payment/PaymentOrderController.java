
package com.fanlink.domain.payment;

import com.fanlink.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/trades")
public class PaymentOrderController {

    private final PaymentOrderService paymentOrderService;

    @PostMapping("/{tradeId}/payment-orders")
    public PaymentOrderResponse prepareOrder(
            @PathVariable Long tradeId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {

        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
        }

        return paymentOrderService.prepareOrder(
                tradeId,
                principal.getUserId()
        );
    }
}
