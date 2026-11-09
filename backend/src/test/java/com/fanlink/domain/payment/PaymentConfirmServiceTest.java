
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentConfirmServiceTest {

    @Mock
    PaymentOrderRepository orderRepository;

    @Mock
    PaymentTradeLockRepository tradeLockRepository;

    @Mock
    TossPaymentClient tossPaymentClient;

    @InjectMocks
    PaymentConfirmService confirmService;

    @Test
    void approvedPaymentChangesTradeStatus() {

        Trade trade = mock(Trade.class);
        User buyer = mock(User.class);

        PaymentOrder order =
                new PaymentOrder(
                        trade, "FL_test001", 10000
                );

        when(trade.getId()).thenReturn(5L);
        when(trade.getBuyer()).thenReturn(buyer);
        when(buyer.getId()).thenReturn(2L);

        when(trade.getStatus())
                .thenReturn(TradeStatus.ACCEPTED);

        when(orderRepository.findByOrderId("FL_test001"))
                .thenReturn(Optional.of(order));

        when(tradeLockRepository.findByIdForUpdate(5L))
                .thenReturn(Optional.of(trade));

        when(orderRepository.findByOrderIdForUpdate(
                "FL_test001"
        )).thenReturn(Optional.of(order));

        PaymentConfirmRequest request =
                new PaymentConfirmRequest(
                        "test-payment-key",
                        "FL_test001",
                        10000
                );

        when(tossPaymentClient.confirm("test-payment-key", "FL_test001", 10000))
                .thenReturn(new TossPaymentResult(true, "ESCROW_TEST_123"));

        PaymentOrderResponse response =
                confirmService.confirm(request, 2L);

        assertEquals(
                PaymentOrderStatus.APPROVED,
                response.status()
        );
        assertTrue(response.escrowVerified());
        assertEquals("ESCROW_TEST_123", order.getEscrowKey());

        assertEquals(
                "test-payment-key",
                order.getPaymentKey()
        );

        verify(tossPaymentClient).confirm(
                "test-payment-key",
                "FL_test001",
                10000
        );

        verify(trade).changeStatus(TradeStatus.PAID);
    }

    @Test
    void manipulatedAmountIsRejected() {

        Trade trade = mock(Trade.class);
        User buyer = mock(User.class);

        PaymentOrder order =
                new PaymentOrder(
                        trade, "FL_test001", 10000
                );

        when(trade.getId()).thenReturn(5L);
        when(trade.getBuyer()).thenReturn(buyer);
        when(buyer.getId()).thenReturn(2L);

        when(orderRepository.findByOrderId("FL_test001"))
                .thenReturn(Optional.of(order));

        when(tradeLockRepository.findByIdForUpdate(5L))
                .thenReturn(Optional.of(trade));

        when(orderRepository.findByOrderIdForUpdate(
                "FL_test001"
        )).thenReturn(Optional.of(order));

        PaymentConfirmRequest request =
                new PaymentConfirmRequest(
                        "test-payment-key",
                        "FL_test001",
                        100
                );

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> confirmService.confirm(
                                request, 2L
                        )
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        verifyNoInteractions(tossPaymentClient);
        verify(trade, never())
                .changeStatus(TradeStatus.PAID);
    }

    @Test
    void paidWithoutProviderEscrowMustNotBecomePaidTrade() {
        Trade trade = mock(Trade.class);
        User buyer = mock(User.class);
        PaymentOrder order = new PaymentOrder(trade, "FL_review001", 10000);
        when(trade.getId()).thenReturn(5L);
        when(trade.getBuyer()).thenReturn(buyer);
        when(buyer.getId()).thenReturn(2L);
        when(trade.getStatus()).thenReturn(TradeStatus.ACCEPTED);
        when(orderRepository.findByOrderId("FL_review001")).thenReturn(Optional.of(order));
        when(tradeLockRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(trade));
        when(orderRepository.findByOrderIdForUpdate("FL_review001")).thenReturn(Optional.of(order));
        when(tossPaymentClient.confirm("payment-no-escrow", "FL_review001", 10000))
                .thenReturn(new TossPaymentResult(false, ""));
        PaymentOrderResponse response = confirmService.confirm(
                new PaymentConfirmRequest("payment-no-escrow", "FL_review001", 10000), 2L);
        assertEquals(PaymentOrderStatus.ESCROW_REVIEW, response.status());
        assertFalse(response.escrowVerified());
        verify(trade, never()).changeStatus(TradeStatus.PAID);
        verify(tossPaymentClient).confirm("payment-no-escrow", "FL_review001", 10000);
    }

}
