package com.homeverse.property.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String secretKey;

    // Lấy ID người dùng (Dùng để định danh trong Controller)
    public String extractUserId(String token) {
        return extractClaim(token, claims -> {
            Object userId = claims.get("userId", Object.class);
            if (userId == null) {
                throw new RuntimeException("JWT không có claim userId");
            }
            return userId.toString();
        });
    }
    public String extractRole(String token) {
        return extractClaim(token, claims -> {
            String role = claims.get("role", String.class);
            if (role == null) {
                throw new RuntimeException("JWT không có role");
            }
            return role;
        });
    }

    // Lấy Email/Username (Dùng nếu cần hiển thị tên)
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public boolean isTokenValidBasic(String token) {
    try {
        Claims claims = extractAllClaims(token);
        System.out.println("JWT CLAIMS = " + claims);
        System.out.println("JWT EXP = " + claims.getExpiration());
        return !claims.getExpiration().before(new Date());
    } catch (Exception e) {
        e.printStackTrace();
        return false;
    }
}
    
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }
}