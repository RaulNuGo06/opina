package com.opina.negocio;

import com.opina.cuenta.Usuario;
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

/** Una persona con acceso al panel de un negocio, con su rol en él. */
@Entity
public class Miembro {

    public enum Rol {
        /** Ve las opiniones y decide quién más tiene acceso. */
        DUENO,
        /** Ve las opiniones. */
        EMPLEADO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "negocio_id")
    private Negocio negocio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol;

    @Column(nullable = false)
    private Instant creadoEn;

    protected Miembro() {
    }

    public Miembro(Negocio negocio, Usuario usuario, Rol rol, Instant creadoEn) {
        this.negocio = negocio;
        this.usuario = usuario;
        this.rol = rol;
        this.creadoEn = creadoEn;
    }

    public UUID getId() {
        return id;
    }

    public Negocio getNegocio() {
        return negocio;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Rol getRol() {
        return rol;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }
}
