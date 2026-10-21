package com.superpatient.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;

@Component
public class JwtUtils {

    private static final long EXPIRE = 12 * 3600 * 1000L; // 12 小时

    /** 密钥由配置提供（app.jwt.secret），不再硬编码在源码中 */
    private final String secret;

    public JwtUtils(@Value("${app.jwt.secret}") String secret) {
        this.secret = secret;
    }

    /**
     * 生成 JWT 令牌
     */
    public String generateToken(Map<String, Object> claims) {
        return Jwts.builder()
                .setClaims(claims)
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRE))
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }

    /**
     * 解析 JWT 令牌
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();
    }
}
