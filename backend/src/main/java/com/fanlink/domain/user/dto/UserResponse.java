package com.fanlink.domain.user.dto;

import com.fanlink.domain.user.Team;
import com.fanlink.domain.user.User;

public record UserResponse(
		Long id,
		String nickname,
		String email,
		String profileImageUrl,
		Team favoriteTeam,
		int level) {

	public static UserResponse from(User user) {
		return new UserResponse(
				user.getId(),
				user.getNickname(),
				user.getEmail(),
				user.getProfileImageUrl(),
				user.getFavoriteTeam(),
				user.getLevel());
	}
}
