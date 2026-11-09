
package com.fanlink.domain.trade;

import com.fanlink.domain.post.Post;
import com.fanlink.domain.post.PostCategory;
import com.fanlink.domain.post.PostRepository;
import com.fanlink.domain.post.PostStatus;
import com.fanlink.domain.user.User;
import com.fanlink.domain.user.UserRepository;

import com.fanlink.domain.payment.PaymentOrderRepository;
import com.fanlink.domain.payment.PaymentTradeLockRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TradeService {

    private static final List<TradeStatus> RESERVED =
            List.of(
                    TradeStatus.ACCEPTED,
                    TradeStatus.PAYMENT_PENDING,
                    TradeStatus.PAID,
                    TradeStatus.DELIVERED,
                    TradeStatus.COMPLETED
            );

    private final TradeRepository tradeRepository;
    private final TradePostLockRepository postLockRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;


    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentTradeLockRepository paymentTradeLockRepository;


    private void requireLogin(Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
        }
    }

    private Trade findTrade(Long tradeId) {
        return tradeRepository.findById(tradeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "거래를 찾을 수 없습니다."
                ));
    }

    private Post lockPost(Long postId) {
        return postLockRepository.findByIdForUpdate(postId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "게시글을 찾을 수 없습니다."
                ));
    }

    private void verifySeller(
            Trade trade, Long sellerId) {

        if (!trade.getPost().getAuthor()
                .getId().equals(sellerId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "판매자만 처리할 수 있습니다."
            );
        }
    }

    private void verifyBuyer(
            Trade trade, Long buyerId) {

        if (!trade.getBuyer().getId().equals(buyerId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "구매자만 처리할 수 있습니다."
            );
        }
    }

    // 구매자가 거래 요청
    @Transactional
    public TradeResponse request(
            Long postId, Long buyerId) {

        requireLogin(buyerId);

        Post post = lockPost(postId);

        if (post.getCategory() != PostCategory.TICKET) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "티켓 게시글만 거래 요청할 수 있습니다."
            );
        }

        if (post.getStatus() != PostStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "거래 가능한 게시글이 아닙니다."
            );
        }

        if (post.getAuthor().getId().equals(buyerId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "본인 티켓은 구매할 수 없습니다."
            );
        }

        if (post.getFacePrice() == null ||
                post.getFacePrice() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "티켓 정가 정보가 올바르지 않습니다."
            );
        }

        if (tradeRepository.existsByPost_IdAndBuyer_Id(
                postId, buyerId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 거래 요청 기록이 있습니다."
            );
        }

        if (tradeRepository.existsByPost_IdAndStatusIn(
                postId, RESERVED)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 다른 구매자의 거래가 확정되었습니다."
            );
        }

        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "구매자를 찾을 수 없습니다."
                ));

        try {
            Trade trade = new Trade(post, buyer);
            return TradeResponse.from(
                    tradeRepository.saveAndFlush(trade)
            );
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "중복 거래 요청입니다.",
                    ex
            );
        }
    }

    // 구매자의 거래 목록
    @Transactional(readOnly = true)
    public List<TradeResponse> getMyTrades(Long buyerId) {

        requireLogin(buyerId);

        return tradeRepository
                .findByBuyer_IdOrderByCreatedAtDesc(buyerId)
                .stream()
                .map(TradeResponse::from)
                .toList();
    }

    // 판매자의 거래 요청 목록
    @Transactional(readOnly = true)
    public List<TradeResponse> getPostTrades(
            Long postId, Long sellerId) {

        requireLogin(sellerId);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "게시글을 찾을 수 없습니다."
                ));

        if (!post.getAuthor().getId().equals(sellerId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "판매자만 거래 목록을 볼 수 있습니다."
            );
        }

        return tradeRepository
                .findByPost_IdOrderByCreatedAtDesc(postId)
                .stream()
                .map(TradeResponse::from)
                .toList();
    }

    // 판매자 거래 수락
    @Transactional
    public TradeResponse accept(
            Long tradeId, Long sellerId) {

        requireLogin(sellerId);

        Trade trade = findTrade(tradeId);
        Post post = lockPost(trade.getPost().getId());

        verifySeller(trade, sellerId);

        if (post.getStatus() != PostStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "판매 중인 게시글이 아닙니다."
            );
        }

        if (trade.getStatus() != TradeStatus.REQUESTED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "요청 대기 상태에서만 수락할 수 있습니다."
            );
        }

        if (tradeRepository.existsByPost_IdAndStatusIn(
                post.getId(), RESERVED)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 다른 거래가 확정되었습니다."
            );
        }

        trade.changeStatus(TradeStatus.ACCEPTED);
        return TradeResponse.from(trade);
    }

    // 판매자 거래 거절
    @Transactional
    public TradeResponse reject(
            Long tradeId, Long sellerId) {

        requireLogin(sellerId);

        Trade trade = findTrade(tradeId);
        verifySeller(trade, sellerId);

        if (trade.getStatus() != TradeStatus.REQUESTED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "요청 대기 상태에서만 거절할 수 있습니다."
            );
        }

        trade.changeStatus(TradeStatus.REJECTED);
        return TradeResponse.from(trade);
    }

    // 구매자 거래 취소

    @Transactional
    public TradeResponse cancel(
            Long tradeId, Long buyerId) {

        requireLogin(buyerId);

        // 거래 행을 먼저 잠금
        Trade trade = paymentTradeLockRepository
                .findByIdForUpdate(tradeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "거래를 찾을 수 없습니다."
                ));

        verifyBuyer(trade, buyerId);

        if (trade.getStatus() != TradeStatus.REQUESTED &&
                trade.getStatus() != TradeStatus.ACCEPTED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "현재 상태에서는 취소할 수 없습니다."
            );
        }

        // 결제 주문 생성 후에는 일반 취소 차단
        if (paymentOrderRepository.existsByTrade_Id(tradeId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "결제 주문이 생성되어 일반 취소할 수 없습니다."
            );
        }

        trade.changeStatus(TradeStatus.CANCELED);

        return TradeResponse.from(trade);
    }

}
