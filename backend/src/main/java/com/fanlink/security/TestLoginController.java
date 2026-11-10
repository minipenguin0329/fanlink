package com.fanlink.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fanlink.domain.user.AuthProvider;
import com.fanlink.domain.user.User;
import com.fanlink.domain.user.UserRepository;

/**
 * TEMPORARY TEST-ONLY LOGIN. Disable APP_TEST_LOGIN_ENABLED after integration tests.
 * It does not bypass JWT verification: it issues normal JWTs for two real database users.
 * Use only on a staging/test database, not with real payments or personal user data.
 */
@RestController
@RequestMapping("/api/auth/test-login")
@ConditionalOnProperty(name = "app.test-login.enabled", havingValue = "true")
public class TestLoginController {

    private static final AuthProvider TEST_PROVIDER = AuthProvider.NAVER;
    private static final int MIN_PASSWORD_LENGTH = 32;

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final String passwordA;
    private final String passwordB;

    public TestLoginController(UserRepository userRepository,
            JwtTokenProvider jwtTokenProvider,
            @Value("${app.test-login.password-a:}") String passwordA,
            @Value("${app.test-login.password-b:}") String passwordB) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordA = passwordA;
        this.passwordB = passwordB;
    }

    public record LoginRequest(String account, String password) {}
    public record LoginResponse(String token, Long userId, String nickname) {}

    @PostMapping
    @Transactional
    public LoginResponse login(@RequestBody LoginRequest request) {
        if (passwordA.length() < MIN_PASSWORD_LENGTH || passwordB.length() < MIN_PASSWORD_LENGTH
                || passwordA.equals(passwordB)) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Test login passwords are not configured securely");
        }
        if (request == null || request.password() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid test login credentials");
        }

        final String providerId;
        final String nickname;
        if ("tester-a".equals(request.account()) && constantTimeMatches(request.password(), passwordA)) {
            providerId = "fanlink-internal-test-account-a";
            nickname = "FANLINK Tester A";
        } else if ("tester-b".equals(request.account()) && constantTimeMatches(request.password(), passwordB)) {
            providerId = "fanlink-internal-test-account-b";
            nickname = "FANLINK Tester B";
        } else {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid test login credentials");
        }

        User user = userRepository.findByProviderAndProviderId(TEST_PROVIDER, providerId)
                .orElseGet(() -> userRepository.save(
                        new User(TEST_PROVIDER, providerId, nickname, null, null)));
        return new LoginResponse(jwtTokenProvider.generateToken(user.getId()), user.getId(), user.getNickname());
    }

    private static boolean constantTimeMatches(String input, String expected) {
        return MessageDigest.isEqual(input.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
    }
}
