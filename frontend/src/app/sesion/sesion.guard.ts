import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Sesion } from './sesion';

/** El panel exige sesión; si no la hay, se va a entrar y después se vuelve a donde se quería ir. */
export const conSesion: CanActivateFn = (_ruta, estado) =>
  inject(Sesion).activa() ||
  inject(Router).createUrlTree(['/entrar'], { queryParams: { volver: estado.url } });
