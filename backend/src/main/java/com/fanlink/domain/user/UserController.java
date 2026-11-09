package com.fanlink.domain.user;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fanlink.domain.user.dto.UpdateProfileRequest;
import com.fanlink.domain.user.dto.UserResponse;
import com.fanlink.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@GetMapping("/me")
	public UserResponse me(@AuthenticationPrincipal UserPrincipal principal) {
		return UserResponse.from(userService.getById(principal.getUserId()));
	}

	@PatchMapping("/me")
	public UserResponse updateMe(@AuthenticationPrincipal UserPrincipal principal,
			@RequestBody UpdateProfileRequest request) {
		return UserResponse.from(userService.updateProfile(principal.getUserId(), request));
	}
}
