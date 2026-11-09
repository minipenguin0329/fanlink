package com.fanlink.domain.matching;

import com.fanlink.domain.post.Post;
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
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchingCapacityTest {
    @Mock MatchingRequestRepository matchingRepository;
    @Mock MatchingPostLockRepository matchingPostLockRepository;
    @Mock PostRepository postRepository;
    @Mock UserRepository userRepository;
    @InjectMocks MatchingService service;

    @Test void capacityFullRejectsNextAcceptance() {
        Post post = mock(Post.class);
        User host = mock(User.class);
        User applicant = mock(User.class);
        when(post.getId()).thenReturn(10L);
        when(post.getStatus()).thenReturn(PostStatus.ACTIVE);
        when(post.getQuantity()).thenReturn(1);
        when(post.getAuthor()).thenReturn(host);
        when(host.getId()).thenReturn(1L);
        MatchingRequest request = new MatchingRequest(post, applicant);
        when(matchingRepository.findById(4L)).thenReturn(Optional.of(request));
        when(matchingPostLockRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(post));
        when(matchingRepository.countByPost_IdAndStatusIn(eq(10L), anyList())).thenReturn(1L);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.accept(4L, 1L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals(MatchStatus.REQUESTED, request.getStatus());
    }

    @Test void availableCapacityAllowsAcceptance() {
        Post post = mock(Post.class);
        User host = mock(User.class);
        User applicant = mock(User.class);
        when(post.getId()).thenReturn(10L);
        when(post.getStatus()).thenReturn(PostStatus.ACTIVE);
        when(post.getQuantity()).thenReturn(2);
        when(post.getAuthor()).thenReturn(host);
        when(host.getId()).thenReturn(1L);
        MatchingRequest request = new MatchingRequest(post, applicant);
        when(matchingRepository.findById(4L)).thenReturn(Optional.of(request));
        when(matchingPostLockRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(post));
        when(matchingRepository.countByPost_IdAndStatusIn(eq(10L), anyList())).thenReturn(1L);
        assertEquals(MatchStatus.HOST_ACCEPTED, service.accept(4L, 1L).status());
    }
}
