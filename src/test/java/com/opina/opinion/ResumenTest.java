package com.opina.opinion;

import com.opina.opinion.Resumen.Recuento;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ResumenTest {

    @Test
    void sinOpinionesLaMediaEsNullYTodasLasNotasValenCero() {
        Resumen resumen = Resumen.de(List.of());

        assertThat(resumen.total()).isZero();
        assertThat(resumen.media()).isNull();
        assertThat(resumen.porPuntuacion()).containsExactly(
                entry(5, 0), entry(4, 0), entry(3, 0), entry(2, 0), entry(1, 0));
    }

    @Test
    void calculaTotalMediaYReparto() {
        // 2×5 + 1×4 + 1×1 = 15 puntos en 4 opiniones → 3,75 → 3,8
        Resumen resumen = Resumen.de(List.of(
                new Recuento((short) 5, 2), new Recuento((short) 4, 1), new Recuento((short) 1, 1)));

        assertThat(resumen.total()).isEqualTo(4);
        assertThat(resumen.media()).isEqualTo(3.8);
        assertThat(resumen.porPuntuacion()).containsExactly(
                entry(5, 2), entry(4, 1), entry(3, 0), entry(2, 0), entry(1, 1));
    }

    private static Map.Entry<Integer, Long> entry(int nota, long total) {
        return Map.entry(nota, total);
    }
}
