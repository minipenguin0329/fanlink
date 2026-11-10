package com.fanlink.domain.user.dto;

import com.fanlink.domain.user.Team;

public record UpdateProfileRequest(String nickname, Team favoriteTeam, String profileImageUrl) {
}
