
package com.fanlink.domain.matching;

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
class MatchingServiceTest {

    @Mock
    MatchingRequestRepository matchingRepository;

    @Mock
    PostRepository postRepository;

    @Mock
    MatchingPostLockRepository matchingPostLockRepository;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    MatchingService matchingService;

    @Test
    void hostCanAcceptRequest() {

        Post post = mock(Post.class);
        User host = mock(User.class);
        User applicant = mock(User.class);

        when(post.getAuthor()).thenReturn(host);
        when(post.getId()).thenReturn(10L);
        when(host.getId()).thenReturn(1L);
        when(applicant.getId()).thenReturn(2L);

        MatchingRequest request =
                new MatchingRequest(post, applicant);

        when(matchingRepository.findById(100L))
                .thenReturn(Optional.of(request));
        when(post.getStatus()).thenReturn(com.fanlink.domain.post.PostStatus.ACTIVE);
        when(matchingPostLockRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(post));

        MatchingResponse response =
                matchingService.accept(100L, 1L);

        assertEquals(
                MatchStatus.HOST_ACCEPTED,
                response.status()
        );
    }

    @Test
    void otherUserCannotAcceptRequest() {

        Post post = mock(Post.class);
        User host = mock(User.class);
        User applicant = mock(User.class);

        when(post.getAuthor()).thenReturn(host);
        when(host.getId()).thenReturn(1L);

        MatchingRequest request =
                new MatchingRequest(post, applicant);

        when(matchingRepository.findById(100L))
                .thenReturn(Optional.of(request));

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> matchingService.accept(100L, 999L)
                );

        assertEquals(
                HttpStatus.FORBIDDEN,
                exception.getStatusCode()
        );
    }

    @Test
    void applicantCanCancelRequest() {

        Post post = mock(Post.class);
        User applicant = mock(User.class);

        when(post.getId()).thenReturn(10L);
        when(applicant.getId()).thenReturn(2L);

        MatchingRequest request =
                new MatchingRequest(post, applicant);

        when(matchingRepository.findById(100L))
                .thenReturn(Optional.of(request));

        MatchingResponse response =
                matchingService.cancel(100L, 2L);

        assertEquals(
                MatchStatus.CANCELED,
                response.status()
        );
    }
}
