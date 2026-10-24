package com.opina.opinion;

import com.opina.AsistenteFalso;
import com.opina.PruebaDeApi;
import com.opina.negocio.Negocio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class InformeApiTest extends PruebaDeApi {

    @Autowired
    ClasificadorOpiniones clasificador;

    @Autowired
    OpinionRepository opiniones;

    @Autowired
    AsistenteFalso asistente;

    Cuenta dueno;
    Negocio negocio;

    @BeforeEach
    void preparar() {
        asistente.reiniciar();
        dueno = nuevaCuenta();
        negocio = nuevoNegocio(dueno);
    }

    @Test
    void cuentaLosTemasYSacaLasUrgentes() {
        guardar(2, "Mucha espera el sábado", 1);
        guardar(1, "Otra vez esperando 30 minutos", 2);
        guardar(1, "Vi una cucaracha junto a la barra", 3);
        guardar(5, null, 4);
        clasificarTodo();

        var informe = informe(dueno, 7);

        assertThat(informe).hasStatusOk();
        assertThat(informe).bodyJson().extractingPath("$.total").isEqualTo(4);
        assertThat(informe).bodyJson().extractingPath("$.porTema.ESPERA").isEqualTo(2);
        assertThat(informe).bodyJson().extractingPath("$.porTema.LIMPIEZA").isEqualTo(1);
        assertThat(informe).bodyJson().extractingPath("$.porTema.PRECIO").isEqualTo(0);
        assertThat(informe).bodyJson().extractingPath("$.urgentes[*].comentario").asArray()
                .containsExactly("Vi una cucaracha junto a la barra");
        assertThat(informe).bodyJson().extractingPath("$.pendientes").isEqualTo(0);
        assertThat(informe).bodyJson().extractingPath("$.iaActiva").isEqualTo(true);
        // Solo las que tienen comentario sirven para el resumen
        assertThat(informe).bodyJson().extractingPath("$.resumen").isEqualTo("Resumen de prueba de 3 opiniones");
    }

    @Test
    void elListadoTambienEnsenaLaClasificacion() {
        guardar(1, "Vi una cucaracha", 1);
        clasificarTodo();

        var listado = mvc.get().uri("/api/negocios/" + negocio.getSlug() + "/opiniones")
                .header(HttpHeaders.AUTHORIZATION, dueno.bearer()).exchange();

        assertThat(listado).bodyJson().extractingPath("$.contenido[0].tema").isEqualTo("LIMPIEZA");
        assertThat(listado).bodyJson().extractingPath("$.contenido[0].urgente").isEqualTo(true);
    }

    @Test
    void elResumenSeReutilizaHastaQueCambianLasOpiniones() {
        guardar(4, "Muy rico todo", 1);
        clasificarTodo();

        informe(dueno, 7);
        informe(dueno, 7);
        assertThat(asistente.resumenes).hasValue(1);

        guardar(2, "Mucha espera", 0);
        informe(dueno, 7); // la opinión nueva aún está pendiente: el resumen ya cambia
        clasificarTodo();
        informe(dueno, 7); // y otra vez cuando la IA la clasifica
        assertThat(asistente.resumenes).hasValue(3);
    }

    @Test
    void soloCuentaLasOpinionesDelPeriodoPedido() {
        guardar(5, "Reciente", 1);
        guardar(1, "De hace un mes", 30);

        assertThat(informe(dueno, 7)).bodyJson().extractingPath("$.total").isEqualTo(1);
        assertThat(informe(dueno, 90)).bodyJson().extractingPath("$.total").isEqualTo(2);
        assertThat(informe(dueno, 365)).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void sinIaDevuelveLasCifrasPeroNoElResumen() {
        guardar(2, "Mucha espera", 1);
        clasificarTodo();
        asistente.activo = false;

        var informe = informe(dueno, 7);

        assertThat(informe).bodyJson().extractingPath("$.iaActiva").isEqualTo(false);
        assertThat(informe).bodyJson().extractingPath("$.porTema.ESPERA").isEqualTo(1);
        assertThat(informe).bodyJson().extractingPath("$.resumen").isNull();
        assertThat(asistente.resumenes).hasValue(0);
    }

    @Test
    void siLaIaFallaElInformeSaleIgualConElUltimoResumen() {
        guardar(4, "Muy rico todo", 1);
        clasificarTodo();
        informe(dueno, 7);
        guardar(2, "Mucha espera", 0);
        asistente.fallar = true;

        var informe = informe(dueno, 7);

        assertThat(informe).hasStatusOk();
        assertThat(informe).bodyJson().extractingPath("$.resumen").isEqualTo("Resumen de prueba de 1 opiniones");
    }

    @Test
    void elInformeEsSoloParaElNegocio() {
        Cuenta deOtroNegocio = nuevaCuenta();
        nuevoNegocio(deOtroNegocio);

        assertThat(informe(deOtroNegocio, 7)).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.get().uri("/api/negocios/" + negocio.getSlug() + "/opiniones/informe").exchange())
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    private void guardar(int puntuacion, String comentario, int haceDias) {
        opiniones.save(new Opinion(negocio, puntuacion, comentario, Instant.now().minus(haceDias, ChronoUnit.DAYS)));
    }

    private void clasificarTodo() {
        while (clasificador.procesarPendientes() > 0) {
            // otra vuelta
        }
    }

    private MvcTestResult informe(Cuenta cuenta, int dias) {
        return mvc.get().uri("/api/negocios/" + negocio.getSlug() + "/opiniones/informe?dias=" + dias)
                .header(HttpHeaders.AUTHORIZATION, cuenta.bearer()).exchange();
    }
}
