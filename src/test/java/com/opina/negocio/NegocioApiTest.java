package com.opina.negocio;

import com.opina.PruebaDeApi;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;

class NegocioApiTest extends PruebaDeApi {

    @Test
    void quienCreaUnNegocioQuedaComoDueno() {
        Cuenta ana = nuevaCuenta();
        String slug = unico("cafe");

        var respuesta = post(ana, "/api/negocios", """
                {"nombre": "Café Central", "slug": "%s", "urlResenaGoogle": "https://g.page/r/cafe/review"}""".formatted(slug));

        assertThat(respuesta).hasStatus(HttpStatus.CREATED);
        assertThat(respuesta).bodyJson().extractingPath("$.rol").isEqualTo("DUENO");
        assertThat(get(ana, "/api/yo")).bodyJson().extractingPath("$.negocios[0].slug").isEqualTo(slug);
    }

    @Test
    void crearUnNegocioExigeSesion() {
        var respuesta = mvc.post().uri("/api/negocios").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nombre": "Café", "slug": "cafe"}""").exchange();

        assertThat(respuesta).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void noDejaRepetirElEnlaceDeOtroNegocio() {
        Cuenta ana = nuevaCuenta();
        Negocio existente = nuevoNegocio(nuevaCuenta());

        var respuesta = post(ana, "/api/negocios", """
                {"nombre": "Copia", "slug": "%s"}""".formatted(existente.getSlug()));

        assertThat(respuesta).hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void elEnlaceSoloAdmiteMinusculasNumerosYGuiones() {
        var respuesta = post(nuevaCuenta(), "/api/negocios", """
                {"nombre": "Bar", "slug": "Bar La Plaza!"}""");

        assertThat(respuesta).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(respuesta).bodyJson().extractingPath("$.errores.slug").asString().startsWith("El enlace solo puede llevar");
    }

    @Test
    void elDuenoAnadeUnEmpleadoQuePasaAVerLasOpiniones() {
        Cuenta dueno = nuevaCuenta();
        Cuenta empleado = nuevaCuenta();
        Negocio negocio = nuevoNegocio(dueno);
        String opiniones = "/api/negocios/" + negocio.getSlug() + "/opiniones";
        assertThat(get(empleado, opiniones)).hasStatus(HttpStatus.FORBIDDEN);

        var respuesta = post(dueno, miembrosDe(negocio), """
                {"email": "%s"}""".formatted(empleado.email().toUpperCase()));

        assertThat(respuesta).hasStatus(HttpStatus.CREATED);
        assertThat(respuesta).bodyJson().extractingPath("$.rol").isEqualTo("EMPLEADO");
        assertThat(get(empleado, opiniones)).hasStatusOk();
    }

    @Test
    void unEmpleadoNoPuedeGestionarMiembros() {
        Cuenta dueno = nuevaCuenta();
        Cuenta empleado = nuevaCuenta();
        Negocio negocio = nuevoNegocio(dueno);
        hacerEmpleado(negocio, empleado);

        assertThat(get(empleado, miembrosDe(negocio))).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(post(empleado, miembrosDe(negocio), """
                {"email": "%s"}""".formatted(nuevaCuenta().email())))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson().extractingPath("$.detail").isEqualTo("Solo el dueño del negocio puede hacer esto");
    }

    @Test
    void soloSePuedeAnadirAQuienYaTieneCuentaYUnaVez() {
        Cuenta dueno = nuevaCuenta();
        Cuenta empleado = nuevaCuenta();
        Negocio negocio = nuevoNegocio(dueno);
        hacerEmpleado(negocio, empleado);

        assertThat(post(dueno, miembrosDe(negocio), """
                {"email": "sin-cuenta@prueba.es"}""")).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(post(dueno, miembrosDe(negocio), """
                {"email": "%s"}""".formatted(empleado.email()))).hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void alQuitarAUnEmpleadoPierdeElAccesoAlMomentoAunqueSuTokenSigaVigente() {
        Cuenta dueno = nuevaCuenta();
        Cuenta empleado = nuevaCuenta();
        Negocio negocio = nuevoNegocio(dueno);
        hacerEmpleado(negocio, empleado);
        String resumen = "/api/negocios/" + negocio.getSlug() + "/opiniones/resumen";
        assertThat(get(empleado, resumen)).hasStatusOk();

        var respuesta = mvc.delete().uri(miembrosDe(negocio) + "/" + empleado.usuario().getId())
                .header(HttpHeaders.AUTHORIZATION, dueno.bearer()).exchange();

        assertThat(respuesta).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(get(empleado, resumen)).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void noSePuedeQuitarAlDueno() {
        Cuenta dueno = nuevaCuenta();
        Negocio negocio = nuevoNegocio(dueno);

        var respuesta = mvc.delete().uri(miembrosDe(negocio) + "/" + dueno.usuario().getId())
                .header(HttpHeaders.AUTHORIZATION, dueno.bearer()).exchange();

        assertThat(respuesta).hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void elDuenoVeQuienTieneAcceso() {
        Cuenta dueno = nuevaCuenta();
        Cuenta empleado = nuevaCuenta();
        Negocio negocio = nuevoNegocio(dueno);
        hacerEmpleado(negocio, empleado);

        var respuesta = get(dueno, miembrosDe(negocio));

        assertThat(respuesta).hasStatusOk();
        assertThat(respuesta).bodyJson().extractingPath("$[*].email").asArray()
                .containsExactly(dueno.email(), empleado.email());
        assertThat(respuesta).bodyJson().extractingPath("$[*].rol").asArray().containsExactly("DUENO", "EMPLEADO");
    }

    private MvcTestResult get(Cuenta cuenta, String uri) {
        return mvc.get().uri(uri).header(HttpHeaders.AUTHORIZATION, cuenta.bearer()).exchange();
    }

    private MvcTestResult post(Cuenta cuenta, String uri, String json) {
        return mvc.post().uri(uri).header(HttpHeaders.AUTHORIZATION, cuenta.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    private static String miembrosDe(Negocio negocio) {
        return "/api/negocios/" + negocio.getSlug() + "/miembros";
    }
}
