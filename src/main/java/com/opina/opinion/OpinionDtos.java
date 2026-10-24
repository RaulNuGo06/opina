package com.opina.opinion;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Lo que entra y sale por la API de opiniones. */
public final class OpinionDtos {

    private OpinionDtos() {
    }

    public record NuevaOpinion(
            @NotNull(message = "La puntuación es obligatoria")
            @Min(value = 1, message = "La puntuación va de 1 a 5")
            @Max(value = 5, message = "La puntuación va de 1 a 5")
            Integer puntuacion,

            @Size(max = 1000, message = "El comentario no puede pasar de 1000 caracteres")
            String comentario) {
    }

    /**
     * Respuesta al cliente que acaba de opinar. Lleva el enlace de reseña de Google sea cual sea la nota:
     * enseñárselo solo a quien pone 4 o 5 estrellas va contra las normas de Google (review gating).
     */
    public record OpinionCreada(UUID id, String urlResenaGoogle) {
    }

    public record OpinionVista(UUID id, int puntuacion, String comentario, Instant creadaEn) {

        static OpinionVista de(Opinion opinion) {
            return new OpinionVista(opinion.getId(), opinion.getPuntuacion(), opinion.getComentario(), opinion.getCreadaEn());
        }
    }

    public record Pagina<T>(List<T> contenido, int pagina, int tamano, long total) {
    }
}
