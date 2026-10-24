package com.opina.opinion;

import com.opina.TestcontainersConfiguration;
import com.opina.negocio.Negocio;
import com.opina.negocio.NegocioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba la API de punta a punta contra PostgreSQL: controlador, validación, servicio, JPA y migraciones.
 * Cada test crea su propio negocio, así que no dependen unos de otros ni hace falta limpiar la base de datos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OpinionApiTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    NegocioRepository negocios;

    @Autowired
    OpinionRepository opiniones;

    Negocio negocio;

    @BeforeEach
    void crearNegocio() {
        negocio = nuevoNegocio();
    }

    @Test
    void guardaLaOpinionYDevuelveElEnlaceDeGoogle() {
        var respuesta = mvc.post().uri(opinionesDe(negocio))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"puntuacion": 5, "comentario": "  Muy buen trato  "}""").exchange();

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
        var respuesta = mvc.post().uri(opinionesDe(negocio))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"puntuacion": 1, "comentario": "La comida llegó fría"}""").exchange();

        assertThat(respuesta).hasStatus(HttpStatus.CREATED);
        assertThat(respuesta).bodyJson().extractingPath("$.urlResenaGoogle").isEqualTo(negocio.getUrlResenaGoogle());
    }

    @Test
    void unComentarioEnBlancoSeGuardaComoSinComentario() {
        mvc.post().uri(opinionesDe(negocio))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"puntuacion": 4, "comentario": "   "}""")
                .assertThat().hasStatus(HttpStatus.CREATED);

        assertThat(opiniones.findAll()).filteredOn(o -> o.getNegocio().getId().equals(negocio.getId()))
                .singleElement().extracting(Opinion::getComentario).isNull();
    }

    @Test
    void rechazaPuntuacionFueraDeRangoYComentarioDemasiadoLargo() {
        var respuesta = mvc.post().uri(opinionesDe(negocio))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"puntuacion\": 6, \"comentario\": \"" + "x".repeat(1001) + "\"}").exchange();

        assertThat(respuesta).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(respuesta).bodyJson().extractingPath("$.errores.puntuacion").isEqualTo("La puntuación va de 1 a 5");
        assertThat(respuesta).bodyJson().extractingPath("$.errores.comentario")
                .isEqualTo("El comentario no puede pasar de 1000 caracteres");
    }

    @Test
    void rechazaUnaOpinionSinPuntuacion() {
        var respuesta = mvc.post().uri(opinionesDe(negocio))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}").exchange();

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

        var primera = mvc.get().uri(opinionesDe(negocio) + "?pagina=0&tamano=2").exchange();
        assertThat(primera).hasStatusOk();
        assertThat(primera).bodyJson().extractingPath("$.contenido[*].comentario").asArray()
                .containsExactly("nueva", "media");
        assertThat(primera).bodyJson().extractingPath("$.total").isEqualTo(3);

        assertThat(mvc.get().uri(opinionesDe(negocio) + "?pagina=1&tamano=2"))
                .bodyJson().extractingPath("$.contenido[*].comentario").asArray().containsExactly("antigua");
    }

    @Test
    void rechazaUnTamanoDePaginaExagerado() {
        var respuesta = mvc.get().uri(opinionesDe(negocio) + "?tamano=500").exchange();

        assertThat(respuesta).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(respuesta).bodyJson().extractingPath("$.errores.tamano").isEqualTo("El tamaño va de 1 a 100");
    }

    @Test
    void elResumenCuentaSoloLasOpinionesDeSuNegocio() {
        Negocio otro = nuevoNegocio();
        guardar(negocio, 5, null, Instant.now());
        guardar(negocio, 4, null, Instant.now());
        guardar(negocio, 1, null, Instant.now());
        guardar(otro, 1, "no debe contar", Instant.now());

        var respuesta = mvc.get().uri(opinionesDe(negocio) + "/resumen").exchange();

        assertThat(respuesta).hasStatusOk();
        assertThat(respuesta).bodyJson().isEqualTo("""
                {"total": 3, "media": 3.3, "porPuntuacion": {"5": 1, "4": 1, "3": 0, "2": 0, "1": 1}}""");
        assertThat(mvc.get().uri(opinionesDe(negocio)))
                .bodyJson().extractingPath("$.contenido[*].comentario").asArray().doesNotContain("no debe contar");
    }

    @Test
    void devuelveLosDatosPublicosDelNegocio() {
        assertThat(mvc.get().uri("/api/negocios/" + negocio.getSlug()))
                .hasStatusOk()
                .bodyJson().isEqualTo("{\"slug\": \"%s\", \"nombre\": \"Bar de prueba\"}".formatted(negocio.getSlug()));
    }

    private Negocio nuevoNegocio() {
        String slug = "bar-" + UUID.randomUUID().toString().substring(0, 8);
        return negocios.save(new Negocio(slug, "Bar de prueba", "https://g.page/r/" + slug + "/review"));
    }

    private void guardar(Negocio n, int puntuacion, String comentario, Instant fecha) {
        opiniones.save(new Opinion(n, puntuacion, comentario, fecha));
    }

    private static String opinionesDe(Negocio n) {
        return "/api/negocios/" + n.getSlug() + "/opiniones";
    }
}
