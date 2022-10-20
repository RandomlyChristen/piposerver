package com.mobilex.piposerver.security;

import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

@Component
public class JwtTokenProvider {

    private static final SignatureAlgorithm SIGN_ALGORITHM = SignatureAlgorithm.HS256;

    @Value("${jwt_secret_key}")
    private String secretKey;

    @Value("${jwt_token_valid_ms}")
    private int tokenValidMs;

    @PostConstruct
    protected void init() {
        secretKey = Base64.getEncoder().encodeToString(secretKey.getBytes());
    }

    public String createToken(@NonNull String userId) {
        Claims claims = Jwts.claims().setSubject(userId);
        Date now = new Date();
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + tokenValidMs))
                .signWith(SIGN_ALGORITHM, secretKey)
                .compact();
    }

    @Nullable
    public String getUserIdFromJwt(@NonNull String jwtString) throws JwtException {
        try {
            return Jwts.parser().setSigningKey(secretKey).parseClaimsJws(jwtString).getBody().getSubject();
        } catch (Exception e) {
            throw new JwtException(null);
        }
    }

    public boolean isValidJwtString(@NonNull String jwtString) throws JwtException {
        try {
            Jws<Claims> claims = Jwts.parser().setSigningKey(secretKey).parseClaimsJws(jwtString);
            return !claims.getBody().getExpiration().before(new Date());
        } catch (Exception e) {
            throw new JwtException(null);
        }
    }
}
