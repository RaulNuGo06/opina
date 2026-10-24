package com.opina.opinion;

import com.opina.PruebaDeApi;
import com.opina.negocio.Negocio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Opinar es público (el cliente no tiene cuenta): los POST de estos tests van sin token a propósito.
 * Leer las opiniones es del panel: hace falta ser dueño o empleado del negocio.
 */
class OpinionApiTest extends PruebaDeApi {

    @Autowired
    OpinionRepository opiniones;

    Cuenta dueno;
    Negocio negocio;

    @BeforeEach
    void crearNegocio() {
        dueno = nuevaCuenta();
        negocio = nuevoNegocio(dueno);
    }

    @Test
    void guardaLaOpinionYDevuelveElEnlaceDeGoogle() {
        var respuesta = opinar("""
                {"puntuacion": 5, "comentario": "  Muy buen trato  "}""");

        assertThat(respuesta).hasStatus(HttpStatus.CREATED);
        assertThat(respuesta).bodyJson().extractingPath("$.urlResenaGoogle").isEqualTo(negocio.getUrlResenaGoogle());

        var guardadas = opiniones.findByNegocioOrderByCreadaEnDesc(negocio, Pageable.unpaged());
        assertThat(guardadas).singleElement().satisfies(o -> {
            assertThat(o.getPuntuacion()).isEqualTo(5);
            assertThat(o.getComentario()).isEqualTo("Muy buen trato");
        });
    }

    @Test
    void tambienOfreceGoogleAQuienPuntuaBajo() {
        // Enseñar el enlace solo a los clientes contentos (review gating) va contra las normas de Google
        var respuesta = opinar("""
                {"puntuacion": 1, "comentario": "La comida llegó fría"}""");

        assertThat(respuesta).hasStatus(HttpStatus.CREATED);
        assertThat(respuesta).bodyJson().extractingPath("$.urlResenaGoogle").isEqualTo(negocio.getUrlResenaGoogle());
    }

    @Test
    void unComentarioEnBlancoSeGuardaComoSinComentario() {
        assertThat(opinar("""
                {"puntuacion": 4, "comentario": "   "}""")).hasStatus(HttpStatus.CREATED);

        assertThat(opiniones.findByNegocioOrderByCreadaEnDesc(negocio, Pageable.unpaged()))
                .singleElement().extracting(Opinion::getComentario).isNull();
    }

    @Test
    void rechazaPuntuacionFueraDeRangoYComentarioDemasiadoLargo() {
        var respuesta = opinar("{\"puntuacion\": 6, \"comentario\": \"" + "x".repeat(1001) + "\"}");

        assertThat(respuesta).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(respuesta).bodyJson().extractingPath("$.errores.puntuacion").isEqualTo("La puntuación va de 1 a 5");
        assertThat(respuesta).bodyJson().extractingPath("$.errores.comentario")
                .isEqualTo("El comentario no puede pasar de 1000 caracteres");
    }

    @Test
    void rechazaUnaOpinionSinPuntuacion() {
        var respuesta = opinar("{}");

        assertThat(respuesta).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(respuesta).bodyJson().extractingPath("$.errores.puntuacion").isEqualTo("La puntuación es obligatoria");
    }

    @Test
    void responde404SiElNegocioNoExiste() {
        var respuesta = mvc.post().uri("/api/negocios/no-existe/opiniones")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"puntuacion": 5}""").exchange();

        assertThat(respuesta).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(respuesta).bodyJson().extractingPath("$.detail")
                .isEqualTo("No existe ningún negocio con el enlace \"no-existe\"");
    }

    @Test
    void listaDeLaMasNuevaALaMasAntiguaYPagina() {
        Instant ahora = Instant.now();
        guardar(negocio, 3, "antigua", ahora.minus(3, ChronoUnit.DAYS));
        guardar(negocio, 5, "nueva", ahora);
        guardar(negocio, 4, "media", ahora.minus(1, ChronoUnit.DAYS));

        var primera = leer(dueno, "?pagina=0&tamano=2");
        assertThat(primera).hasStatusOk();
        assertThat(primera).bodyJson().extractingPath("$.contenido[*].comentario").asArray()
                .containsExactly("nueva", "media");
        assertThat(primera).bodyJson().extractingPath("$.total").isEqualTo(3);

        assertThat(leer(dueno, "?pagina=1&tamano=2"))
                .bodyJson().extractingPath("$.contenido[*].comentario").asArray().containsExactly("antigua");
    }

    @Test
    void rechazaUnTamanoDePaginaExagerado() {
        var respuesta = leer(dueno, "?tamano=500");

        assertThat(respuesta).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(respuesta).bodyJson().extractingPath("$.errores.tamano").isEqualTo("El tamaño va de 1 a 100");
    }

    @Test
    void elResumenCuentaSoloLasOpinionesDeSuNegocio() {
        Negocio otro = nuevoNegocio(nuevaCuenta());
        guardar(negocio, 5, null, Instant.now());
        guardar(negocio, 4, null, Instant.now());
        guardar(negocio, 1, null, Instant.now());
        guardar(otro, 1, "no debe contar", Instant.now());

        var respuesta = leer(dueno, "/resumen");

        assertThat(respuesta).hasStatusOk();
        assertThat(respuesta).bodyJson().isEqualTo("""
                {"total": 3, "media": 3.3, "porPuntuacion": {"5": 1, "4": 1, "3": 0, "2": 0, "1": 1}}""");
        assertThat(leer(dueno, "")).bodyJson().extractingPath("$.contenido[*].comentario").asArray()
                .doesNotContain("no debe contar");
    }

    @Test
    void elEmpleadoTambienVeLasOpiniones() {
        Cuenta empleado = nuevaCuenta();
        hacerEmpleado(negocio, empleado);

        assertThat(leer(empleado, "")).hasStatusOk();
        assertThat(leer(empleado, "/resumen")).hasStatusOk();
    }

    @Test
    void leerLasOpinionesExigeSesion() {
        assertThat(mvc.get().uri(opinionesDe(negocio)).exchange()).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.get().uri(opinionesDe(negocio) + "/resumen").exchange()).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void quienLlevaOtroNegocioNoVeEstasOpiniones() {
        Cuenta deOtroNegocio = nuevaCuenta();
        nuevoNegocio(deOtroNegocio);
        guardar(negocio, 2, "privada", Instant.now());

        var listado = leer(deOtroNegocio, "");

        assertThat(listado).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(listado).bodyJson().extractingPath("$.detail").isEqualTo("No tienes acceso a este negocio");
        assertThat(leer(deOtroNegocio, "/resumen")).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void devuelveLosDatosPublicosDelNegocioSinSesion() {
        assertThat(mvc.get().uri("/api/negocios/" + negocio.getSlug()))
                .hasStatusOk()
                .bodyJson().isEqualTo("{\"slug\": \"%s\", \"nombre\": \"Bar de prueba\"}".formatted(negocio.getSlug()));
    }

    private MvcTestResult opinar(String json) {
        return mvc.post().uri(opinionesDe(negocio)).contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    private MvcTestResult leer(Cuenta cuenta, String sufijo) {
        return mvc.get().uri(opinionesDe(negocio) + sufijo).header(HttpHeaders.AUTHORIZATION, cuenta.bearer()).exchange();
    }

    private void guardar(Negocio n, int puntuacion, String comentario, Instant fecha) {
        opiniones.save(new Opinion(n, puntuacion, comentario, fecha));
    }

    private static String opinionesDe(Negocio n) {
        return "/api/negocios/" + n.getSlug() + "/opiniones";
    }
}
