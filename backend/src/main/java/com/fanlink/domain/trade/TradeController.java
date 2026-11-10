
package com.fanlink.domain.trade;

import com.fanlink.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class TradeController {

    private final TradeService tradeService;

    private Long currentUserId(UserPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
        }

        return principal.getUserId();
    }

    // 구매자가 거래 요청
    @PostMapping("/posts/{postId}/trades")
    @ResponseStatus(HttpStatus.CREATED)
    public TradeResponse request(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserPrincipal principal) {

        return tradeService.request(
                postId, currentUserId(principal)
        );
    }

    // 구매자의 거래 목록
    @GetMapping("/trades/mine")
    public List<TradeResponse> myTrades(
            @AuthenticationPrincipal UserPrincipal principal) {

        return tradeService.getMyTrades(
                currentUserId(principal)
        );
    }

    // 판매자의 거래 요청 목록
    @GetMapping("/posts/{postId}/trades")
    public List<TradeResponse> postTrades(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserPrincipal principal) {

        return tradeService.getPostTrades(
                postId, currentUserId(principal)
        );
    }

    // 판매자가 거래 수락
    @PatchMapping("/trades/{tradeId}/accept")
    public TradeResponse accept(
            @PathVariable Long tradeId,
            @AuthenticationPrincipal UserPrincipal principal) {

        return tradeService.accept(
                tradeId, currentUserId(principal)
        );
    }

    // 판매자가 거래 거절
    @PatchMapping("/trades/{tradeId}/reject")
    public TradeResponse reject(
            @PathVariable Long tradeId,
            @AuthenticationPrincipal UserPrincipal principal) {

        return tradeService.reject(
                tradeId, currentUserId(principal)
        );
    }

    // 판매자: 결제 완료 후 티켓 전달
    @PatchMapping("/trades/{tradeId}/deliver")
    public TradeResponse deliver(
            @PathVariable Long tradeId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return tradeService.markDelivered(tradeId, currentUserId(principal));
    }

    // 구매자: 티켓 수령 확인
    @PatchMapping("/trades/{tradeId}/confirm-receipt")
    public TradeResponse confirmReceipt(
            @PathVariable Long tradeId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return tradeService.confirmReceipt(tradeId, currentUserId(principal));
    }

    // 구매자가 거래 취소
    @PatchMapping("/trades/{tradeId}/cancel")
    public TradeResponse cancel(
            @PathVariable Long tradeId,
            @AuthenticationPrincipal UserPrincipal principal) {

        return tradeService.cancel(
                tradeId, currentUserId(principal)
        );
    }
}
