package com.fanlink.domain.user;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Login itself happens via Spring Security's own /oauth2/authorization/{kakao|naver}
 * redirect flow (see SecurityConfig + OAuth2SuccessHandler); this controller only
 * exposes the static info the frontend needs to kick that flow off.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	@GetMapping("/providers")
	public Map<String, String> providers() {
		return Map.of(
				"kakao", "/oauth2/authorization/kakao",
				"naver", "/oauth2/authorization/naver");
	}
}
