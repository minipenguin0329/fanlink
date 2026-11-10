
package com.fanlink.domain.matching;

import java.time.LocalDateTime;

public record MatchingResponse(
        Long requestId,
        Long postId,
        Long applicantId,
        MatchStatus status,
        LocalDateTime createdAt
) {
    public static MatchingResponse from(MatchingRequest request) {
        return new MatchingResponse(
                request.getId(),
                request.getPost().getId(),
                request.getApplicant().getId(),
                request.getStatus(),
                request.getCreatedAt()
        );
    }
}
