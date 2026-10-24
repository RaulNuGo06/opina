package com.opina.cuenta;

import com.opina.cuenta.CuentaDtos.Login;
import com.opina.cuenta.CuentaDtos.Registro;
import com.opina.cuenta.CuentaDtos.Yo;
import com.opina.cuenta.TokenService.Token;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class CuentaController {

    private final CuentaService servicio;

    public CuentaController(CuentaService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/auth/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public Token registrar(@Valid @RequestBody Registro registro) {
        return servicio.registrar(registro);
    }

    @PostMapping("/auth/login")
    public Token entrar(@Valid @RequestBody Login login) {
        return servicio.entrar(login);
    }

    /** Quién soy y qué negocios puedo ver. Lo primero que pide el panel al entrar. */
    @GetMapping("/yo")
    public Yo yo(@AuthenticationPrincipal Jwt jwt) {
        return servicio.yo(UUID.fromString(jwt.getSubject()));
    }
}
