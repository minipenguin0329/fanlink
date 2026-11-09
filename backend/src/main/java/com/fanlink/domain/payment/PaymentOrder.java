
package com.fanlink.domain.payment;

import com.fanlink.domain.trade.Trade;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "trade_id",
            nullable = false,
            unique = true
    )
    private Trade trade;

    @Column(
            name = "order_id",
            nullable = false,
            unique = true,
            length = 64
    )
    private String orderId;

    @Column(nullable = false)
    private int amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentOrderStatus status =
            PaymentOrderStatus.READY;

    @Column(name = "payment_key", unique = true, length = 200)
    private String paymentKey;

    @Version
    private Long version;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public PaymentOrder(
            Trade trade,
            String orderId,
            int amount
    ) {
        if (trade == null) {
            throw new IllegalArgumentException(
                    "거래 정보가 필요합니다."
            );
        }

        if (orderId == null || orderId.isBlank()
                || orderId.length() > 64) {
            throw new IllegalArgumentException(
                    "올바른 주문번호가 필요합니다."
            );
        }

        if (amount <= 0) {
            throw new IllegalArgumentException(
                    "결제 금액은 0원보다 커야 합니다."
            );
        }

        this.trade = trade;
        this.orderId = orderId;
        this.amount = amount;
        this.status = PaymentOrderStatus.READY;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void markApproved(String confirmedPaymentKey) {
        if (status != PaymentOrderStatus.READY) {
            throw new IllegalStateException(
                    "승인 대기 주문만 결제 완료 처리할 수 있습니다."
            );
        }

        if (confirmedPaymentKey == null
                || confirmedPaymentKey.isBlank()
                || confirmedPaymentKey.length() > 200) {
            throw new IllegalArgumentException(
                    "올바른 결제 식별자가 필요합니다."
            );
        }

        this.paymentKey = confirmedPaymentKey;
        this.status = PaymentOrderStatus.APPROVED;
    }

}
