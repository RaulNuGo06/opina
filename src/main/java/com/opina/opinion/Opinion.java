package com.opina.opinion;

import com.opina.negocio.Negocio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.time.Instant;
import java.util.UUID;

@Entity
public class Opinion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "negocio_id")
    private Negocio negocio;

    @Column(nullable = false)
    private short puntuacion;

    private String comentario;

    @Column(nullable = false)
    private Instant creadaEn;

    protected Opinion() {
    }

    public Opinion(Negocio negocio, int puntuacion, String comentario, Instant creadaEn) {
        this.negocio = negocio;
        this.puntuacion = (short) puntuacion;
        this.comentario = comentario;
        this.creadaEn = creadaEn;
    }

    public UUID getId() {
        return id;
    }

    public Negocio getNegocio() {
        return negocio;
    }

    public int getPuntuacion() {
        return puntuacion;
    }

    public String getComentario() {
        return comentario;
    }

    public Instant getCreadaEn() {
        return creadaEn;
    }
}
