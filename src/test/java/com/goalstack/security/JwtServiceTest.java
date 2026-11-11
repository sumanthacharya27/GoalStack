package com.goalstack.security;

import com.goalstack.entity.User;
import com.goalstack.service.JwtService;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Uses the real token classes (no Spring context, no database). */
class JwtServiceTest {

    private static final String SECRET = "0123456789-0123456789-0123456789";

    private final SecretKey key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    private final JwtService jwtService = new JwtService(new NimbusJwtEncoder(new ImmutableSecret<>(key)), 60);

    @Test
    void tokenContainsTheUserIdAndExpiresAfterTheConfiguredTime() {
        String token = jwtService.createToken(userWithId(42L));

        Jwt jwt = decoderFor(key).decode(token);

        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofHours(1));
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        String token = jwtService.createToken(userWithId(42L));
        SecretKey otherKey = new SecretKeySpec("another-secret-another-secret-123".getBytes(StandardCharsets.UTF_8),
                "HmacSHA256");

        assertThatThrownBy(() -> decoderFor(otherKey).decode(token)).isInstanceOf(JwtException.class);
    }

    private JwtDecoder decoderFor(SecretKey secretKey) {
        return NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();
    }

    private User userWithId(Long id) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
