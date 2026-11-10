package com.fanlink.domain.post.dto;

import java.util.List;

public record PostUpdateRequest(
		String title,
		String content,
		Integer facePrice,
		Integer quantity,
		List<String> imageUrls) {
}
