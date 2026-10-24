package com.opina.negocio;

import com.opina.cuenta.Usuario;
import com.opina.cuenta.UsuarioRepository;
import com.opina.negocio.Miembro.Rol;
import com.opina.negocio.NegocioDtos.MiembroVista;
import com.opina.negocio.NegocioDtos.NegocioDelPanel;
import com.opina.negocio.NegocioDtos.NegocioPublico;
import com.opina.negocio.NegocioDtos.NuevoMiembro;
import com.opina.negocio.NegocioDtos.NuevoNegocio;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class NegocioService {

    private final NegocioRepository negocios;
    private final MiembroRepository miembros;
    private final UsuarioRepository usuarios;
    private final ControlAcceso acceso;
    private final Clock reloj;

    public NegocioService(NegocioRepository negocios, MiembroRepository miembros, UsuarioRepository usuarios,
                          ControlAcceso acceso, Clock reloj) {
        this.negocios = negocios;
        this.miembros = miembros;
        this.usuarios = usuarios;
        this.acceso = acceso;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public NegocioPublico verPublico(String slug) {
        Negocio negocio = negocios.findBySlug(slug).orElseThrow(() -> new NegocioNoEncontradoException(slug));
        return new NegocioPublico(negocio.getSlug(), negocio.getNombre());
    }

    /** Quien crea el negocio queda como su dueño. */
    @Transactional
    public NegocioDelPanel crear(UUID usuarioId, NuevoNegocio nuevo) {
        if (negocios.findBySlug(nuevo.slug()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ese enlace ya lo usa otro negocio. Prueba con otro");
        }
        Usuario dueno = usuarios.getReferenceById(usuarioId);
        Negocio negocio = negocios.save(new Negocio(nuevo.slug(), nuevo.nombre().strip(), nuevo.urlResenaGoogle()));
        miembros.save(new Miembro(negocio, dueno, Rol.DUENO, Instant.now(reloj)));
        return new NegocioDelPanel(negocio.getSlug(), negocio.getNombre(), negocio.getUrlResenaGoogle(), Rol.DUENO);
    }

    @Transactional(readOnly = true)
    public List<MiembroVista> miembros(String slug, UUID usuarioId) {
        Negocio negocio = acceso.exigirDueno(slug, usuarioId);
        return miembros.findByNegocioOrderByCreadoEn(negocio).stream().map(MiembroVista::de).toList();
    }

    /** La persona tiene que haberse registrado antes; así nadie recibe una contraseña elegida por otro. */
    @Transactional
    public MiembroVista anadirEmpleado(String slug, UUID usuarioId, NuevoMiembro nuevo) {
        Negocio negocio = acceso.exigirDueno(slug, usuarioId);
        Usuario empleado = usuarios.findByEmail(Usuario.normalizarEmail(nuevo.email()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No hay ninguna cuenta con ese email. Pídele que se registre primero"));
        if (miembros.existsByNegocioAndUsuarioId(negocio, empleado.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esa persona ya tiene acceso a este negocio");
        }
        return MiembroVista.de(miembros.save(new Miembro(negocio, empleado, Rol.EMPLEADO, Instant.now(reloj))));
    }

    @Transactional
    public void quitarMiembro(String slug, UUID usuarioId, UUID aQuitar) {
        Negocio negocio = acceso.exigirDueno(slug, usuarioId);
        Miembro miembro = miembros.findByNegocioAndUsuarioId(negocio, aQuitar)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Esa persona no tiene acceso a este negocio"));
        if (miembro.getRol() == Rol.DUENO) {
            // Un negocio sin dueño se quedaría sin nadie que pueda gestionarlo
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede quitar al dueño del negocio");
        }
        miembros.delete(miembro);
    }
}
