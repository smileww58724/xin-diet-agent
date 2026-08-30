package com.dietagent.config.jwt;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenProviderTest {

    /** HMAC-SHA256 要求密钥至少 32 字节 */
    private static final String SECRET = "unit-test-secret-0123456789abcdef0123";

    private final JwtTokenProvider provider = new JwtTokenProvider(SECRET, 3_600_000);

    @Test
    @DisplayName("签发的 token 能解析出 userId 且校验通过")
    void generateAndParse() {
        String token = provider.generateToken(42L, "alice");

        assertTrue(provider.validateToken(token));
        assertEquals(42L, provider.getUserIdFromToken(token));
    }

    @Test
    @DisplayName("篡改内容的 token 校验失败")
    void tamperedTokenRejected() {
        String token = provider.generateToken(42L, "alice");
        String tampered = token.substring(0, token.length() - 3) + "abc";

        assertFalse(provider.validateToken(tampered));
    }

    @Test
    @DisplayName("其他密钥签发的 token 校验失败")
    void foreignKeyTokenRejected() {
        JwtTokenProvider other = new JwtTokenProvider("another-secret-0123456789abcdef00", 3_600_000);
        String token = other.generateToken(42L, "alice");

        assertFalse(provider.validateToken(token));
    }

    @Test
    @DisplayName("过期 token 校验失败")
    void expiredTokenRejected() {
        // 用同一密钥直接签发一个已过期的 token（构造器校验不允许负数有效期，故不走 generateToken）
        javax.crypto.SecretKey key =
                io.jsonwebtoken.security.Keys.hmacShaKeyFor(SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        long now = System.currentTimeMillis();
        String expired = io.jsonwebtoken.Jwts.builder()
                .subject("42")
                .issuedAt(new java.util.Date(now - 2000))
                .expiration(new java.util.Date(now - 1000))
                .signWith(key)
                .compact();

        assertFalse(provider.validateToken(expired));
    }

    @Test
    @DisplayName("密钥不足 32 字节时拒绝构造（fail-fast）")
    void weakSecretRejected() {
        assertThrows(IllegalStateException.class,
                () -> new JwtTokenProvider("too-short", 3_600_000));
    }

    @Test
    @DisplayName("密钥为空时拒绝构造（fail-fast）")
    void blankSecretRejected() {
        assertThrows(IllegalStateException.class,
                () -> new JwtTokenProvider("  ", 3_600_000));
    }

    @Test
    @DisplayName("非法格式的 token 抛 JwtException")
    void garbageTokenRejected() {
        assertThrows(JwtException.class, () -> provider.getUserIdFromToken("not-a-jwt"));
    }
}
