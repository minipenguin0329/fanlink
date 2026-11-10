package com.fanlink.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.fanlink.domain.user.AuthProvider;
import com.fanlink.domain.user.User;
import com.fanlink.domain.user.UserRepository;

class TestLoginControllerTest {
    private static final String A = "A".repeat(40);
    private static final String B = "B".repeat(40);

    @Test
    void existingTestUserGetsRealJwt() {
        UserRepository users = mock(UserRepository.class);
        JwtTokenProvider jwt = mock(JwtTokenProvider.class);
        User user = mock(User.class);
        when(user.getId()).thenReturn(42L);
        when(user.getNickname()).thenReturn("FANLINK Tester A");
        when(users.findByProviderAndProviderId(AuthProvider.NAVER, "fanlink-internal-test-account-a"))
                .thenReturn(Optional.of(user));
        when(jwt.generateToken(42L)).thenReturn("signed-jwt");

        TestLoginController controller = new TestLoginController(users, jwt, A, B);
        var result = controller.login(new TestLoginController.LoginRequest("tester-a", A));
        assertEquals("signed-jwt", result.token());
        assertEquals(42L, result.userId());
        verify(jwt).generateToken(42L);
    }

    @Test
    void wrongPasswordNeverCreatesUserOrToken() {
        var controller = new TestLoginController(mock(UserRepository.class),
                mock(JwtTokenProvider.class), A, B);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.login(new TestLoginController.LoginRequest("tester-a", "bad password")));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void weakServerPasswordsDisableFeature() {
        var controller = new TestLoginController(mock(UserRepository.class),
                mock(JwtTokenProvider.class), "short", B);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.login(new TestLoginController.LoginRequest("tester-a", "short")));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
    }
}
