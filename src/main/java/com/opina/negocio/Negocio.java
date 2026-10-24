package com.opina.negocio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.Instant;
import java.util.UUID;

@Entity
public class Negocio {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String nombre;

    private String urlResenaGoogle;

    @Column(nullable = false)
    private Instant creadoEn;

    protected Negocio() {
    }

    public Negocio(String slug, String nombre, String urlResenaGoogle) {
        this.slug = slug;
        this.nombre = nombre;
        this.urlResenaGoogle = urlResenaGoogle;
        this.creadoEn = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getNombre() {
        return nombre;
    }

    public String getUrlResenaGoogle() {
        return urlResenaGoogle;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }
}
