package com.opina.opinion;

import com.opina.ia.AsistenteIa;
import com.opina.ia.AsistenteIa.FalloIa;
import com.opina.ia.Clasificacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;

/**
 * Clasifica con IA las opiniones nuevas en segundo plano, para que el cliente no espere a la IA al opinar.
 *
 * Cada opinión va en su propia transacción y queda bloqueada mientras se clasifica (una sola fila y unos
 * segundos). Si la IA falla, la vuelta se detiene: lo normal es que esté caída o saturada, y seguir solo
 * gastaría los reintentos de las demás opiniones. Se vuelve a probar en la siguiente vuelta.
 */
@Component
public class ClasificadorOpiniones {

    private static final Logger log = LoggerFactory.getLogger(ClasificadorOpiniones.class);

    /** Como mucho, cuántas opiniones se clasifican en cada vuelta */
    static final int POR_VUELTA = 20;

    private final OpinionRepository opiniones;
    private final AsistenteIa asistente;
    private final TransactionTemplate transaccion;
    private final Clock reloj;

    public ClasificadorOpiniones(OpinionRepository opiniones, AsistenteIa asistente,
                                 TransactionTemplate transaccion, Clock reloj) {
        this.opiniones = opiniones;
        this.asistente = asistente;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    @Scheduled(fixedDelayString = "${opina.ia.intervalo}", initialDelayString = "${opina.ia.intervalo}")
    void vuelta() {
        if (asistente.activo()) {
            procesarPendientes();
        }
    }

    private enum Resultado { NO_QUEDAN, CLASIFICADA, FALLO }

    /** Devuelve cuántas opiniones ha clasificado en esta vuelta. */
    public int procesarPendientes() {
        int clasificadas = 0;
        while (clasificadas < POR_VUELTA && transaccion.execute(estado -> clasificarSiguiente()) == Resultado.CLASIFICADA) {
            clasificadas++;
        }
        return clasificadas;
    }

    private Resultado clasificarSiguiente() {
        Opinion opinion = opiniones.bloquearSiguientePendiente().orElse(null);
        if (opinion == null) {
            return Resultado.NO_QUEDAN;
        }
        try {
            Clasificacion clasificacion = asistente.clasificar(opinion.getComentario(), opinion.getPuntuacion());
            if (clasificacion == null || clasificacion.tema() == null || clasificacion.sentimiento() == null) {
                throw new FalloIa("Clasificación incompleta");
            }
            opinion.clasificar(clasificacion, Instant.now(reloj));
            return Resultado.CLASIFICADA;
        } catch (FalloIa e) {
            log.warn("No se ha podido clasificar la opinión {}: {}", opinion.getId(), e.getMessage());
            opinion.anotarFalloIa();
            return Resultado.FALLO;
        }
    }
}
