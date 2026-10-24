package com.opina.negocio;

import com.opina.negocio.Miembro.Rol;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Decide quién puede ver o gestionar cada negocio. Todos los servicios del panel pasan por aquí,
 * así los permisos están en un solo sitio y se pueden leer de una vez.
 */
@Service
public class ControlAcceso {

    private final NegocioRepository negocios;
    private final MiembroRepository miembros;

    public ControlAcceso(NegocioRepository negocios, MiembroRepository miembros) {
        this.negocios = negocios;
        this.miembros = miembros;
    }

    /** Dueño o empleado: puede ver las opiniones. */
    @Transactional(readOnly = true)
    public Negocio exigirMiembro(String slug, UUID usuarioId) {
        return buscarMiembro(slug, usuarioId).getNegocio();
    }

    /** Solo el dueño: puede decidir quién más entra al panel. */
    @Transactional(readOnly = true)
    public Negocio exigirDueno(String slug, UUID usuarioId) {
        Miembro miembro = buscarMiembro(slug, usuarioId);
        if (miembro.getRol() != Rol.DUENO) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el dueño del negocio puede hacer esto");
        }
        return miembro.getNegocio();
    }

    private Miembro buscarMiembro(String slug, UUID usuarioId) {
        Negocio negocio = negocios.findBySlug(slug).orElseThrow(() -> new NegocioNoEncontradoException(slug));
        return miembros.findByNegocioAndUsuarioId(negocio, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a este negocio"));
    }
}
