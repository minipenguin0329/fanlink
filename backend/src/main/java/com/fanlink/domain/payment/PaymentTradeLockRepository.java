
package com.fanlink.domain.payment;

import com.fanlink.domain.trade.Trade;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentTradeLockRepository
        extends Repository<Trade, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT t FROM Trade t
        WHERE t.id = :tradeId
        """)
    Optional<Trade> findByIdForUpdate(
            @Param("tradeId") Long tradeId
    );
}
