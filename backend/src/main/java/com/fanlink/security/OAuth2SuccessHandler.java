package com.fanlink.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

	private final JwtTokenProvider jwtTokenProvider;

	@Value("${app.frontend-url}")
	private String frontendUrl;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException {
		UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
		String token = jwtTokenProvider.generateToken(principal.getUserId());

		String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth/callback")
				.queryParam("token", token)
				.build().toUriString();

		getRedirectStrategy().sendRedirect(request, response, targetUrl);
	}
}
