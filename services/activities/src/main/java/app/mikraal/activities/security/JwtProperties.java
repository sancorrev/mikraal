package app.mikraal.activities.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** The development login's HS256 tokens (docs/dev-conventions.md). Replaced by the identity provider later. */
@ConfigurationProperties("mikraal.jwt")
public record JwtProperties(String secret, String issuer, String audience) {
}
