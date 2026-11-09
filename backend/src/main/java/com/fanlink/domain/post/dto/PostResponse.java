package com.fanlink.domain.post.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fanlink.domain.post.Post;
import com.fanlink.domain.post.PostCategory;
import com.fanlink.domain.post.PostImage;
import com.fanlink.domain.post.PostStatus;
import com.fanlink.domain.user.Team;

public record PostResponse(
		Long id,
		PostCategory category,
		Team team,
		String title,
		String content,
		Integer facePrice,
		Integer quantity,
		PostStatus status,
		int viewCount,
		List<String> imageUrls,
		Long authorId,
		String authorNickname,
		String authorProfileImageUrl,
		LocalDateTime createdAt) {

	public static PostResponse from(Post post) {
		return new PostResponse(
				post.getId(),
				post.getCategory(),
				post.getTeam(),
				post.getTitle(),
				post.getContent(),
				post.getFacePrice(),
				post.getQuantity(),
				post.getStatus(),
				post.getViewCount(),
				post.getImages().stream().map(PostImage::getImageUrl).toList(),
				post.getAuthor().getId(),
				post.getAuthor().getNickname(),
				post.getAuthor().getProfileImageUrl(),
				post.getCreatedAt());
	}
}
