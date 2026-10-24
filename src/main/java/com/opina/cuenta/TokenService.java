package com.opina.cuenta;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final Clock reloj;
    private final Duration duracion;

    public TokenService(JwtEncoder encoder, Clock reloj, @Value("${opina.jwt.duracion:8h}") Duration duracion) {
        this.encoder = encoder;
        this.reloj = reloj;
        this.duracion = duracion;
    }

    /** El token solo lleva el id del usuario: los permisos se consultan en cada petición, así quitarlos surte efecto al momento. */
    public Token emitir(Usuario usuario) {
        Instant ahora = Instant.now(reloj);
        Instant caduca = ahora.plus(duracion);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(JwtConfig.EMISOR)
                .subject(usuario.getId().toString())
                .issuedAt(ahora)
                .expiresAt(caduca)
                .build();
        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
        String valor = encoder.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
        return new Token(valor, caduca);
    }

    public record Token(String token, Instant caducaEn) {
    }
}
