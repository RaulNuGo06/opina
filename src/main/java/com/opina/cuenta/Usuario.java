package com.opina.cuenta;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String hashContrasena;

    @Column(nullable = false)
    private Instant creadoEn;

    protected Usuario() {
    }

    public Usuario(String email, String nombre, String hashContrasena, Instant creadoEn) {
        this.email = normalizarEmail(email);
        this.nombre = nombre;
        this.hashContrasena = hashContrasena;
        this.creadoEn = creadoEn;
    }

    /** "Ana@Bar.com " y "ana@bar.com" son la misma cuenta. */
    public static String normalizarEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getNombre() {
        return nombre;
    }

    public String getHashContrasena() {
        return hashContrasena;
    }
}
