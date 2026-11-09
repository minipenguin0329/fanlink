package com.fanlink.security;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fanlink.domain.user.AuthProvider;
import com.fanlink.domain.user.User;
import com.fanlink.domain.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

	private final UserRepository userRepository;

	@Override
	@Transactional
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
		OAuth2User oAuth2User = super.loadUser(userRequest);
		String registrationId = userRequest.getClientRegistration().getRegistrationId();

		OAuth2UserInfo userInfo = switch (registrationId) {
			case "kakao" -> new KakaoUserInfo(oAuth2User.getAttributes());
			case "naver" -> new NaverUserInfo(oAuth2User.getAttributes());
			default -> throw new OAuth2AuthenticationException("지원하지 않는 소셜 로그인입니다: " + registrationId);
		};

		AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());
		User user = userRepository.findByProviderAndProviderId(provider, userInfo.getProviderId())
				.map(existing -> {
					existing.updateProfile(null, null, userInfo.getProfileImageUrl());
					return existing;
				})
				.orElseGet(() -> userRepository.save(new User(
						provider,
						userInfo.getProviderId(),
						userInfo.getNickname(),
						userInfo.getEmail(),
						userInfo.getProfileImageUrl())));

		return UserPrincipal.of(user).withAttributes(oAuth2User.getAttributes());
	}
}
