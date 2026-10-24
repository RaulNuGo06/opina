package com.opina.cuenta;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * Tokens JWT firmados con HS256 por la propia API, que es a la vez quien los emite y quien los comprueba.
 * El secreto llega por la variable JWT_SECRET y nunca está en el código.
 */
@Configuration(proxyBeanMethods = false)
public class JwtConfig {

    static final String EMISOR = "opina";
    private static final int BYTES_MINIMOS = 32; // HS256 necesita una clave de al menos 256 bits

    private static final Logger log = LoggerFactory.getLogger(JwtConfig.class);

    @Bean
    SecretKey claveJwt(@Value("${opina.jwt.secreto:}") String secreto) {
        byte[] bytes;
        if (secreto.isBlank()) {
            // Cómodo en desarrollo; en producción las sesiones se perderían en cada reinicio
            log.warn("No hay JWT_SECRET: se usa una clave aleatoria y las sesiones caducarán al reiniciar");
            bytes = new byte[BYTES_MINIMOS];
            new SecureRandom().nextBytes(bytes);
        } else {
            bytes = secreto.getBytes(StandardCharsets.UTF_8);
            if (bytes.length < BYTES_MINIMOS) {
                throw new IllegalStateException("JWT_SECRET debe tener al menos " + BYTES_MINIMOS + " bytes");
            }
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey claveJwt) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(claveJwt));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey claveJwt) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(claveJwt).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(EMISOR)); // firma, caducidad y emisor
        return decoder;
    }
}
