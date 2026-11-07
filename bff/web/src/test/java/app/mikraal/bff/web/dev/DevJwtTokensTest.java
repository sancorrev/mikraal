package app.mikraal.bff.web.dev;

import static app.mikraal.bff.web.SeedIds.LUCIA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

import app.mikraal.bff.web.directory.Person;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.KeyLengthException;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;

class DevJwtTokensTest {

    private static final String SECRET = "mikraal-dev-only-secret-change-me-0123456789abcdef";
    private static final Instant NOW = Instant.parse("2026-11-07T10:00:00Z");

    @Test
    void issuesTheClaimsTheServiceExpects() throws Exception {
        DevJwtTokens tokens = new DevJwtTokens(new DevProperties(null, SECRET), Clock.fixed(NOW, ZoneOffset.UTC));

        SignedJWT jwt = SignedJWT.parse(tokens.issueFor(new Person(LUCIA, "Lucía Martín Ortega")));

        assertThat(jwt.getHeader().getAlgorithm()).isEqualTo(JWSAlgorithm.HS256);
        assertThat(jwt.verify(new MACVerifier(SECRET.getBytes(StandardCharsets.UTF_8)))).isTrue();
        JWTClaimsSet claims = jwt.getJWTClaimsSet();
        assertThat(claims.getIssuer()).isEqualTo("mikraal-dev");
        assertThat(claims.getAudience()).isEqualTo(List.of("mikraal-activities"));
        assertThat(claims.getSubject()).isEqualTo(LUCIA.toString());
        assertThat(claims.getStringClaim("name")).isEqualTo("Lucía Martín Ortega");
        assertThat(claims.getExpirationTime()).isEqualTo(Date.from(NOW.plusSeconds(3600)));
        assertThat(claims.getClaims().keySet()).containsExactlyInAnyOrder("iss", "aud", "sub", "name", "exp");
    }

    @Test
    void refusesASecretTooShortForHs256() {
        assertThatThrownBy(() -> new DevJwtTokens(new DevProperties(null, "short"), Clock.systemUTC()))
                .isInstanceOf(KeyLengthException.class);
    }
}
