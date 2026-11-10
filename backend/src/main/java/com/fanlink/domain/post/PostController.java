package com.fanlink.domain.post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fanlink.domain.post.dto.PostCreateRequest;
import com.fanlink.domain.post.dto.PostResponse;
import com.fanlink.domain.post.dto.PostUpdateRequest;
import com.fanlink.domain.user.Team;
import com.fanlink.security.UserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

	private final PostService postService;

	@GetMapping
	public Page<PostResponse> list(
			@RequestParam(required = false) PostCategory category,
			@RequestParam(required = false) Team team,
			Pageable pageable) {
		return postService.list(category, team, pageable).map(PostResponse::from);
	}

	@GetMapping("/mine")
	public Page<PostResponse> mine(@AuthenticationPrincipal UserPrincipal principal, Pageable pageable) {
		return postService.listByAuthor(principal.getUserId(), pageable).map(PostResponse::from);
	}

	@GetMapping("/{postId}")
	public PostResponse detail(@PathVariable Long postId) {
		return PostResponse.from(postService.getAndIncreaseView(postId));
	}

	@PostMapping
	public PostResponse create(@AuthenticationPrincipal UserPrincipal principal,
			@Valid @RequestBody PostCreateRequest request) {
		return PostResponse.from(postService.create(principal.getUserId(), request));
	}

	@PatchMapping("/{postId}")
	public PostResponse update(@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long postId, @RequestBody PostUpdateRequest request) {
		return PostResponse.from(postService.update(postId, principal.getUserId(), request));
	}

	@PatchMapping("/{postId}/status")
	public PostResponse changeStatus(@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long postId, @RequestParam PostStatus status) {
		return PostResponse.from(postService.changeStatus(postId, principal.getUserId(), status));
	}

	@DeleteMapping("/{postId}")
	public void delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long postId) {
		postService.delete(postId, principal.getUserId());
	}
}
