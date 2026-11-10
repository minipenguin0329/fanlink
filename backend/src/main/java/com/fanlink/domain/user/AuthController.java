package com.fanlink.domain.user;

import java.net.URI;
import java.util.Map;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fanlink.common.ApiException;
import com.fanlink.security.JwtTokenProvider;
import com.fanlink.security.MobileOAuthExchange;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final MobileOAuthExchange mobileExchange;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping("/mobile/{provider}")
    public ResponseEntity<Void> startMobileLogin(@PathVariable String provider,
            @RequestParam String challenge, HttpSession session) {
        if (!("kakao".equals(provider) || "naver".equals(provider)) || !challenge.matches("[a-f0-9]{64}")) {
            throw ApiException.badRequest("지원하지 않는 로그인 요청입니다.");
        }
        session.setAttribute(MobileOAuthExchange.SESSION_CHALLENGE, challenge);
        return ResponseEntity.status(302)
            .location(URI.create("/oauth2/authorization/" + provider))
            .cacheControl(CacheControl.noStore()).build();
    }

    public record MobileTokenRequest(String code, String verifier) {}

    @PostMapping("/mobile/token")
    public ResponseEntity<Map<String, String>> exchangeMobileCode(@RequestBody MobileTokenRequest request) {
        Long userId = mobileExchange.consume(request.code(), request.verifier());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(Map.of("token", jwtTokenProvider.generateToken(userId)));
    }

	@GetMapping("/providers")
	public Map<String, String> providers() {
		return Map.of(
				"kakao", "/oauth2/authorization/kakao",
				"naver", "/oauth2/authorization/naver");
	}
}
