package com.paymesh.user_service.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JWTUtilTest {

    private static final String SECRET_A = "test-secret-A-0123456789-0123456789-xx";
    private static final String SECRET_B = "test-secret-B-0123456789-0123456789-xx";

    @Test
    void tokenRoundTripUsesConfiguredSecret() {
        JWTUtil util = new JWTUtil(SECRET_A);
        String token = util.generateToken(9L, "a@b.com", "ROLE_USER");
        assertEquals("a@b.com", util.extractUsername(token));
        assertEquals("ROLE_USER", util.extractRole(token));
        assertEquals(9L, util.extractUserId(token));
    }

    // Regression for the gateway "JWT signature does not match" error:
    // a token signed with one secret must NOT validate under another.
    @Test
    void tokenFromDifferentSecretIsRejected() {
        String token = new JWTUtil(SECRET_A).generateToken(9L, "a@b.com", "ROLE_USER");
        assertFalse(new JWTUtil(SECRET_B).validateToken(token, "a@b.com"));
    }
}