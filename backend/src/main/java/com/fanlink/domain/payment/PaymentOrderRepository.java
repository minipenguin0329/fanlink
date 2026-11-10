
package com.fanlink.domain.payment;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentOrderRepository
        extends JpaRepository<PaymentOrder, Long> {

    Optional<PaymentOrder> findByTrade_Id(Long tradeId);

    Optional<PaymentOrder> findByOrderId(String orderId);

    boolean existsByTrade_Id(Long tradeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT p FROM PaymentOrder p
        WHERE p.orderId = :orderId
        """)
    Optional<PaymentOrder> findByOrderIdForUpdate(
            @Param("orderId") String orderId
    );
}
