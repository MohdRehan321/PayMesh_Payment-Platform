package com.paymesh.user_service.util;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JWTUtil is a Spring @Component that receives its signing secret through
 * {@code @Value("${jwt.secret}")} field injection. These tests therefore build the
 * utility directly and set the secret field via ReflectionTestUtils (no Spring context
 * required), keeping them fast and independent of application configuration.
 */
class JWTUtilTest {

    private static final String SECRET_A = "test-secret-A-0123456789-0123456789-xx";
    private static final String SECRET_B = "test-secret-B-0123456789-0123456789-xx";

    private static JWTUtil utilWithSecret(String secret) {
        JWTUtil util = new JWTUtil();
        ReflectionTestUtils.setField(util, "secret", secret);
        return util;
    }

    @Test
    void tokenRoundTripUsesConfiguredSecret() {
        JWTUtil util = utilWithSecret(SECRET_A);
        String token = util.generateToken(9L, "a@b.com", "ROLE_USER");
        assertEquals("a@b.com", util.extractUsername(token));
        assertEquals("ROLE_USER", util.extractRole(token));
        assertEquals(9L, util.extractUserId(token));
    }

    // Regression for the gateway "JWT signature does not match" error:
    // a token signed with one secret must NOT validate under another.
    @Test
    void tokenFromDifferentSecretIsRejected() {
        String token = utilWithSecret(SECRET_A).generateToken(9L, "a@b.com", "ROLE_USER");
        assertFalse(utilWithSecret(SECRET_B).validateToken(token, "a@b.com"));
    }
}
