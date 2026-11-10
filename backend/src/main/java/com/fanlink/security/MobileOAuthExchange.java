package com.fanlink.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.fanlink.common.ApiException;

@Component
public class MobileOAuthExchange {
    public static final String SESSION_CHALLENGE = MobileOAuthExchange.class.getName() + ".challenge";
    public static final String CALLBACK = "fanlink://oauth/callback";

    private record Pending(Long userId, String challenge, Instant expiresAt) {}
    private final Map<String, Pending> pending = new HashMap<>();

    public synchronized String issue(Long userId, String challenge) {
        Instant now = Instant.now();
        pending.values().removeIf(entry -> !entry.expiresAt().isAfter(now));
        if (pending.size() >= 4096) {
            throw ApiException.badRequest("로그인 요청이 많습니다. 잠시 후 다시 시도해 주세요.");
        }
        String code = UUID.randomUUID().toString();
        pending.put(code, new Pending(userId, challenge, now.plusSeconds(90)));
        return code;
    }

    public synchronized Long consume(String code, String verifier) {
        Pending entry = pending.get(code);
        if (entry == null || !entry.expiresAt().isAfter(Instant.now())
                || verifier == null || !verifier.matches("[a-f0-9]{64}")
                || !MessageDigest.isEqual(HexFormat.of().parseHex(entry.challenge()), sha256(verifier))) {
            throw ApiException.badRequest("로그인 요청이 만료되었거나 유효하지 않습니다.");
        }
        pending.remove(code);
        return entry.userId();
    }

    private byte[] sha256(String verifier) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}

