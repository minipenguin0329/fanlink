package com.fanlink.domain.trade;

import com.fanlink.domain.payment.PaymentOrderRepository;
import com.fanlink.domain.payment.PaymentTradeLockRepository;
import com.fanlink.domain.post.Post;
import com.fanlink.domain.post.PostRepository;
import com.fanlink.domain.user.User;
import com.fanlink.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradeLifecycleTest {
    @Mock TradeRepository tradeRepository;
    @Mock TradePostLockRepository postLockRepository;
    @Mock PostRepository postRepository;
    @Mock UserRepository userRepository;
    @Mock PaymentOrderRepository paymentOrderRepository;
    @Mock PaymentTradeLockRepository paymentTradeLockRepository;
    @InjectMocks TradeService service;

    private Trade tradeAt(TradeStatus status) {
        Post post = mock(Post.class);
        User seller = mock(User.class);
        User buyer = mock(User.class);
        when(post.getFacePrice()).thenReturn(10000);
        Trade trade = new Trade(post, buyer);
        trade.changeStatus(status);
        lenient().when(post.getId()).thenReturn(10L);
        lenient().when(post.getAuthor()).thenReturn(seller);
        lenient().when(seller.getId()).thenReturn(1L);
        lenient().when(buyer.getId()).thenReturn(2L);
        when(paymentTradeLockRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(trade));
        return trade;
    }

    @Test void paidTicketDeliveredAndBuyerConfirmed() {
        Trade trade = tradeAt(TradeStatus.PAID);
        assertEquals(TradeStatus.DELIVERED, service.markDelivered(4L, 1L).status());
        assertEquals(TradeStatus.COMPLETED, service.confirmReceipt(4L, 2L).status());
    }

    @Test void unpaidTicketCannotBeDelivered() {
        Trade trade = tradeAt(TradeStatus.ACCEPTED);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.markDelivered(4L, 1L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals(TradeStatus.ACCEPTED, trade.getStatus());
    }

    @Test void unrelatedUserCannotConfirmReceipt() {
        Trade trade = tradeAt(TradeStatus.DELIVERED);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.confirmReceipt(4L, 999L));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertEquals(TradeStatus.DELIVERED, trade.getStatus());
    }
}
