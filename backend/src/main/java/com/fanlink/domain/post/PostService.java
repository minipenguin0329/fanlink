package com.fanlink.domain.post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fanlink.common.ApiException;
import com.fanlink.domain.post.dto.PostCreateRequest;
import com.fanlink.domain.post.dto.PostUpdateRequest;
import com.fanlink.domain.user.Team;
import com.fanlink.domain.user.User;
import com.fanlink.domain.user.UserService;
import com.fanlink.ocr.TicketPriceVerificationService;
import com.fanlink.ocr.dto.VerifyTicketPriceResponse;
import com.fanlink.storage.FileStorageService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostService {

	private final PostRepository postRepository;
	private final UserService userService;
	private final FileStorageService fileStorageService;
	private final TicketPriceVerificationService priceVerificationService;

	@Transactional
	public Post create(Long authorId, PostCreateRequest request) {
		User author = userService.getById(authorId);

		if (request.category() == PostCategory.TICKET && request.facePrice() != null) {
			verifyTicketPriceOrThrow(request);
		}

		Post post = new Post(
				author,
				request.category(),
				request.team(),
				request.title(),
				request.content(),
				request.facePrice(),
				request.quantity(),
				request.priceCaptureImageUrl());
		post.replaceImages(request.imageUrls());

		return postRepository.save(post);
	}

	private void verifyTicketPriceOrThrow(PostCreateRequest request) {
		if (request.priceCaptureImageUrl() == null || request.priceCaptureImageUrl().isBlank()) {
			throw ApiException.badRequest("티켓 양도글은 정가 확인용 캡처 이미지가 필요합니다.");
		}
		byte[] captureBytes = fileStorageService.readImage(request.priceCaptureImageUrl());
		VerifyTicketPriceResponse result = priceVerificationService.verify(
				captureBytes, request.priceCaptureImageUrl(), request.facePrice());
		if (!result.valid()) {
			throw ApiException.badRequest(result.message());
		}
	}

	public Page<Post> list(PostCategory category, Team team, Pageable pageable) {
		if (category != null && team != null) {
			return postRepository.findByCategoryAndTeam(category, team, pageable);
		}
		if (category != null) {
			return postRepository.findByCategory(category, pageable);
		}
		if (team != null) {
			return postRepository.findByTeam(team, pageable);
		}
		return postRepository.findAll(pageable);
	}

	public Page<Post> listByAuthor(Long authorId, Pageable pageable) {
		return postRepository.findByAuthorId(authorId, pageable);
	}

	@Transactional
	public Post getAndIncreaseView(Long postId) {
		Post post = getById(postId);
		post.increaseViewCount();
		return post;
	}

	public Post getById(Long postId) {
		return postRepository.findById(postId)
				.orElseThrow(() -> ApiException.notFound("게시글을 찾을 수 없습니다."));
	}

	@Transactional
	public Post update(Long postId, Long requesterId, PostUpdateRequest request) {
		Post post = getById(postId);
		post.verifyAuthor(requesterId);
		post.update(request.title(), request.content(), request.facePrice(), request.quantity(), request.imageUrls());
		return post;
	}

	@Transactional
	public Post changeStatus(Long postId, Long requesterId, PostStatus status) {
		Post post = getById(postId);
		post.verifyAuthor(requesterId);
		post.changeStatus(status);
		return post;
	}

	@Transactional
	public void delete(Long postId, Long requesterId) {
		Post post = getById(postId);
		post.verifyAuthor(requesterId);
		postRepository.delete(post);
	}
}
