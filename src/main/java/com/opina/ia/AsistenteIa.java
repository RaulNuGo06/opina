package com.opina.ia;

import java.util.List;

/**
 * Lo que opInA le pide a la IA. Detrás está Claude (ClaudeAsistente) o, si no hay clave de API,
 * SinIa, que deja la aplicación funcionando sin estas funciones. En los tests se usa uno falso.
 */
public interface AsistenteIa {

    boolean activo();

    Clasificacion clasificar(String comentario, int puntuacion);

    /** Un párrafo para el dueño del negocio con lo más importante de las opiniones del periodo. */
    String resumir(String negocio, int dias, List<OpinionParaResumir> opiniones);

    record OpinionParaResumir(int puntuacion, String comentario, Clasificacion.Tema tema, boolean urgente) {
    }

    /** La IA no ha podido responder (no hay conexión, se ha negado, la respuesta no vale…). */
    class FalloIa extends RuntimeException {
        public FalloIa(String mensaje, Throwable causa) {
            super(mensaje, causa);
        }

        public FalloIa(String mensaje) {
            super(mensaje);
        }
    }
}
