package com.fanlink.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import org.junit.jupiter.api.Test;

import com.fanlink.common.ApiException;

class MobileOAuthExchangeTest {
    @Test
    void codeRequiresItsVerifierAndCanOnlyBeUsedOnce() throws Exception {
        var exchange = new MobileOAuthExchange();
        String verifier = "a".repeat(64);
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
        String code = exchange.issue(42L, HexFormat.of().formatHex(digest));

        assertThrows(ApiException.class, () -> exchange.consume(code, "b".repeat(64)));
        assertEquals(42L, exchange.consume(code, verifier));
        assertThrows(ApiException.class, () -> exchange.consume(code, verifier));
    }
}

