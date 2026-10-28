package com.youlai.boot.framework.security.token;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.youlai.boot.framework.security.config.SecurityProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * JWT 只携带会话引用，角色和登录来源以服务端会话为准
 *
 * @author Ray.Hao
 * @since 4.13.0
 */
@Component
@ConditionalOnProperty(prefix = "security.token", name = "type", havingValue = "jwt", matchIfMissing = true)
public class JwtAccessTokenCodec implements AccessTokenCodec {

    private static final String ISSUER = "youlai-boot";
    private static final String AUDIENCE = "youlai-api";
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;

    public JwtAccessTokenCodec(SecurityProperties properties, Clock clock) {
        var secret = new SecretKeySpec(properties.getToken().getJwt().getSecretKey()
                .getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        encoder = new NimbusJwtEncoder(new ImmutableSecret<>(secret));
        var nimbus = NimbusJwtDecoder.withSecretKey(secret).macAlgorithm(MacAlgorithm.HS256)
                .validateType(false).build();
        var timestamp = new JwtTimestampValidator(Duration.ZERO);
        timestamp.setClock(clock);
        nimbus.setJwtValidator(new DelegatingOAuth2TokenValidator<>(timestamp,
                new JwtIssuerValidator(ISSUER), new JwtClaimValidator<List<String>>("aud",
                audiences -> audiences != null && audiences.contains(AUDIENCE))));
        decoder = nimbus;
    }

    @Override
    public IssuedToken issue(String sessionId, Long userId, Instant issuedAt, Instant expiresAt) {
        var claims = JwtClaimsSet.builder().issuer(ISSUER).audience(List.of(AUDIENCE))
                .subject(userId.toString()).id(SecureTokenGenerator.generate())
                .issuedAt(issuedAt).expiresAt(expiresAt)
                .claim("sid", sessionId).claim("token_use", "access").build();
        var header = JwsHeader.with(MacAlgorithm.HS256).type("at+jwt").build();
        String value = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(value, new Reference(sessionId, userId, expiresAt), null);
    }

    @Override
    public Reference verify(String token) {
        if (token == null || token.length() > 4096) {
            throw invalid();
        }
        try {
            Jwt jwt = decoder.decode(token);
            String sessionId = jwt.getClaimAsString("sid");
            if (!"at+jwt".equals(jwt.getHeaders().get("typ"))
                    || !"access".equals(jwt.getClaimAsString("token_use"))
                    || sessionId == null || !sessionId.matches("[A-Za-z0-9_-]{43}")
                    || jwt.getExpiresAt() == null || jwt.getIssuedAt() == null) {
                throw invalid();
            }
            return new Reference(sessionId, Long.valueOf(jwt.getSubject()), jwt.getExpiresAt());
        } catch (JwtException | IllegalArgumentException exception) {
            throw invalid();
        }
    }

    /**
     * 创建 JWT 凭证无效异常
     */
    private static BadCredentialsException invalid() {
        return new BadCredentialsException("Invalid access token");
    }
}
