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
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchingFlowAdditionalTest {

    @Mock MatchingRequestRepository matchingRepository;
    @Mock PostRepository postRepository;
    @Mock MatchingPostLockRepository matchingPostLockRepository;
    @Mock UserRepository userRepository;
    @InjectMocks MatchingService matchingService;

    @Test
    void hostAcceptThenApplicantConfirms() {
        Post post = mock(Post.class);
        User host = mock(User.class);
        User applicant = mock(User.class);
        when(post.getAuthor()).thenReturn(host);
        when(post.getId()).thenReturn(10L);
        when(host.getId()).thenReturn(1L);
        when(applicant.getId()).thenReturn(2L);

        MatchingRequest request = new MatchingRequest(post, applicant);
        when(matchingRepository.findById(100L)).thenReturn(Optional.of(request));
        when(post.getStatus()).thenReturn(com.fanlink.domain.post.PostStatus.ACTIVE);
        when(matchingPostLockRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(post));

        MatchingResponse accepted = matchingService.accept(100L, 1L);
        assertEquals(MatchStatus.HOST_ACCEPTED, accepted.status());

        MatchingResponse confirmed = matchingService.confirm(100L, 2L);
        assertEquals(MatchStatus.CONFIRMED, confirmed.status());
    }

    @Test
    void applicantCannotConfirmBeforeHostAccept() {
        Post post = mock(Post.class);
        User applicant = mock(User.class);
        when(applicant.getId()).thenReturn(2L);
        MatchingRequest request = new MatchingRequest(post, applicant);
        when(matchingRepository.findById(100L)).thenReturn(Optional.of(request));

        assertThrows(ResponseStatusException.class,
                () -> matchingService.confirm(100L, 2L));
        assertEquals(MatchStatus.REQUESTED, request.getStatus());
    }
}
