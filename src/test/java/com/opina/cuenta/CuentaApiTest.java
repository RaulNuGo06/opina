package com.opina.cuenta;

import com.jayway.jsonpath.JsonPath;
import com.opina.PruebaDeApi;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class CuentaApiTest extends PruebaDeApi {

    @Autowired
    JwtEncoder encoder;

    @Test
    void alRegistrarseRecibeUnTokenQueSirveParaEntrarAlPanel() throws Exception {
        String email = unico("Ana") + "@Bar.ES";
        var registro = post("/api/auth/registro", """
                {"nombre": "Ana", "email": "%s", "contrasena": "%s"}""".formatted(email, CONTRASENA));
        assertThat(registro).hasStatus(HttpStatus.CREATED);
        String token = JsonPath.read(registro.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.token");

        var yo = mvc.get().uri("/api/yo").header(HttpHeaders.AUTHORIZATION, "Bearer " + token).exchange();
        assertThat(yo).hasStatusOk();
        assertThat(yo).bodyJson().extractingPath("$.email").isEqualTo(email.toLowerCase());
        assertThat(yo).bodyJson().extractingPath("$.negocios").asArray().isEmpty();
    }

    @Test
    void noDejaRegistrarDosVecesElMismoEmailAunqueCambienLasMayusculas() {
        Cuenta existente = nuevaCuenta();

        var respuesta = post("/api/auth/registro", """
                {"nombre": "Otra", "email": "%s", "contrasena": "%s"}""".formatted(existente.email().toUpperCase(), CONTRASENA));

        assertThat(respuesta).hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void elRegistroExigeEmailValidoYContrasenaLarga() {
        var respuesta = post("/api/auth/registro", """
                {"nombre": "Ana", "email": "no-es-un-email", "contrasena": "corta"}""");

        assertThat(respuesta).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(respuesta).bodyJson().extractingPath("$.errores.email").isEqualTo("El email no es válido");
        assertThat(respuesta).bodyJson().extractingPath("$.errores.contrasena")
                .isEqualTo("La contraseña debe tener entre 10 y 72 caracteres");
    }

    @Test
    void entraConEmailYContrasenaCorrectos() {
        Cuenta cuenta = nuevaCuenta();

        var respuesta = post("/api/auth/login", """
                {"email": "%s", "contrasena": "%s"}""".formatted(cuenta.email(), CONTRASENA));

        assertThat(respuesta).hasStatusOk();
        assertThat(respuesta).bodyJson().extractingPath("$.token").asString().isNotBlank();
    }

    @Test
    void elErrorDeLoginNoDelataSiElEmailTieneCuenta() {
        Cuenta cuenta = nuevaCuenta();

        var contrasenaMala = post("/api/auth/login", """
                {"email": "%s", "contrasena": "no-es-esta-contrasena"}""".formatted(cuenta.email()));
        var emailSinCuenta = post("/api/auth/login", """
                {"email": "nadie-%s", "contrasena": "%s"}""".formatted(cuenta.email(), CONTRASENA));

        assertThat(contrasenaMala).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(emailSinCuenta).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(contrasenaMala).bodyJson().extractingPath("$.detail").isEqualTo("Email o contraseña incorrectos");
        assertThat(emailSinCuenta).bodyJson().extractingPath("$.detail").isEqualTo("Email o contraseña incorrectos");
    }

    @Test
    void sinTokenPideIniciarSesion() {
        var respuesta = mvc.get().uri("/api/yo").exchange();

        assertThat(respuesta).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(respuesta).hasHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        assertThat(respuesta).bodyJson().extractingPath("$.detail").isEqualTo("Inicia sesión para ver esto");
    }

    @Test
    void rechazaUnTokenManipulado() {
        Cuenta cuenta = nuevaCuenta();
        String token = cuenta.token();
        // Se cambia un carácter de la firma (no el último, que en base64 puede tener bits sin usar)
        int i = token.length() - 10;
        String manipulado = token.substring(0, i) + (token.charAt(i) == 'a' ? 'b' : 'a') + token.substring(i + 1);

        var respuesta = mvc.get().uri("/api/yo").header(HttpHeaders.AUTHORIZATION, "Bearer " + manipulado).exchange();

        assertThat(respuesta).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(respuesta).bodyJson().extractingPath("$.detail")
                .isEqualTo("La sesión no es válida o ha caducado. Vuelve a iniciar sesión");
    }

    @Test
    void rechazaUnTokenCaducado() {
        Cuenta cuenta = nuevaCuenta();
        Instant ayer = Instant.now().minus(1, ChronoUnit.DAYS);
        String caducado = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder().issuer("opina").subject(cuenta.usuario().getId().toString())
                        .issuedAt(ayer).expiresAt(ayer.plus(1, ChronoUnit.HOURS)).build())).getTokenValue();

        var respuesta = mvc.get().uri("/api/yo").header(HttpHeaders.AUTHORIZATION, "Bearer " + caducado).exchange();

        assertThat(respuesta).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    private MvcTestResult post(String uri, String json) {
        return mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }
}
