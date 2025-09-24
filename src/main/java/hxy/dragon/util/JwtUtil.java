package hxy.dragon.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT utility class for token generation and validation
 *
 * @author houxiaoyi
 */
@Slf4j
@Component
public class JwtUtil {

    private final SecretKey SECRET_KEY = Keys.hmacShaKeyFor("mySecretKeyForJwtTokenGenerationAndValidation12345".getBytes(StandardCharsets.UTF_8));

    @Value("${jwt.accessExpiration:7200000}") // default 2 hours
    private Long accessExpiration;

    @Value("${jwt.refreshExpiration:2592000000}") // default 30 days
    private Long refreshExpiration;

    /**
     * Generate token for user
     */
    public String generateAccessToken(Long userId, String username, String email) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("email", email);
        claims.put("tokenType", "access");
        return createToken(claims, username, accessExpiration);
    }

    /**
     * Generate refresh token for user
     */
    public String generateRefreshToken(Long userId, String username, String email) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("email", email);
        claims.put("tokenType", "refresh");
        return createToken(claims, username, refreshExpiration);
    }

    /**
     * Create token with claims and subject
     */
    private String createToken(Map<String, Object> claims, String subject, Long expiresInMs) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiresInMs))
                .signWith(SECRET_KEY)
                .compact();
    }

    /**
     * Extract username from token
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract user ID from token
     */
    public Long extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        return Long.valueOf(claims.get("userId").toString());
    }

    /**
     * Extract email from token
     */
    public String extractEmail(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("email").toString();
    }

    /**
     * Extract expiration date from token
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extract specific claim from token
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extract all claims from token
     */
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(SECRET_KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.warn("Error extracting claims from token", e);
            throw new RuntimeException("Invalid token");
        }
    }

    /**
     * Check if token is expired
     */
    public Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Validate token
     */
    public Boolean validateToken(String token, String username) {
        try {
            final String extractedUsername = extractUsername(token);
            return (extractedUsername.equals(username) && !isTokenExpired(token));
        } catch (Exception e) {
            log.error("Token validation failed", e);
            return false;
        }
    }

    /**
     * Validate access token (ensure tokenType is access)
     */
    public Boolean validateAccessToken(String token, String username) {
        try {
            final String extractedUsername = extractUsername(token);
            final String tokenType = extractTokenType(token);
            return ("access".equals(tokenType) && extractedUsername.equals(username) && !isTokenExpired(token));
        } catch (Exception e) {
            log.error("Access token validation failed", e);
            return false;
        }
    }

    /**
     * Validate refresh token (ensure tokenType is refresh)
     */
    public Boolean validateRefreshToken(String token, String username) {
        try {
            final String extractedUsername = extractUsername(token);
            final String tokenType = extractTokenType(token);
            return ("refresh".equals(tokenType) && extractedUsername.equals(username) && !isTokenExpired(token));
        } catch (Exception e) {
            log.error("Refresh token validation failed", e);
            return false;
        }
    }

    /**
     * Extract token type (access/refresh)
     */
    public String extractTokenType(String token) {
        Claims claims = extractAllClaims(token);
        Object type = claims.get("tokenType");
        return type == null ? null : type.toString();
    }

    /**
     * Check if token is valid (not expired and properly formatted)
     */
    public Boolean isTokenValid(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            log.error("Token validation failed", e);
            return false;
        }
    }
}