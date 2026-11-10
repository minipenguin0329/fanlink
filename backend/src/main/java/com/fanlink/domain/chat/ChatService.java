
package com.fanlink.domain.chat;

import com.fanlink.domain.matching.MatchStatus;
import com.fanlink.domain.matching.MatchingRequest;
import com.fanlink.domain.matching.MatchingRequestRepository;
import com.fanlink.domain.post.Post;
import com.fanlink.domain.post.PostCategory;
import com.fanlink.domain.post.PostRepository;
import com.fanlink.domain.post.PostStatus;
import com.fanlink.domain.user.User;
import com.fanlink.domain.user.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository roomRepository;
    private final ChatMessageRepository messageRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final MatchingRequestRepository matchingRepository;

    // 로그인 여부 검사
    private void requireLogin(Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
        }
    }

    // 채팅방 참가자인지 검사
    private ChatRoom findAuthorizedRoom(
            Long roomId, Long userId) {

        requireLogin(userId);

        ChatRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "채팅방을 찾을 수 없습니다."
                ));

        if (!room.isMember(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "채팅방 참가자만 접근할 수 있습니다."
            );
        }

        return room;
    }

    // 매칭 상태 검사
    private void verifyMatchingActive(ChatRoom room) {

        if (room.getType() != ChatRoomType.MATCHING) {
            return;
        }

        MatchingRequest matching = matchingRepository
                .findByPost_IdAndApplicant_Id(
                        room.getPost().getId(),
                        room.getParticipant().getId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "유효한 매칭 신청이 없습니다."
                ));

        MatchStatus status = matching.getStatus();

        if (status != MatchStatus.REQUESTED &&
                status != MatchStatus.HOST_ACCEPTED &&
                status != MatchStatus.CONFIRMED) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "종료된 매칭에서는 메시지를 보낼 수 없습니다."
            );
        }
    }

    // 채팅방 생성 또는 기존 채팅방 반환
    @Transactional
    public ChatRoomResponse createRoom(
            Long postId, Long userId) {

        requireLogin(userId);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "게시글이 존재하지 않습니다."
                ));

        if (post.getAuthor().getId().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "본인 게시글에는 채팅을 신청할 수 없습니다."
            );
        }

        if (post.getStatus() != PostStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "현재 이용할 수 없는 게시글입니다."
            );
        }

        ChatRoomType type = switch (post.getCategory()) {
            case TICKET, GOODS -> ChatRoomType.TRADE;
            case MATE, CARPOOL -> ChatRoomType.MATCHING;
        };

        // 메이트·카풀 채팅은 매칭 신청자만 생성 가능
        if (type == ChatRoomType.MATCHING) {
            MatchingRequest matching = matchingRepository
                    .findByPost_IdAndApplicant_Id(postId, userId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "매칭 신청 후 채팅을 시작할 수 있습니다."
                    ));

            MatchStatus status = matching.getStatus();

            if (status != MatchStatus.REQUESTED &&
                    status != MatchStatus.HOST_ACCEPTED &&
                    status != MatchStatus.CONFIRMED) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "진행 중인 매칭이 아닙니다."
                );
            }
        }

        // 기존 채팅방이 있으면 재사용
        var existing = roomRepository
                .findByPost_IdAndParticipant_Id(postId, userId);

        if (existing.isPresent()) {
            return ChatRoomResponse.from(existing.get());
        }

        User participant = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "사용자를 찾을 수 없습니다."
                ));

        ChatRoom room = new ChatRoom(post, participant, type);

        return ChatRoomResponse.from(
                roomRepository.saveAndFlush(room)
        );
    }

    // 내 채팅방 목록
    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getMyRooms(Long userId) {

        requireLogin(userId);

        return roomRepository.findAllForUser(userId)
                .stream()
                .map(ChatRoomResponse::from)
                .toList();
    }

    // 채팅 메시지 저장
    @Transactional
    public ChatMessageResponse sendMessage(
            Long roomId,
            Long userId,
            String content) {

        ChatRoom room = findAuthorizedRoom(roomId, userId);

        verifyMatchingActive(room);

        if (content == null || content.isBlank()
                || content.length() > 2000) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "메시지는 1~2000자여야 합니다."
            );
        }

        User sender = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "사용자가 존재하지 않습니다."
                ));

        ChatMessage message =
                new ChatMessage(room, sender, content);

        return ChatMessageResponse.from(
                messageRepository.saveAndFlush(message)
        );
    }

    // 최근 메시지 50개 조회
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(
            Long roomId, Long userId) {

        findAuthorizedRoom(roomId, userId);

        List<ChatMessageResponse> result = new ArrayList<>();

        for (ChatMessage message : messageRepository
                .findTop50ByRoom_IdOrderByIdDesc(roomId)) {

            result.add(ChatMessageResponse.from(message));
        }

        // 오래된 메시지부터 보여주기
        Collections.reverse(result);

        return result;
    }

    // 실시간 전송에 사용할 메시지와 수신자 정보
    public record ChatDelivery(
            ChatMessageResponse message,
            Long hostId,
            Long participantId
    ) {
    }

    @Transactional
    public ChatDelivery sendForWebSocket(
            Long roomId,
            Long userId,
            String content
    ) {
        // 기존 채팅방 참가자 검사
        ChatRoom room = findAuthorizedRoom(roomId, userId);

        // 기존 메시지 검증 및 DB 저장 기능 재사용
        ChatMessageResponse saved =
                sendMessage(roomId, userId, content);

        // 호스트와 참가자 ID 반환
        return new ChatDelivery(
                saved,
                room.getPost().getAuthor().getId(),
                room.getParticipant().getId()
        );
    }

}
