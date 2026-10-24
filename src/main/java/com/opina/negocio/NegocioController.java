package com.opina.negocio;

import com.opina.negocio.NegocioDtos.MiembroVista;
import com.opina.negocio.NegocioDtos.NegocioDelPanel;
import com.opina.negocio.NegocioDtos.NegocioPublico;
import com.opina.negocio.NegocioDtos.NuevoMiembro;
import com.opina.negocio.NegocioDtos.NuevoNegocio;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/negocios")
public class NegocioController {

    private final NegocioService servicio;

    public NegocioController(NegocioService servicio) {
        this.servicio = servicio;
    }

    /** Público: lo usa el formulario al que llega el cliente desde la tarjeta o el QR. */
    @GetMapping("/{slug}")
    public NegocioPublico ver(@PathVariable String slug) {
        return servicio.verPublico(slug);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NegocioDelPanel crear(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody NuevoNegocio nuevo) {
        return servicio.crear(usuario(jwt), nuevo);
    }

    @GetMapping("/{slug}/miembros")
    public List<MiembroVista> miembros(@AuthenticationPrincipal Jwt jwt, @PathVariable String slug) {
        return servicio.miembros(slug, usuario(jwt));
    }

    @PostMapping("/{slug}/miembros")
    @ResponseStatus(HttpStatus.CREATED)
    public MiembroVista anadirEmpleado(@AuthenticationPrincipal Jwt jwt, @PathVariable String slug,
                                       @Valid @RequestBody NuevoMiembro nuevo) {
        return servicio.anadirEmpleado(slug, usuario(jwt), nuevo);
    }

    @DeleteMapping("/{slug}/miembros/{usuarioId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void quitarMiembro(@AuthenticationPrincipal Jwt jwt, @PathVariable String slug, @PathVariable UUID usuarioId) {
        servicio.quitarMiembro(slug, usuario(jwt), usuarioId);
    }

    private static UUID usuario(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
