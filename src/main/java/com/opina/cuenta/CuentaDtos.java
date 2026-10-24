package com.opina.cuenta;

import com.opina.negocio.Miembro.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public final class CuentaDtos {

    private CuentaDtos() {
    }

    public record Registro(
            @NotBlank(message = "Escribe tu nombre")
            @Size(max = 120, message = "El nombre no puede pasar de 120 caracteres")
            String nombre,

            @NotBlank(message = "Escribe tu email")
            @Email(message = "El email no es válido")
            @Size(max = 254, message = "El email es demasiado largo")
            String email,

            // BCrypt solo usa los primeros 72 bytes: más largo daría una falsa sensación de seguridad
            @NotBlank(message = "Escribe una contraseña")
            @Size(min = 10, max = 72, message = "La contraseña debe tener entre 10 y 72 caracteres")
            String contrasena) {
    }

    public record Login(
            @NotBlank(message = "Escribe tu email") String email,
            @NotBlank(message = "Escribe tu contraseña") String contrasena) {
    }

    public record Yo(UUID id, String nombre, String email, List<MiNegocio> negocios) {
    }

    public record MiNegocio(String slug, String nombre, Rol rol) {
    }
}
