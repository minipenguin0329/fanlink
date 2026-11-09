
package com.fanlink.domain.matching;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MatchingRequestRepository
        extends JpaRepository<MatchingRequest, Long> {

    boolean existsByPost_IdAndApplicant_Id(
            Long postId,
            Long applicantId
    );

    Optional<MatchingRequest> findByPost_IdAndApplicant_Id(
            Long postId,
            Long applicantId
    );

    List<MatchingRequest> findByPost_IdOrderByCreatedAtDesc(
            Long postId
    );
}
