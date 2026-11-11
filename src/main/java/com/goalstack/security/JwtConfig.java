package com.goalstack.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/** Creates the key used to sign login tokens, plus the objects that create and check tokens. */
@Configuration
public class JwtConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtConfig.class);

    @Bean
    public SecretKey jwtSecretKey(@Value("${app.jwt.secret:}") String secret) {
        byte[] bytes;
        if (secret == null || secret.isBlank()) {
            bytes = new byte[32];
            new SecureRandom().nextBytes(bytes);
            log.warn("JWT_SECRET is not set. Using a random secret: all users are logged out when the app restarts.");
        } else {
            bytes = secret.getBytes(StandardCharsets.UTF_8);
            if (bytes.length < 32) {
                throw new IllegalStateException("JWT_SECRET must be at least 32 characters long.");
            }
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
        return NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
