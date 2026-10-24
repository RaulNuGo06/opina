package com.opina;

import com.opina.ia.AsistenteIa;
import com.opina.ia.Clasificacion;
import com.opina.ia.Clasificacion.Sentimiento;
import com.opina.ia.Clasificacion.Tema;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Sustituye a Claude en los tests: responde al momento, siempre igual y sin gastar dinero.
 * Clasifica por palabras clave y deja ver cuántas veces se le ha llamado.
 */
public class AsistenteFalso implements AsistenteIa {

    public volatile boolean activo = true;
    public volatile boolean fallar = false;
    public final List<String> comentariosClasificados = new CopyOnWriteArrayList<>();
    public final AtomicInteger resumenes = new AtomicInteger();

    public void reiniciar() {
        activo = true;
        fallar = false;
        comentariosClasificados.clear();
        resumenes.set(0);
    }

    @Override
    public boolean activo() {
        return activo;
    }

    @Override
    public Clasificacion clasificar(String comentario, int puntuacion) {
        if (fallar) {
            throw new FalloIa("Fallo simulado");
        }
        comentariosClasificados.add(comentario);
        String texto = comentario.toLowerCase(Locale.ROOT);
        if (texto.contains("cucaracha") || texto.contains("sucio")) {
            return new Clasificacion(Tema.LIMPIEZA, Sentimiento.NEGATIVO, true);
        }
        if (texto.contains("esper")) {
            return new Clasificacion(Tema.ESPERA, Sentimiento.NEGATIVO, false);
        }
        return new Clasificacion(Tema.CALIDAD, puntuacion >= 4 ? Sentimiento.POSITIVO : Sentimiento.NEUTRO, false);
    }

    @Override
    public String resumir(String negocio, int dias, List<OpinionParaResumir> opiniones) {
        if (fallar) {
            throw new FalloIa("Fallo simulado");
        }
        resumenes.incrementAndGet();
        return "Resumen de prueba de " + opiniones.size() + " opiniones";
    }
}
