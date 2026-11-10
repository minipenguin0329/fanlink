package com.fanlink.domain.comment.dto;

import java.time.LocalDateTime;

import com.fanlink.domain.comment.Comment;

public record CommentResponse(
		Long id,
		Long parentId,
		String content,
		Long authorId,
		String authorNickname,
		String authorProfileImageUrl,
		LocalDateTime createdAt) {

	public static CommentResponse from(Comment comment) {
		return new CommentResponse(
				comment.getId(),
				comment.getParent() != null ? comment.getParent().getId() : null,
				comment.getContent(),
				comment.getAuthor().getId(),
				comment.getAuthor().getNickname(),
				comment.getAuthor().getProfileImageUrl(),
				comment.getCreatedAt());
	}
}
