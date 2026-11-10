package com.fanlink.integration;

import com.fanlink.domain.chat.ChatMessageResponse;
import com.fanlink.domain.chat.ChatRoomResponse;
import com.fanlink.domain.chat.ChatService;
import com.fanlink.domain.matching.MatchStatus;
import com.fanlink.domain.matching.MatchingRequestRepository;
import com.fanlink.domain.matching.MatchingResponse;
import com.fanlink.domain.matching.MatchingService;
import com.fanlink.domain.payment.PaymentConfirmRequest;
import com.fanlink.domain.payment.PaymentConfirmService;
import com.fanlink.domain.payment.PaymentOrderRepository;
import com.fanlink.domain.payment.PaymentOrderResponse;
import com.fanlink.domain.payment.PaymentOrderService;
import com.fanlink.domain.payment.PaymentOrderStatus;
import com.fanlink.domain.payment.TossPaymentClient;
import com.fanlink.domain.post.Post;
import com.fanlink.domain.post.PostCategory;
import com.fanlink.domain.post.PostRepository;
import com.fanlink.domain.trade.TradeRepository;
import com.fanlink.domain.trade.TradeResponse;
import com.fanlink.domain.trade.TradeService;
import com.fanlink.domain.trade.TradeStatus;
import com.fanlink.domain.user.AuthProvider;
import com.fanlink.domain.user.Team;
import com.fanlink.domain.user.User;
import com.fanlink.domain.user.UserRepository;
import com.fanlink.domain.payment.TossPaymentResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.EntityManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Real Spring Data JPA repositories + real service methods + embedded H2 database.
 * External Toss PG is stubbed; this does NOT test a live Toss payment, HTTP security,
 * STOMP delivery, PostgreSQL-specific locking, or React Native.
 */
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import({
        MatchingService.class,
        ChatService.class,
        TradeService.class,
        PaymentOrderService.class,
        PaymentConfirmService.class
})
class CoreFlowJpaIntegrationTest {

    @Autowired UserRepository users;
    @Autowired PostRepository posts;
    @Autowired MatchingRequestRepository matchRequests;
    @Autowired TradeRepository trades;
    @Autowired PaymentOrderRepository paymentOrders;
    @Autowired MatchingService matching;
    @Autowired ChatService chat;
    @Autowired TradeService trade;
    @Autowired PaymentOrderService orders;
    @Autowired PaymentConfirmService payments;
    @Autowired EntityManager entityManager;

    @MockBean TossPaymentClient toss;

    private User newUser(String key) {
        return users.saveAndFlush(new User(
                AuthProvider.KAKAO, "jpa-integration-" + key, key, null, null));
    }

    private Post newPost(User author, PostCategory category, Integer facePrice) {
        return posts.saveAndFlush(new Post(
                author, category, Team.SAMSUNG_LIONS,
                "통합 테스트 게시물", "테스트 본문",
                facePrice, 2, null));
    }

    @Test
    void mateApplyAcceptConfirmThenBothParticipantsCanReadMessages() {
        User host = newUser("mate-host");
        User applicant = newUser("mate-applicant");
        Post post = newPost(host, PostCategory.MATE, null);

        MatchingResponse requested = matching.apply(post.getId(), applicant.getId());
        assertEquals(MatchStatus.REQUESTED, requested.status());

        ChatRoomResponse room = chat.createRoom(post.getId(), applicant.getId());
        assertEquals(post.getId(), room.postId());
        assertEquals(applicant.getId(), room.participantId());

        assertEquals(MatchStatus.HOST_ACCEPTED,
                matching.accept(requested.requestId(), host.getId()).status());
        assertEquals(MatchStatus.CONFIRMED,
                matching.confirm(requested.requestId(), applicant.getId()).status());

        ChatMessageResponse sent = chat.sendMessage(room.roomId(), applicant.getId(), "같이 응원해요!");
        assertEquals(applicant.getId(), sent.senderId());
        assertEquals("같이 응원해요!", sent.content());

        List<ChatMessageResponse> hostMessages = chat.getMessages(room.roomId(), host.getId());
        assertEquals(1, hostMessages.size());
        assertEquals(sent.messageId(), hostMessages.get(0).messageId());
        assertEquals(1, chat.getMyRooms(host.getId()).size());

        entityManager.flush();
        entityManager.clear();
        assertEquals(MatchStatus.CONFIRMED,
                matchRequests.findById(requested.requestId()).orElseThrow().getStatus());
    }

