package com.opina.ia;

import java.util.List;

/** Sin clave de API la aplicación funciona igual, solo que sin clasificar ni resumir. */
class SinIa implements AsistenteIa {

    @Override
    public boolean activo() {
        return false;
    }

    @Override
    public Clasificacion clasificar(String comentario, int puntuacion) {
        throw new FalloIa("La IA no está configurada");
    }

    @Override
    public String resumir(String negocio, int dias, List<OpinionParaResumir> opiniones) {
        throw new FalloIa("La IA no está configurada");
    }
}
