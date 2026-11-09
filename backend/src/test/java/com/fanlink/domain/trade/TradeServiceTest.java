
package com.fanlink.domain.trade;

import com.fanlink.domain.post.Post;
import com.fanlink.domain.post.PostCategory;
import com.fanlink.domain.post.PostRepository;
import com.fanlink.domain.post.PostStatus;
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
class TradeServiceTest {

    @Mock
    TradeRepository tradeRepository;

    @Mock
    TradePostLockRepository postLockRepository;

    @Mock
    PostRepository postRepository;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    TradeService tradeService;

    @Test
    void sellerCannotBuyOwnTicket() {

        Post post = mock(Post.class);
        User seller = mock(User.class);

        when(postLockRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(post));

        when(post.getCategory())
                .thenReturn(PostCategory.TICKET);

        when(post.getStatus())
                .thenReturn(PostStatus.ACTIVE);

        when(post.getAuthor()).thenReturn(seller);
        when(seller.getId()).thenReturn(1L);

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> tradeService.request(10L, 1L)
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        verifyNoInteractions(tradeRepository);
    }

    @Test
    void sellerCanAcceptRequest() {

        Post post = mock(Post.class);
        User seller = mock(User.class);
        User buyer = mock(User.class);

        when(post.getFacePrice()).thenReturn(10000);

        Trade trade = new Trade(post, buyer);

        when(post.getId()).thenReturn(10L);
        when(post.getAuthor()).thenReturn(seller);
        when(post.getStatus()).thenReturn(PostStatus.ACTIVE);

        when(seller.getId()).thenReturn(1L);
        when(buyer.getId()).thenReturn(2L);

        when(tradeRepository.findById(5L))
                .thenReturn(Optional.of(trade));

        when(postLockRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(post));

        TradeResponse response =
                tradeService.accept(5L, 1L);

        assertEquals(
                TradeStatus.ACCEPTED,
                response.status()
        );
    }

    @Test
    void otherUserCannotAcceptRequest() {

        Post post = mock(Post.class);
        User seller = mock(User.class);
        User buyer = mock(User.class);

        when(post.getFacePrice()).thenReturn(10000);

        Trade trade = new Trade(post, buyer);

        when(post.getId()).thenReturn(10L);
        when(post.getAuthor()).thenReturn(seller);
        when(seller.getId()).thenReturn(1L);

        when(tradeRepository.findById(5L))
                .thenReturn(Optional.of(trade));

        when(postLockRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(post));

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> tradeService.accept(5L, 999L)
                );

        assertEquals(
                HttpStatus.FORBIDDEN,
                exception.getStatusCode()
        );
    }
}
