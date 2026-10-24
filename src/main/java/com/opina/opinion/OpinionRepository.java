package com.opina.opinion;

import com.opina.negocio.Negocio;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OpinionRepository extends JpaRepository<Opinion, UUID> {

    Page<Opinion> findByNegocioOrderByCreadaEnDesc(Negocio negocio, Pageable pageable);

    @Query("""
            select new com.opina.opinion.Resumen$Recuento(o.puntuacion, count(o))
            from Opinion o
            where o.negocio = :negocio
            group by o.puntuacion
            """)
    List<Resumen.Recuento> contarPorPuntuacion(Negocio negocio);

    /**
     * La opinión pendiente más antigua, bloqueada hasta el final de la transacción.
     * SKIP LOCKED salta las que ya está clasificando otra instancia, así nunca se clasifica dos veces la misma.
     */
    @Query(value = """
            select * from opinion
            where estado_ia = 'PENDIENTE'
            order by creada_en
            limit 1
            for update skip locked
            """, nativeQuery = true)
    Optional<Opinion> bloquearSiguientePendiente();

    List<Opinion> findByNegocioAndCreadaEnGreaterThanEqualOrderByCreadaEnDesc(Negocio negocio, Instant desde, Limit limite);
}
