
package com.fanlink.domain.trade;

import com.fanlink.domain.post.Post;
import com.fanlink.domain.user.User;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "trades",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_trade_post_buyer",
                columnNames = {"post_id", "buyer_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @Column(nullable = false)
    private int unitFacePrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TradeStatus status = TradeStatus.REQUESTED;

    @Version
    private Long version;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Trade(Post post, User buyer) {
        if (post.getFacePrice() == null ||
                post.getFacePrice() <= 0) {
            throw new IllegalArgumentException(
                    "티켓 정가가 올바르지 않습니다."
            );
        }

        this.post = post;
        this.buyer = buyer;
        this.unitFacePrice = post.getFacePrice();
        this.status = TradeStatus.REQUESTED;
    }

    public void changeStatus(TradeStatus nextStatus) {
        this.status = nextStatus;
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
}
