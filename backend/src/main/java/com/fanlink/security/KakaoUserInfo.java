package com.fanlink.security;

import java.util.Map;

public class KakaoUserInfo implements OAuth2UserInfo {

	private final Map<String, Object> attributes;

	public KakaoUserInfo(Map<String, Object> attributes) {
		this.attributes = attributes;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> account() {
		return (Map<String, Object>) attributes.getOrDefault("kakao_account", Map.of());
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> profile() {
		return (Map<String, Object>) account().getOrDefault("profile", Map.of());
	}

	@Override
	public String getProviderId() {
		return String.valueOf(attributes.get("id"));
	}

	@Override
	public String getNickname() {
		Object nickname = profile().get("nickname");
		return nickname != null ? nickname.toString() : "라이온즈팬";
	}

	@Override
	public String getEmail() {
		Object email = account().get("email");
		return email != null ? email.toString() : null;
	}

	@Override
	public String getProfileImageUrl() {
		Object url = profile().get("profile_image_url");
		return url != null ? url.toString() : null;
	}
}
