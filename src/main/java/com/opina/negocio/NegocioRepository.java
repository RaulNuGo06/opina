package com.opina.negocio;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NegocioRepository extends JpaRepository<Negocio, UUID> {

    Optional<Negocio> findBySlug(String slug);
}
