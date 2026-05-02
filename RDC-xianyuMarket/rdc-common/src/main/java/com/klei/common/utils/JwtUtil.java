package com.klei.common.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.klei.common.exception.AuthException;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class JwtUtil {

    private static final String SECRET = "RDC-xianyuMarket-SecretKey-2026";
    private static final Algorithm ALGORITHM = Algorithm.HMAC256(SECRET);

    private static final long ACCESS_EXPIRE = 2 * 60 * 60 * 1000;
    private static final long REFRESH_EXPIRE = 7 * 24 * 60 * 60 * 1000;

    public static String createAccessToken(Long userId, List<String> permissions, List<String> roles, Integer vipLevel, Date banEndTime) {
        Date now = new Date();
        Date expire = new Date(now.getTime() + ACCESS_EXPIRE);
        var builder = JWT.create()
                .withClaim("userId", userId)
                .withClaim("perms", String.join(",", permissions))
                .withClaim("roles", String.join(",", roles))
                .withClaim("vipLevel", vipLevel == null ? 0 : vipLevel)
                .withIssuedAt(now)
                .withExpiresAt(expire);
        if (banEndTime != null) {
            builder.withClaim("banEndTime", banEndTime);
        }
        return builder.sign(ALGORITHM);
    }

    public static String createRefreshToken(Long userId) {
        Date now = new Date();
        Date expire = new Date(now.getTime() + REFRESH_EXPIRE);
        return JWT.create()
                .withClaim("userId", userId)
                .withIssuedAt(now)
                .withExpiresAt(expire)
                .sign(ALGORITHM);
    }

    public static String createRefreshToken(Long userId, long expireMillis) {
        Date now = new Date();
        Date expire = new Date(now.getTime() + expireMillis);
        return JWT.create()
                .withClaim("userId", userId)
                .withIssuedAt(now)
                .withExpiresAt(expire)
                .sign(ALGORITHM);
    }

    public static DecodedJWT verify(String token) {
        try {
            return JWT.require(ALGORITHM).build().verify(token);
        } catch (JWTVerificationException e) {
            throw new AuthException(401, "Token无效或已过期");
        }
    }

    public static Long getUserId(String token) {
        return verify(token).getClaim("userId").asLong();
    }

    public static List<String> getPermissions(String token) {
        String perms = verify(token).getClaim("perms").asString();
        if (perms == null || perms.isEmpty()) {
            return List.of();
        }
        return Arrays.asList(perms.split(","));
    }

    public static List<String> getRoles(String token) {
        String roles = verify(token).getClaim("roles").asString();
        if (roles == null || roles.isEmpty()) {
            return List.of();
        }
        return Arrays.asList(roles.split(","));
    }

    public static Integer getVipLevel(String token) {
        return verify(token).getClaim("vipLevel").asInt();
    }

    public static String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}