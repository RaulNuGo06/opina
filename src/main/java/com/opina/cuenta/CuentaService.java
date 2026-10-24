package com.opina.cuenta;

import com.opina.cuenta.CuentaDtos.Login;
import com.opina.cuenta.CuentaDtos.MiNegocio;
import com.opina.cuenta.CuentaDtos.Registro;
import com.opina.cuenta.CuentaDtos.Yo;
import com.opina.cuenta.TokenService.Token;
import com.opina.negocio.MiembroRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class CuentaService {

    private final UsuarioRepository usuarios;
    private final MiembroRepository miembros;
    private final PasswordEncoder contrasenas;
    private final TokenService tokens;
    private final Clock reloj;
    private final String hashDeRelleno;

    public CuentaService(UsuarioRepository usuarios, MiembroRepository miembros, PasswordEncoder contrasenas,
                         TokenService tokens, Clock reloj) {
        this.usuarios = usuarios;
        this.miembros = miembros;
        this.contrasenas = contrasenas;
        this.tokens = tokens;
        this.reloj = reloj;
        this.hashDeRelleno = contrasenas.encode("relleno-para-igualar-tiempos");
    }

    @Transactional
    public Token registrar(Registro registro) {
        String email = Usuario.normalizarEmail(registro.email());
        if (usuarios.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya hay una cuenta con ese email");
        }
        Usuario usuario = usuarios.save(new Usuario(
                email, registro.nombre().strip(), contrasenas.encode(registro.contrasena()), Instant.now(reloj)));
        return tokens.emitir(usuario);
    }

    @Transactional(readOnly = true)
    public Token entrar(Login login) {
        Usuario usuario = usuarios.findByEmail(Usuario.normalizarEmail(login.email())).orElse(null);

        // Se comprueba una contraseña aunque el email no exista, para que el tiempo de respuesta
        // no delate qué emails tienen cuenta. El mensaje de error tampoco dice cuál de los dos falla.
        String hash = usuario != null ? usuario.getHashContrasena() : hashDeRelleno;
        boolean coincide = contrasenas.matches(login.contrasena(), hash);
        if (usuario == null || !coincide) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email o contraseña incorrectos");
        }
        return tokens.emitir(usuario);
    }

    @Transactional(readOnly = true)
    public Yo yo(UUID usuarioId) {
        Usuario usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La cuenta ya no existe"));
        var negocios = miembros.findByUsuarioIdOrderByCreadoEn(usuarioId).stream()
                .map(m -> new MiNegocio(m.getNegocio().getSlug(), m.getNegocio().getNombre(), m.getRol()))
                .toList();
        return new Yo(usuario.getId(), usuario.getNombre(), usuario.getEmail(), negocios);
    }
}
