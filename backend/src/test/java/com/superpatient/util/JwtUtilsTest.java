package com.superpatient.util;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    private final JwtUtils jwtUtils = new JwtUtils("unittestsecretkey123456");

    @Test
    void generatesTokenThatCanBeParsedBack() {
        String token = jwtUtils.generateToken(claimsOfUserId(42L));

        Claims claims = jwtUtils.parseToken(token);

        assertEquals(42L, claims.get("userId", Long.class));
        assertNotNull(claims.getExpiration());
    }

    @Test
    void rejectsTokenSignedWithAnotherSecret() {
        JwtUtils other = new JwtUtils("anothersecretkey12345678");
        String foreignToken = other.generateToken(claimsOfUserId(1L));

        assertThrows(Exception.class, () -> jwtUtils.parseToken(foreignToken));
    }

    @Test
    void rejectsMalformedToken() {
        assertThrows(Exception.class, () -> jwtUtils.parseToken("not-a-jwt"));
    }

    /** jjwt 0.9.1 会往传入的 claims map 里写 exp，必须是可变 Map */
    private Map<String, Object> claimsOfUserId(long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        return claims;
    }
}
