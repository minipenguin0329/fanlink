
package com.fanlink.domain.payment;

import com.fanlink.domain.trade.Trade;
import com.fanlink.domain.trade.TradeStatus;
import com.fanlink.domain.user.User;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentOrderServiceTest {

    @Mock
    PaymentOrderRepository orderRepository;

    @Mock
    PaymentTradeLockRepository tradeLockRepository;

    @InjectMocks
    PaymentOrderService paymentOrderService;

    @Test
    void buyerCanPreparePaymentOrder() {

        Trade trade = mock(Trade.class);
        User buyer = mock(User.class);

        when(tradeLockRepository.findByIdForUpdate(5L))
                .thenReturn(Optional.of(trade));

        when(trade.getBuyer()).thenReturn(buyer);
        when(buyer.getId()).thenReturn(2L);

        when(trade.getStatus())
                .thenReturn(TradeStatus.ACCEPTED);

        when(trade.getId()).thenReturn(5L);
        when(trade.getUnitFacePrice()).thenReturn(10000);

        when(orderRepository.findByTrade_Id(5L))
                .thenReturn(Optional.empty());

        when(orderRepository.saveAndFlush(
                any(PaymentOrder.class)
        )).thenAnswer(invocation ->
                invocation.getArgument(0)
        );

        PaymentOrderResponse response =
                paymentOrderService.prepareOrder(5L, 2L);

        assertEquals(5L, response.tradeId());
        assertEquals(10000, response.amount());
        assertEquals(
                PaymentOrderStatus.READY,
                response.status()
        );

        assertTrue(response.orderId().startsWith("FL_"));

        verify(orderRepository).saveAndFlush(
                any(PaymentOrder.class)
        );
    }

    @Test
    void outsiderCannotPreparePaymentOrder() {

        Trade trade = mock(Trade.class);
        User buyer = mock(User.class);

        when(tradeLockRepository.findByIdForUpdate(5L))
                .thenReturn(Optional.of(trade));

        when(trade.getBuyer()).thenReturn(buyer);
        when(buyer.getId()).thenReturn(2L);

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> paymentOrderService
                                .prepareOrder(5L, 999L)
                );

        assertEquals(
                HttpStatus.FORBIDDEN,
                exception.getStatusCode()
        );

        verifyNoInteractions(orderRepository);
    }

    @Test
    void requestedTradeCannotBePaid() {

        Trade trade = mock(Trade.class);
        User buyer = mock(User.class);

        when(tradeLockRepository.findByIdForUpdate(5L))
                .thenReturn(Optional.of(trade));

        when(trade.getBuyer()).thenReturn(buyer);
        when(buyer.getId()).thenReturn(2L);

        when(trade.getStatus())
                .thenReturn(TradeStatus.REQUESTED);

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> paymentOrderService
                                .prepareOrder(5L, 2L)
                );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        verifyNoInteractions(orderRepository);
    }
}
