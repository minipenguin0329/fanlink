package com.fanlink.domain.post.dto;

import java.util.List;

import com.fanlink.domain.post.PostCategory;
import com.fanlink.domain.user.Team;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record PostCreateRequest(
		@NotNull PostCategory category,
		@NotNull Team team,
		@NotBlank String title,
		@NotBlank String content,
		@PositiveOrZero Integer facePrice,
		@Positive Integer quantity,
		List<String> imageUrls,
		/** Required when category=TICKET and facePrice is set; re-verified server-side before save. */
		String priceCaptureImageUrl) {
}
