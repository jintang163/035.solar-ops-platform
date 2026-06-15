package com.solar.ops.admin.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtTokenUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    public String generateToken(Long userId, String username, String role) {
        return generateToken(userId, username, role, null, null, null);
    }

    public String generateToken(Long userId, String username, String role, Integer isAdmin, Long orgId, Integer dataScope) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);
        if (isAdmin != null) {
            claims.put("isAdmin", isAdmin);
        }
        if (orgId != null) {
            claims.put("orgId", orgId);
        }
        if (dataScope != null) {
            claims.put("dataScope", dataScope);
        }
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }

    public Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            return null;
        }
    }

    public Boolean isTokenExpired(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims == null) {
            return true;
        }
        Date expiration = claims.getExpiration();
        return expiration.before(new Date());
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims != null) {
            return Long.valueOf(claims.get("userId").toString());
        }
        return null;
    }

    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims != null) {
            return claims.get("username").toString();
        }
        return null;
    }

    public String getRoleFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims != null) {
            return claims.get("role").toString();
        }
        return null;
    }

    public Integer getIsAdminFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims != null && claims.get("isAdmin") != null) {
            return Integer.valueOf(claims.get("isAdmin").toString());
        }
        return 0;
    }

    public Long getOrgIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims != null && claims.get("orgId") != null) {
            return Long.valueOf(claims.get("orgId").toString());
        }
        return null;
    }

    public Integer getDataScopeFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims != null && claims.get("dataScope") != null) {
            return Integer.valueOf(claims.get("dataScope").toString());
        }
        return 1;
    }

    public Boolean validateToken(String token) {
        return !isTokenExpired(token);
    }
}
