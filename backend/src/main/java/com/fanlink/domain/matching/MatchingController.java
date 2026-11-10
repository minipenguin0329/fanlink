
package com.fanlink.domain.matching;

import com.fanlink.security.UserPrincipal;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class MatchingController {

    private final MatchingService matchingService;

    @PostMapping("/{postId}/matches")
    @ResponseStatus(HttpStatus.CREATED)
    public MatchingResponse apply(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
        }

        return matchingService.apply(
                postId,
                principal.getUserId()
        );
    }

    @GetMapping("/{postId}/matches/me")
    public MatchingResponse getMyRequest(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
        }

        return matchingService.getMyRequest(
                postId,
                principal.getUserId()
        );
    }
}
