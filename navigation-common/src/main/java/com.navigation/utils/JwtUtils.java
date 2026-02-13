package com.navigation.utils;

import io.jsonwebtoken.*;
import java.util.Date;

/**
 * 处理 JSON Web Token（JWT）相关的操作，比如生成、验证 JWT
 */

public class JwtUtils {
    private static final String SECRET_KEY = "mySecretKey"; // 秘钥
    private static final long EXPIRATION_TIME = 86400000; // 1天有效期

    // 生成 Token，使用 Integer 类型的 userId
    public static String generateToken(Integer userId, String role) {
        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("role", role)            // 存储角色（user / admin）
                .setIssuedAt(new Date())        // 签发时间
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) // 过期时间
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY) // 签名算法
                .compact();
    }

    // 解析 Token（增加异常捕获）
    public static Claims parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(SECRET_KEY)
                    .parseClaimsJws(token)
                    .getBody();
            System.out.println("解析 JWT 成功：" + claims);
            return claims;
        } catch (Exception e) {
            System.err.println("解析 JWT 失败，原始 Token: " + token);
            e.printStackTrace();
        }
        return null; // 解析失败返回 null
    }

    // 获取用户ID（通过 Token 中的 subject 获取用户ID）
    public static Integer getUserId(String token) {
        Claims claims = parseToken(token);
        // 解析成功时，将 userId 转换为 Integer 类型并返回
        return claims != null ? Integer.parseInt(claims.getSubject()) : null;
    }

    // 获取角色
    public static String getUserRole(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.get("role", String.class) : null;
    }

    // 新增：验证 Token 是否有效
    public static boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            // 检查过期时间
            return claims != null && claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;  // 如果解析失败或者有异常，说明 Token 无效
        }
    }
}