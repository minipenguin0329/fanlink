
package com.fanlink.domain.matching;

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
public class MatchingActionController {

    private final MatchingService matchingService;

    private Long currentUserId(UserPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
        }
        return principal.getUserId();
    }

    // 호스트의 신청 목록
    @GetMapping("/posts/{postId}/matches")
    public List<MatchingResponse> listForHost(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserPrincipal principal) {

        return matchingService.listForHost(
                postId, currentUserId(principal)
        );
    }

    // 신청 수락
    @PatchMapping("/matches/{requestId}/accept")
    public MatchingResponse accept(
            @PathVariable Long requestId,
            @AuthenticationPrincipal UserPrincipal principal) {

        return matchingService.accept(
                requestId, currentUserId(principal)
        );
    }

    // 신청 거절
    @PatchMapping("/matches/{requestId}/reject")
    public MatchingResponse reject(
            @PathVariable Long requestId,
            @AuthenticationPrincipal UserPrincipal principal) {

        return matchingService.reject(
                requestId, currentUserId(principal)
        );
    }

    // 최종 참여 확정
    @PatchMapping("/matches/{requestId}/confirm")
    public MatchingResponse confirm(
            @PathVariable Long requestId,
            @AuthenticationPrincipal UserPrincipal principal) {

        return matchingService.confirm(
                requestId, currentUserId(principal)
        );
    }

    // 참여 취소
    @PatchMapping("/matches/{requestId}/cancel")
    public MatchingResponse cancel(
            @PathVariable Long requestId,
            @AuthenticationPrincipal UserPrincipal principal) {

        return matchingService.cancel(
                requestId, currentUserId(principal)
        );
    }
}
