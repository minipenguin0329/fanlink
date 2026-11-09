package com.fanlink.domain.post;

import com.fanlink.common.ApiException;
import com.fanlink.domain.post.dto.PostCreateRequest;
import com.fanlink.domain.post.dto.PostUpdateRequest;
import com.fanlink.domain.user.Team;
import com.fanlink.domain.user.User;
import com.fanlink.domain.user.UserService;
import com.fanlink.ocr.TicketPriceVerificationService;
import com.fanlink.ocr.dto.VerifyTicketPriceResponse;
import com.fanlink.storage.FileStorageService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostPriceSafetyTest {

    @Mock PostRepository postRepository;
    @Mock UserService userService;
    @Mock FileStorageService fileStorageService;
    @Mock TicketPriceVerificationService priceVerificationService;
    @InjectMocks PostService postService;

    @Test
    void ticketWithoutFacePriceCannotBePublished() {
        when(userService.getById(1L)).thenReturn(mock(User.class));
        PostCreateRequest request = ticketRequest(null, "capture.png");

        ApiException error = assertThrows(ApiException.class,
                () -> postService.create(1L, request));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        verifyNoInteractions(postRepository, fileStorageService, priceVerificationService);
    }

    @Test
    void ticketWithoutVerifiedOcrCannotBePublished() {
        when(userService.getById(1L)).thenReturn(mock(User.class));
        byte[] screenshot = new byte[]{1, 2, 3};
        when(fileStorageService.readImage("capture.png")).thenReturn(screenshot);
        when(priceVerificationService.verify(screenshot, "capture.png", 10000))
                .thenReturn(new VerifyTicketPriceResponse(true, false, null, 10000,
                        "OCR 자동 검증에 실패했습니다."));

        ApiException error = assertThrows(ApiException.class,
                () -> postService.create(1L, ticketRequest(10000, "capture.png")));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        verifyNoInteractions(postRepository);
    }

    @Test
    void ticketFacePriceCannotBeChangedAfterVerification() {
        Post post = mock(Post.class);
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(post.getCategory()).thenReturn(PostCategory.TICKET);
        when(post.getFacePrice()).thenReturn(10000);

        PostUpdateRequest change = new PostUpdateRequest(null, null, 50000, null, null);
        ApiException error = assertThrows(ApiException.class,
                () -> postService.update(10L, 1L, change));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        verify(post).verifyAuthor(1L);
        verify(post, never()).update(any(), any(), any(), any(), any());
    }

    @Test
    void ticketCanUpdateTitleWithoutChangingPrice() {
        Post post = mock(Post.class);
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(post.getCategory()).thenReturn(PostCategory.TICKET);

        PostUpdateRequest change = new PostUpdateRequest("새 제목", null, null, null, null);
        assertSame(post, postService.update(10L, 1L, change));
        verify(post).update("새 제목", null, null, null, null);
    }

    private PostCreateRequest ticketRequest(Integer price, String capture) {
        return new PostCreateRequest(PostCategory.TICKET, Team.SAMSUNG_LIONS,
                "테스트 티켓", "좌석 1매", price, 1, List.of(), capture);
    }
}
