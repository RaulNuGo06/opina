package com.opina.negocio;

import com.opina.negocio.Miembro.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class NegocioDtos {

    private NegocioDtos() {
    }

    /** Lo que necesita el formulario público para saludar al cliente con el nombre del negocio. */
    public record NegocioPublico(String slug, String nombre) {
    }

    public record NuevoNegocio(
            @NotBlank(message = "Escribe el nombre del negocio")
            @Size(max = 120, message = "El nombre no puede pasar de 120 caracteres")
            String nombre,

            // Va en el enlace de la tarjeta (/n/bar-la-plaza): mismas reglas que la restricción de la tabla
            @NotBlank(message = "Elige el enlace del negocio")
            @Size(max = 60, message = "El enlace no puede pasar de 60 caracteres")
            @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$",
                    message = "El enlace solo puede llevar minúsculas, números y guiones, por ejemplo bar-la-plaza")
            String slug,

            @Size(max = 500, message = "El enlace de Google es demasiado largo")
            @Pattern(regexp = "^https://.+", message = "El enlace de Google debe empezar por https://")
            String urlResenaGoogle) {
    }

    public record NegocioDelPanel(String slug, String nombre, String urlResenaGoogle, Rol rol) {
    }

    public record NuevoMiembro(
            @NotBlank(message = "Escribe el email de la persona")
            @Email(message = "El email no es válido")
            String email) {
    }

    public record MiembroVista(UUID usuarioId, String nombre, String email, Rol rol, Instant desde) {

        static MiembroVista de(Miembro m) {
            return new MiembroVista(m.getUsuario().getId(), m.getUsuario().getNombre(), m.getUsuario().getEmail(),
                    m.getRol(), m.getCreadoEn());
        }
    }
}
