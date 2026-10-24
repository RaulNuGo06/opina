package com.opina.opinion;

import com.opina.negocio.Negocio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
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
}
