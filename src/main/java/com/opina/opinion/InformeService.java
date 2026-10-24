package com.opina.opinion;

import com.opina.ia.AsistenteIa;
import com.opina.ia.AsistenteIa.FalloIa;
import com.opina.ia.AsistenteIa.OpinionParaResumir;
import com.opina.ia.Clasificacion.Tema;
import com.opina.negocio.ControlAcceso;
import com.opina.negocio.Negocio;
import com.opina.opinion.Opinion.EstadoIa;
import com.opina.opinion.OpinionDtos.OpinionVista;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * El informe del panel: de qué hablan los clientes, qué es urgente y un resumen escrito por la IA.
 *
 * Sin @Transactional a propósito: escribir el resumen puede tardar unos segundos y no hay que tener
 * una conexión a la base de datos ocupada mientras tanto.
 */
@Service
public class InformeService {

    private static final Logger log = LoggerFactory.getLogger(InformeService.class);

    /** Tope de opiniones que entran en un informe; de sobra para un negocio local en 90 días */
    static final int MAXIMO_OPINIONES = 500;
    /** Cuántos comentarios recibe la IA para escribir el resumen (los más recientes) */
    static final int MAXIMO_PARA_RESUMIR = 150;
    static final int MAXIMO_URGENTES = 10;

    private final OpinionRepository opiniones;
    private final ControlAcceso acceso;
    private final AsistenteIa asistente;
    private final JdbcClient jdbc;
    private final Clock reloj;

    public InformeService(OpinionRepository opiniones, ControlAcceso acceso, AsistenteIa asistente,
                          JdbcClient jdbc, Clock reloj) {
        this.opiniones = opiniones;
        this.acceso = acceso;
        this.asistente = asistente;
        this.jdbc = jdbc;
        this.reloj = reloj;
    }

    public record Informe(int dias, int total, Map<Tema, Integer> porTema, List<OpinionVista> urgentes,
                          int pendientes, boolean iaActiva, String resumen, Instant resumenGeneradoEn) {
    }

    public Informe informe(String slug, UUID usuarioId, int dias) {
        Negocio negocio = acceso.exigirMiembro(slug, usuarioId);
        Instant desde = Instant.now(reloj).minus(Duration.ofDays(dias));
        List<Opinion> periodo = opiniones.findByNegocioAndCreadaEnGreaterThanEqualOrderByCreadaEnDesc(
                negocio, desde, Limit.of(MAXIMO_OPINIONES));

        Map<Tema, Integer> porTema = new LinkedHashMap<>();
        for (Tema tema : Tema.values()) {
            porTema.put(tema, 0);
        }
        int pendientes = 0;
        for (Opinion o : periodo) {
            if (o.getTema() != null) porTema.merge(o.getTema(), 1, Integer::sum);
            if (o.getEstadoIa() == EstadoIa.PENDIENTE) pendientes++;
        }
        List<OpinionVista> urgentes = periodo.stream()
                .filter(Opinion::isUrgente)
                .limit(MAXIMO_URGENTES)
                .map(OpinionVista::de)
                .toList();

        ResumenGuardado resumen = asistente.activo() ? resumen(negocio, dias, periodo) : null;
        return new Informe(dias, periodo.size(), porTema, urgentes, pendientes, asistente.activo(),
                resumen == null ? null : resumen.texto(), resumen == null ? null : resumen.generadoEn());
    }

    private record ResumenGuardado(String huella, String texto, Instant generadoEn) {
    }

    /**
     * Reutiliza el resumen guardado si las opiniones del periodo no han cambiado desde que se escribió.
     * La huella cambia si entra una opinión nueva o si la IA termina de clasificar alguna.
     */
    private ResumenGuardado resumen(Negocio negocio, int dias, List<Opinion> periodo) {
        List<Opinion> conComentario = periodo.stream().filter(o -> o.getComentario() != null).toList();
        if (conComentario.isEmpty()) {
            return null;
        }
        long clasificadas = conComentario.stream().filter(o -> o.getEstadoIa() == EstadoIa.HECHA).count();
        String huella = conComentario.size() + "|" + clasificadas + "|" + conComentario.getFirst().getId();

        ResumenGuardado guardado = jdbc.sql("select huella, texto, generado_en from resumen_ia where negocio_id = ? and dias = ?")
                .params(negocio.getId(), dias)
                .query((fila, n) -> new ResumenGuardado(fila.getString(1), fila.getString(2),
                        fila.getObject(3, OffsetDateTime.class).toInstant()))
                .optional()
                .orElse(null);
        if (guardado != null && guardado.huella().equals(huella)) {
            return guardado;
        }

        List<OpinionParaResumir> material = conComentario.stream()
                .limit(MAXIMO_PARA_RESUMIR)
                .map(o -> new OpinionParaResumir(o.getPuntuacion(), o.getComentario(), o.getTema(), o.isUrgente()))
                .toList();
        try {
            String texto = asistente.resumir(negocio.getNombre(), dias, material);
            Instant ahora = Instant.now(reloj);
            jdbc.sql("""
                            insert into resumen_ia (negocio_id, dias, huella, texto, generado_en) values (?, ?, ?, ?, ?)
                            on conflict (negocio_id, dias) do update
                            set huella = excluded.huella, texto = excluded.texto, generado_en = excluded.generado_en
                            """)
                    .params(negocio.getId(), dias, huella, texto, OffsetDateTime.ofInstant(ahora, ZoneOffset.UTC))
                    .update();
            return new ResumenGuardado(huella, texto, ahora);
        } catch (FalloIa e) {
            // Mejor un resumen algo atrasado que ninguno; si no hay ninguno, el panel muestra solo las cifras
            log.warn("No se ha podido escribir el resumen de {}: {}", negocio.getSlug(), e.getMessage());
            return guardado;
        }
    }
}
