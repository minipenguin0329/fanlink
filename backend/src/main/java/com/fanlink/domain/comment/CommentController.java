package com.fanlink.domain.comment;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fanlink.domain.comment.dto.CommentCreateRequest;
import com.fanlink.domain.comment.dto.CommentResponse;
import com.fanlink.security.UserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class CommentController {

	private final CommentService commentService;

	@GetMapping("/api/posts/{postId}/comments")
	public List<CommentResponse> list(@PathVariable Long postId) {
		return commentService.listByPost(postId).stream().map(CommentResponse::from).toList();
	}

	@PostMapping("/api/posts/{postId}/comments")
	public CommentResponse create(@AuthenticationPrincipal UserPrincipal principal,
			@PathVariable Long postId, @Valid @RequestBody CommentCreateRequest request) {
		return CommentResponse.from(commentService.create(postId, principal.getUserId(), request));
	}

	@DeleteMapping("/api/comments/{commentId}")
	public void delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long commentId) {
		commentService.delete(commentId, principal.getUserId());
	}
}
