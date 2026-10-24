package com.opina.opinion;

import com.opina.opinion.OpinionDtos.NuevaOpinion;
import com.opina.opinion.OpinionDtos.OpinionCreada;
import com.opina.opinion.OpinionDtos.OpinionVista;
import com.opina.opinion.OpinionDtos.Pagina;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/negocios/{slug}/opiniones")
public class OpinionController {

    private final OpinionService servicio;

    public OpinionController(OpinionService servicio) {
        this.servicio = servicio;
    }

    /** Público: lo usa el formulario al que llega el cliente desde la tarjeta NFC o el QR. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OpinionCreada crear(@PathVariable String slug, @Valid @RequestBody NuevaOpinion nueva) {
        return servicio.crear(slug, nueva);
    }

    // Listado y resumen son para el panel del negocio. En la fase 2 pedirán iniciar sesión.

    @GetMapping
    public Pagina<OpinionVista> listar(
            @PathVariable String slug,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página empieza en 0") int pagina,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "El tamaño va de 1 a 100")
            @Max(value = 100, message = "El tamaño va de 1 a 100") int tamano) {
        return servicio.listar(slug, pagina, tamano);
    }

    @GetMapping("/resumen")
    public Resumen resumen(@PathVariable String slug) {
        return servicio.resumen(slug);
    }
}