    @Test
    void unrelatedUserCannotReadOrSendMatchingChat() {
        User host = newUser("chat-host");
        User applicant = newUser("chat-applicant");
        User outsider = newUser("chat-outsider");
        Post post = newPost(host, PostCategory.CARPOOL, null);

        matching.apply(post.getId(), applicant.getId());
        ChatRoomResponse room = chat.createRoom(post.getId(), applicant.getId());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> chat.sendMessage(room.roomId(), outsider.getId(), "침입"));
        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
    }

    @Test
    void ticketRequestAcceptanceOrderAndStubbedApprovalPersistInDatabase() {
        User seller = newUser("ticket-seller");
        User buyer = newUser("ticket-buyer");
        Post ticket = newPost(seller, PostCategory.TICKET, 10000);

        // The ticket entity is seeded directly for this test; PostService OCR
        // and the actual HTTP create-post endpoint are NOT exercised here.
        TradeResponse requested = trade.request(ticket.getId(), buyer.getId());
        assertEquals(TradeStatus.REQUESTED, requested.status());
        assertEquals(TradeStatus.ACCEPTED,
                trade.accept(requested.tradeId(), seller.getId()).status());

        PaymentOrderResponse order = orders.prepareOrder(requested.tradeId(), buyer.getId());
        assertEquals(10000, order.amount());
        assertEquals(PaymentOrderStatus.READY, order.status());
        assertEquals(order.orderId(), orders.prepareOrder(requested.tradeId(), buyer.getId()).orderId());

        when(toss.confirm("test-payment-key-integrated", order.orderId(), 10000))
                .thenReturn(new TossPaymentResult(true, "ESCROW_TEST_INTEGRATED"));

        PaymentOrderResponse approved = payments.confirm(
                new PaymentConfirmRequest("test-payment-key-integrated", order.orderId(), 10000),
                buyer.getId());
        assertEquals(PaymentOrderStatus.APPROVED, approved.status());
        verify(toss, times(1)).confirm("test-payment-key-integrated", order.orderId(), 10000);

        // Approved requests with the same paymentKey must not call PG twice.
        assertEquals(PaymentOrderStatus.APPROVED,
                payments.confirm(new PaymentConfirmRequest(
                        "test-payment-key-integrated", order.orderId(), 10000), buyer.getId()).status());
        verifyNoMoreInteractions(toss);

        entityManager.flush();
        entityManager.clear();
        assertEquals(TradeStatus.PAID,
                trades.findById(requested.tradeId()).orElseThrow().getStatus());
        assertEquals(PaymentOrderStatus.APPROVED,
                paymentOrders.findByOrderId(order.orderId()).orElseThrow().getStatus());
    }

    @Test
    void tamperedAmountIsRejectedBeforeTossIsCalled() {
        User seller = newUser("tamper-seller");
        User buyer = newUser("tamper-buyer");
        Post ticket = newPost(seller, PostCategory.TICKET, 15000);

        TradeResponse request = trade.request(ticket.getId(), buyer.getId());
        trade.accept(request.tradeId(), seller.getId());
        PaymentOrderResponse order = orders.prepareOrder(request.tradeId(), buyer.getId());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> payments.confirm(new PaymentConfirmRequest(
                        "test-payment-key-tampered", order.orderId(), 100), buyer.getId()));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verifyNoInteractions(toss);
    }
}
