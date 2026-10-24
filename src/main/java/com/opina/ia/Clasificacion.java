package com.opina.ia;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Lo que la IA deduce de una opinión. Es también el esquema JSON que se le exige en la respuesta,
 * así que solo puede devolver valores de estas listas.
 */
public record Clasificacion(
        @JsonPropertyDescription("El tema principal del comentario")
        Tema tema,

        @JsonPropertyDescription("Cómo se siente el cliente según lo que escribe")
        Sentimiento sentimiento,

        @JsonPropertyDescription("true solo si el negocio debería actuar cuanto antes")
        boolean urgente) {

    public enum Tema {
        /** Trato del personal, amabilidad, atención */
        ATENCION,
        /** Tiempos de espera, colas, lentitud */
        ESPERA,
        /** Calidad del producto o del servicio en sí: comida, corte de pelo, entrenamiento… */
        CALIDAD,
        PRECIO,
        LIMPIEZA,
        /** Ruido, música, temperatura, instalaciones, espacio */
        AMBIENTE,
        OTRO
    }

    public enum Sentimiento {
        POSITIVO, NEUTRO, NEGATIVO
    }
}
