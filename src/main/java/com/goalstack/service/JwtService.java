package com.goalstack.service;

import com.goalstack.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

/** Creates signed login tokens. The token's subject is the user's id. */
@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final long expirySeconds;

    public JwtService(JwtEncoder jwtEncoder, @Value("${app.jwt.expiry-minutes:120}") long expiryMinutes) {
        this.jwtEncoder = jwtEncoder;
        this.expirySeconds = expiryMinutes * 60;
    }

    public long getExpirySeconds() {
        return expirySeconds;
    }

    public String createToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("goalstack")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expirySeconds))
                .subject(String.valueOf(user.getId()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
