package com.fanlink.domain.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fanlink.common.ApiException;
import com.fanlink.domain.user.dto.UpdateProfileRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;

	public User getById(Long userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> ApiException.notFound("사용자를 찾을 수 없습니다."));
	}

	@Transactional
	public User updateProfile(Long userId, UpdateProfileRequest request) {
		User user = getById(userId);
		user.updateProfile(request.nickname(), request.favoriteTeam(), request.profileImageUrl());
		return user;
	}
}
