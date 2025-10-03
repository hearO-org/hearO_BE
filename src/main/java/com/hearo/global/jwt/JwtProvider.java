package com.hearo.global.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Component
public class JwtProvider {

    @Value("${security.jwt.secret}") private String secretB64;
    @Value("${security.jwt.access-token-validity}")   private long accessTtl;
    @Value("${security.jwt.refresh-token-validity}") private long refreshTtl;

    private Key key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretB64));
    }

    public String createAccess(Long userId, int tokenVer, List<String> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("typ", "ACCESS")
                .claim("ver", tokenVer)
                .claim("roles", roles)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(accessTtl)))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String createRefresh(Long userId, int tokenVer, String jti) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("typ", "REFRESH")
                .claim("ver", tokenVer)
                .claim("jti", jti)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(refreshTtl)))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    public JwtPayload verify(String token) {
        var jws = Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(token);
        var c = jws.getBody();
        Long uid = Long.valueOf(c.getSubject());
        String typ = (String) c.get("typ");
        Integer ver = (Integer) c.get("ver");
        String jti = (String) c.get("jti");
        return new JwtPayload(uid, typ, ver, jti);
    }

    public long accessTtlSeconds()  { return accessTtl; }
    public long refreshTtlSeconds() { return refreshTtl; }
}
