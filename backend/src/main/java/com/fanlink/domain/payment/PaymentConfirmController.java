
package com.fanlink.domain.payment;

import com.fanlink.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentConfirmController {

    private final PaymentConfirmService confirmService;

    @PostMapping("/confirm")
    public PaymentOrderResponse confirm(
            @RequestBody PaymentConfirmRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
        }

        return confirmService.confirm(
                request,
                principal.getUserId()
        );
    }
}
