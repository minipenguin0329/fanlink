package com.fanlink.security;

public interface OAuth2UserInfo {
	String getProviderId();
	String getNickname();
	String getEmail();
	String getProfileImageUrl();
}
