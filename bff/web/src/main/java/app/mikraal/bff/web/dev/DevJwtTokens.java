package app.mikraal.bff.web.dev;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Date;

import app.mikraal.bff.web.directory.Person;
import app.mikraal.bff.web.identity.ServiceTokens;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.KeyLengthException;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** HS256 tokens as {@code docs/dev-conventions.md} defines them. Never use outside the dev profile. */
@Component
@Profile("dev")
class DevJwtTokens implements ServiceTokens {

    static final String ISSUER = "mikraal-dev";
    static final String AUDIENCE = "mikraal-activities";
    static final Duration LIFETIME = Duration.ofHours(1);

    private final MACSigner signer;
    private final Clock clock;

    DevJwtTokens(DevProperties properties, Clock clock) throws KeyLengthException {
        this.signer = new MACSigner(properties.jwtSecret().getBytes(StandardCharsets.UTF_8));
        this.clock = clock;
    }

    @Override
    public String issueFor(Person person) {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .audience(AUDIENCE)
                .subject(person.id().toString())
                .claim("name", person.fullName())
                .expirationTime(Date.from(clock.instant().plus(LIFETIME)))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        try {
            jwt.sign(signer);
        } catch (JOSEException e) {
            throw new IllegalStateException("Cannot sign the service token", e);
        }
        return jwt.serialize();
    }
}
