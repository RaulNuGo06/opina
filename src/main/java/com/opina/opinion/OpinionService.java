package com.opina.opinion;

import com.opina.negocio.ControlAcceso;
import com.opina.negocio.Negocio;
import com.opina.negocio.NegocioNoEncontradoException;
import com.opina.negocio.NegocioRepository;
import com.opina.opinion.OpinionDtos.NuevaOpinion;
import com.opina.opinion.OpinionDtos.OpinionCreada;
import com.opina.opinion.OpinionDtos.OpinionVista;
import com.opina.opinion.OpinionDtos.Pagina;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class OpinionService {

    private final NegocioRepository negocios;
    private final OpinionRepository opiniones;
    private final ControlAcceso acceso;
    private final Clock reloj;

    public OpinionService(NegocioRepository negocios, OpinionRepository opiniones, ControlAcceso acceso, Clock reloj) {
        this.negocios = negocios;
        this.opiniones = opiniones;
        this.acceso = acceso;
        this.reloj = reloj;
    }

    @Transactional
    public OpinionCreada crear(String slug, NuevaOpinion nueva) {
        Negocio negocio = negocios.findBySlug(slug).orElseThrow(() -> new NegocioNoEncontradoException(slug));
        Opinion opinion = opiniones.save(
                new Opinion(negocio, nueva.puntuacion(), limpiar(nueva.comentario()), Instant.now(reloj)));
        return new OpinionCreada(opinion.getId(), negocio.getUrlResenaGoogle());
    }

    @Transactional(readOnly = true)
    public Pagina<OpinionVista> listar(String slug, UUID usuarioId, int pagina, int tamano) {
        Negocio negocio = acceso.exigirMiembro(slug, usuarioId);
        Page<Opinion> resultado = opiniones.findByNegocioOrderByCreadaEnDesc(negocio, PageRequest.of(pagina, tamano));
        return new Pagina<>(
                resultado.map(OpinionVista::de).getContent(), pagina, tamano, resultado.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Resumen resumen(String slug, UUID usuarioId) {
        return Resumen.de(opiniones.contarPorPuntuacion(acceso.exigirMiembro(slug, usuarioId)));
    }

    /** Un comentario vacío o solo con espacios se guarda como "sin comentario". */
    private static String limpiar(String comentario) {
        if (comentario == null || comentario.isBlank()) {
            return null;
        }
        return comentario.strip();
    }
}
