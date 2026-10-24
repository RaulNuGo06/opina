package com.opina.negocio;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class NegocioNoEncontradoException extends ResponseStatusException {

    public NegocioNoEncontradoException(String slug) {
        super(HttpStatus.NOT_FOUND, "No existe ningún negocio con el enlace \"" + slug + "\"");
    }
}
