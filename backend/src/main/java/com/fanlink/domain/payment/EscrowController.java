package com.fanlink.domain.payment;

import com.fanlink.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/trades/{tradeId}/escrow")
public class EscrowController {
    private final EscrowService service;

    private Long actor(UserPrincipal principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                "로그인이 필요합니다.");
        return principal.getUserId();
    }

    @GetMapping
    public EscrowStatusResponse status(@PathVariable Long tradeId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return service.get(tradeId, actor(principal));
    }

    @PostMapping("/direct-delivery")
    public EscrowStatusResponse received(@PathVariable Long tradeId,
            @RequestBody DirectReceiptRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return service.registerReceived(tradeId, actor(principal),
                request == null ? null : request.receiverName());
    }

    public record DirectReceiptRequest(String receiverName) { }
}
