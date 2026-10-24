import { Injectable, computed, signal } from '@angular/core';
import { Token } from '../api/api';

const CLAVE = 'opina.sesion';

/**
 * El token del panel. Se guarda en localStorage para no perder la sesión al recargar;
 * a cambio, un fallo XSS podría leerlo (ver "Decisiones" en el README).
 */
@Injectable({ providedIn: 'root' })
export class Sesion {
  private readonly actual = signal<Token | null>(leer());

  readonly token = computed(() => {
    const sesion = this.actual();
    return sesion && new Date(sesion.caducaEn).getTime() > Date.now() ? sesion.token : null;
  });

  readonly activa = computed(() => this.token() !== null);

  iniciar(token: Token) {
    this.actual.set(token);
    try {
      localStorage.setItem(CLAVE, JSON.stringify(token));
    } catch {
      // Sin almacenamiento (modo privado estricto): la sesión dura hasta cerrar la pestaña
    }
  }

  cerrar() {
    this.actual.set(null);
    try {
      localStorage.removeItem(CLAVE);
    } catch {
      // Nada que borrar
    }
  }
}

function leer(): Token | null {
  try {
    const guardado = localStorage.getItem(CLAVE);
    return guardado ? (JSON.parse(guardado) as Token) : null;
  } catch {
    return null;
  }
}
