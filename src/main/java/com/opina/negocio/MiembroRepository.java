package com.opina.negocio;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MiembroRepository extends JpaRepository<Miembro, UUID> {

    Optional<Miembro> findByNegocioAndUsuarioId(Negocio negocio, UUID usuarioId);

    boolean existsByNegocioAndUsuarioId(Negocio negocio, UUID usuarioId);

    @EntityGraph(attributePaths = "usuario")
    List<Miembro> findByNegocioOrderByCreadoEn(Negocio negocio);

    @EntityGraph(attributePaths = "negocio")
    List<Miembro> findByUsuarioIdOrderByCreadoEn(UUID usuarioId);
}
