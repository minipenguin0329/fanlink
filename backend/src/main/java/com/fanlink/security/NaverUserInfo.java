package com.fanlink.security;

import java.util.Map;

public class NaverUserInfo implements OAuth2UserInfo {

	private final Map<String, Object> response;

	@SuppressWarnings("unchecked")
	public NaverUserInfo(Map<String, Object> attributes) {
		this.response = (Map<String, Object>) attributes.getOrDefault("response", Map.of());
	}

	@Override
	public String getProviderId() {
		return String.valueOf(response.get("id"));
	}

	@Override
	public String getNickname() {
		Object name = response.get("name");
		return name != null ? name.toString() : "라이온즈팬";
	}

	@Override
	public String getEmail() {
		Object email = response.get("email");
		return email != null ? email.toString() : null;
	}

	@Override
	public String getProfileImageUrl() {
		Object url = response.get("profile_image");
		return url != null ? url.toString() : null;
	}
}
