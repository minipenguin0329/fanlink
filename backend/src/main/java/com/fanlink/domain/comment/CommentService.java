package com.fanlink.domain.comment;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fanlink.common.ApiException;
import com.fanlink.domain.comment.dto.CommentCreateRequest;
import com.fanlink.domain.post.Post;
import com.fanlink.domain.post.PostService;
import com.fanlink.domain.user.User;
import com.fanlink.domain.user.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommentService {

	private final CommentRepository commentRepository;
	private final PostService postService;
	private final UserService userService;

	@Transactional
	public Comment create(Long postId, Long authorId, CommentCreateRequest request) {
		Post post = postService.getById(postId);
		User author = userService.getById(authorId);

		Comment parent = null;
		if (request.parentId() != null) {
			parent = commentRepository.findById(request.parentId())
					.orElseThrow(() -> ApiException.notFound("답글을 달 댓글을 찾을 수 없습니다."));
		}

		return commentRepository.save(new Comment(post, author, parent, request.content()));
	}

	public List<Comment> listByPost(Long postId) {
		return commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
	}

	@Transactional
	public void delete(Long commentId, Long requesterId) {
		Comment comment = commentRepository.findById(commentId)
				.orElseThrow(() -> ApiException.notFound("댓글을 찾을 수 없습니다."));
		comment.verifyAuthor(requesterId);
		commentRepository.delete(comment);
	}
}
