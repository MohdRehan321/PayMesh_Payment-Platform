package com.paymesh.api_gateway.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class JwtUtilTest {

    @Test
    void validateToken_throws_when_secret_not_set() {
        JwtUtil util = new JwtUtil();
        // explicitly set secret to null to simulate missing configuration
        util.setSecret(null);
        assertThrows(IllegalStateException.class, () -> JwtUtil.validateToken("dummy.token.here"));
    }
}
