package com.opina;

import com.opina.cuenta.TokenService;
import com.opina.cuenta.Usuario;
import com.opina.cuenta.UsuarioRepository;
import com.opina.negocio.Miembro;
import com.opina.negocio.Miembro.Rol;
import com.opina.negocio.MiembroRepository;
import com.opina.negocio.Negocio;
import com.opina.negocio.NegocioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.Instant;
import java.util.UUID;

/**
 * Base de los tests de la API: arranca la aplicación entera contra PostgreSQL y ofrece atajos
 * para preparar datos sin pasar por HTTP. Todo lleva un sufijo aleatorio, así cada test
 * trabaja con sus propios datos y no hace falta vaciar la base de datos entre tests.
 */
// El clasificador automático no corre en los tests: cada test lo lanza cuando le hace falta
@SpringBootTest(properties = "opina.ia.intervalo=PT1H")
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, PruebaDeApi.IaDePrueba.class})
public abstract class PruebaDeApi {

    @TestConfiguration(proxyBeanMethods = false)
    static class IaDePrueba {

        @Bean
        @Primary
        AsistenteFalso asistenteFalso() {
            return new AsistenteFalso();
        }
    }

    /** Hash BCrypt de "contrasena-de-prueba", calculado una vez para no gastar tiempo en cada test. */
    protected static final String CONTRASENA = "contrasena-de-prueba";
    private static final String HASH = "{bcrypt}$2a$10$CbnePzUZbH0LbFStOnYdseFjv1LG90NOgaj6wb7bbBOgC2Cx.eSE.";

    @Autowired
    protected MockMvcTester mvc;

    @Autowired
    protected UsuarioRepository usuarios;

    @Autowired
    protected NegocioRepository negocios;

    @Autowired
    protected MiembroRepository miembros;

    @Autowired
    protected TokenService tokens;

    protected record Cuenta(Usuario usuario, String token) {

        public String bearer() {
            return "Bearer " + token;
        }

        public String email() {
            return usuario.getEmail();
        }
    }

    protected Cuenta nuevaCuenta() {
        Usuario usuario = usuarios.save(new Usuario(unico("persona") + "@prueba.es", "Persona de prueba", HASH, Instant.now()));
        return new Cuenta(usuario, tokens.emitir(usuario).token());
    }

    protected Negocio nuevoNegocio(Cuenta dueno) {
        String slug = unico("bar");
        Negocio negocio = negocios.save(new Negocio(slug, "Bar de prueba", "https://g.page/r/" + slug + "/review"));
        miembros.save(new Miembro(negocio, dueno.usuario(), Rol.DUENO, Instant.now()));
        return negocio;
    }

    protected void hacerEmpleado(Negocio negocio, Cuenta cuenta) {
        miembros.save(new Miembro(negocio, cuenta.usuario(), Rol.EMPLEADO, Instant.now()));
    }

    protected static String unico(String prefijo) {
        return prefijo + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
