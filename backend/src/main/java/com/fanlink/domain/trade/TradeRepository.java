
package com.fanlink.domain.trade;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface TradeRepository
        extends JpaRepository<Trade, Long> {

    boolean existsByPost_IdAndBuyer_Id(
            Long postId, Long buyerId
    );

    boolean existsByPost_IdAndStatusIn(
            Long postId,
            Collection<TradeStatus> statuses
    );

    List<Trade> findByPost_IdOrderByCreatedAtDesc(
            Long postId
    );

    List<Trade> findByBuyer_IdOrderByCreatedAtDesc(
            Long buyerId
    );
}
