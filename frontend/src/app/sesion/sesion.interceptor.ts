import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { Sesion } from './sesion';

/**
 * Añade el token a las llamadas a la API. Si el servidor dice que la sesión ya no vale
 * (caducada, o el secreto cambió al reiniciar), se cierra y se manda a entrar de nuevo.
 */
export const sesionInterceptor: HttpInterceptorFn = (peticion, siguiente) => {
  const sesion = inject(Sesion);
  const router = inject(Router);
  const token = sesion.token();

  const conToken =
    token && peticion.url.startsWith('/api/')
      ? peticion.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : peticion;

  return siguiente(conToken).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401 && token) {
        sesion.cerrar();
        router.navigate(['/entrar'], { queryParams: { volver: router.url } });
      }
      return throwError(() => error);
    }),
  );
};
