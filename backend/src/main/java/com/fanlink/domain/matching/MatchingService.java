
package com.fanlink.domain.matching;

import com.fanlink.domain.post.Post;
import com.fanlink.domain.post.PostCategory;
import com.fanlink.domain.post.PostRepository;
import com.fanlink.domain.post.PostStatus;
import com.fanlink.domain.user.User;
import com.fanlink.domain.user.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchingService {

    private final MatchingRequestRepository matchingRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    // 로그인 확인
    private void requireLogin(Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
        }
    }

    // 매칭 신청
    @Transactional
    public MatchingResponse apply(Long postId, Long userId) {

        requireLogin(userId);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "게시글을 찾을 수 없습니다."
                ));

        if (post.getCategory() != PostCategory.MATE &&
                post.getCategory() != PostCategory.CARPOOL) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "메이트 또는 카풀 게시글만 신청할 수 있습니다."
            );
        }

        if (post.getStatus() != PostStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "모집 중인 게시글이 아닙니다."
            );
        }

        if (post.getAuthor().getId().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "본인 게시글에는 신청할 수 없습니다."
            );
        }

        User applicant = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "회원이 존재하지 않습니다."
                ));

        if (matchingRepository.existsByPost_IdAndApplicant_Id(
                postId, userId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 신청한 게시글입니다."
            );
        }

        MatchingRequest request =
                new MatchingRequest(post, applicant);

        try {
            return MatchingResponse.from(
                    matchingRepository.saveAndFlush(request)
            );
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "중복 신청이거나 저장할 수 없는 요청입니다.",
                    e
            );
        }
    }

    // 내 신청 내역 조회
    @Transactional(readOnly = true)
    public MatchingResponse getMyRequest(
            Long postId, Long userId) {

        requireLogin(userId);

        MatchingRequest request = matchingRepository
                .findByPost_IdAndApplicant_Id(postId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "신청 내역이 없습니다."
                ));

        return MatchingResponse.from(request);
    }

    // 호스트가 자신의 게시글에 들어온 신청 목록 조회
    @Transactional(readOnly = true)
    public List<MatchingResponse> listForHost(
            Long postId, Long hostId) {

        requireLogin(hostId);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "게시글이 존재하지 않습니다."
                ));

        if (!post.getAuthor().getId().equals(hostId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "게시글 작성자만 신청 목록을 볼 수 있습니다."
            );
        }

        return matchingRepository
                .findByPost_IdOrderByCreatedAtDesc(postId)
                .stream()
                .map(MatchingResponse::from)
                .toList();
    }

    // 호스트 권한 확인
    private void verifyHost(
            MatchingRequest request, Long hostId) {

        if (!request.getPost().getAuthor().getId().equals(hostId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "게시글 작성자만 처리할 수 있습니다."
            );
        }
    }

    // 신청자 권한 확인
    private void verifyApplicant(
            MatchingRequest request, Long userId) {

        if (!request.getApplicant().getId().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "신청자 본인만 처리할 수 있습니다."
            );
        }
    }

    // 매칭 신청 조회
    private MatchingRequest findRequest(Long requestId) {

        return matchingRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "매칭 신청 내역을 찾을 수 없습니다."
                ));
    }

    // 호스트가 매칭 신청 수락
    @Transactional
    public MatchingResponse accept(
            Long requestId, Long hostId) {

        requireLogin(hostId);

        MatchingRequest request = findRequest(requestId);
        verifyHost(request, hostId);

        if (request.getStatus() != MatchStatus.REQUESTED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "신청 대기 상태에서만 수락할 수 있습니다."
            );
        }

        request.changeStatus(MatchStatus.HOST_ACCEPTED);

        return MatchingResponse.from(request);
    }

    // 호스트가 매칭 신청 거절
    @Transactional
    public MatchingResponse reject(
            Long requestId, Long hostId) {

        requireLogin(hostId);

        MatchingRequest request = findRequest(requestId);
        verifyHost(request, hostId);

        if (request.getStatus() != MatchStatus.REQUESTED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "신청 대기 상태에서만 거절할 수 있습니다."
            );
        }

        request.changeStatus(MatchStatus.REJECTED);

        return MatchingResponse.from(request);
    }

    // 신청자가 최종 참여 확정
    @Transactional
    public MatchingResponse confirm(
            Long requestId, Long userId) {

        requireLogin(userId);

        MatchingRequest request = findRequest(requestId);
        verifyApplicant(request, userId);

        if (request.getStatus() != MatchStatus.HOST_ACCEPTED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "호스트가 수락한 신청만 확정할 수 있습니다."
            );
        }

        request.changeStatus(MatchStatus.CONFIRMED);

        return MatchingResponse.from(request);
    }

    // 신청자가 참여 취소
    @Transactional
    public MatchingResponse cancel(
            Long requestId, Long userId) {

        requireLogin(userId);

        MatchingRequest request = findRequest(requestId);
        verifyApplicant(request, userId);

        MatchStatus status = request.getStatus();

        if (status != MatchStatus.REQUESTED &&
                status != MatchStatus.HOST_ACCEPTED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "현재 상태에서는 취소할 수 없습니다."
            );
        }

        request.changeStatus(MatchStatus.CANCELED);

        return MatchingResponse.from(request);
    }
}
