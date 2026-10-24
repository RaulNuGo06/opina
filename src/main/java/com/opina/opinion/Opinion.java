package com.opina.opinion;

import com.opina.ia.Clasificacion;
import com.opina.ia.Clasificacion.Sentimiento;
import com.opina.ia.Clasificacion.Tema;
import com.opina.negocio.Negocio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    /** En qué punto está la clasificación con IA de esta opinión */
    public enum EstadoIa {
        PENDIENTE,
        HECHA,
        /** Sin comentario no hay nada que clasificar: no se gasta ninguna llamada */
        OMITIDA,
        /** Falló varias veces seguidas; se deja de intentar */
        FALLIDA
    }

    static final int MAXIMO_INTENTOS_IA = 3;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoIa estadoIa;

    @Column(nullable = false)
    private short intentosIa;

    @Enumerated(EnumType.STRING)
    private Tema tema;

    @Enumerated(EnumType.STRING)
    private Sentimiento sentimiento;

    @Column(nullable = false)
    private boolean urgente;

    private Instant clasificadaEn;

    protected Opinion() {
    }

    public Opinion(Negocio negocio, int puntuacion, String comentario, Instant creadaEn) {
        this.negocio = negocio;
        this.puntuacion = (short) puntuacion;
        this.comentario = comentario;
        this.creadaEn = creadaEn;
        this.estadoIa = comentario == null ? EstadoIa.OMITIDA : EstadoIa.PENDIENTE;
    }

    void clasificar(Clasificacion clasificacion, Instant ahora) {
        this.tema = clasificacion.tema();
        this.sentimiento = clasificacion.sentimiento();
        this.urgente = clasificacion.urgente();
        this.clasificadaEn = ahora;
        this.estadoIa = EstadoIa.HECHA;
    }

    /** Un fallo puntual (red, saturación) se reintenta en la siguiente vuelta; tras varios, se abandona. */
    void anotarFalloIa() {
        this.intentosIa++;
        if (this.intentosIa >= MAXIMO_INTENTOS_IA) {
            this.estadoIa = EstadoIa.FALLIDA;
        }
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

    public EstadoIa getEstadoIa() {
        return estadoIa;
    }

    public int getIntentosIa() {
        return intentosIa;
    }

    public Tema getTema() {
        return tema;
    }

    public Sentimiento getSentimiento() {
        return sentimiento;
    }

    public boolean isUrgente() {
        return urgente;
    }
}
