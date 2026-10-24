package com.opina.negocio;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/negocios")
public class NegocioController {

    private final NegocioRepository negocios;

    public NegocioController(NegocioRepository negocios) {
        this.negocios = negocios;
    }

    /** Lo que necesita el formulario público para saludar al cliente con el nombre del negocio. */
    @GetMapping("/{slug}")
    public NegocioPublico ver(@PathVariable String slug) {
        Negocio negocio = negocios.findBySlug(slug).orElseThrow(() -> new NegocioNoEncontradoException(slug));
        return new NegocioPublico(negocio.getSlug(), negocio.getNombre());
    }

    public record NegocioPublico(String slug, String nombre) {
    }
}
