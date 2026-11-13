package com.opina.comun;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Cuando la app de Angular va dentro del jar (la imagen Docker), las rutas de Angular las tiene que servir Spring.
 * Si alguien abre /panel/bar-la-plaza directamente, se le da index.html y Angular pinta la pantalla que toca.
 * En desarrollo no se usa: la app la sirve ng serve en el puerto 4200.
 */
@Controller
class FrontendController {

    @GetMapping({"/n/{slug}", "/entrar", "/registro", "/panel", "/panel/{slug}"})
    String app() {
        return "forward:/index.html";
    }
}
