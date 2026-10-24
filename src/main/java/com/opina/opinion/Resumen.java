package com.opina.opinion;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cifras de un negocio para la cabecera del panel.
 *
 * @param media           nota media redondeada a un decimal, o null si aún no hay opiniones
 * @param porPuntuacion   cuántas opiniones hay de cada nota, de 5 a 1 (siempre las cinco claves)
 */
public record Resumen(long total, Double media, Map<Integer, Long> porPuntuacion) {

    public record Recuento(short puntuacion, long total) {
    }

    static Resumen de(List<Recuento> recuentos) {
        Map<Integer, Long> porPuntuacion = new LinkedHashMap<>();
        for (int nota = 5; nota >= 1; nota--) {
            porPuntuacion.put(nota, 0L);
        }

        long total = 0;
        long suma = 0;
        for (Recuento r : recuentos) {
            porPuntuacion.put((int) r.puntuacion(), r.total());
            total += r.total();
            suma += r.puntuacion() * r.total();
        }

        Double media = total == 0 ? null : Math.round(suma * 10.0 / total) / 10.0;
        return new Resumen(total, media, porPuntuacion);
    }
}
