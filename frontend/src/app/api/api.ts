import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

// Lo mismo que devuelve el backend (ver los DTOs de Spring)

export type Rol = 'DUENO' | 'EMPLEADO';

export interface NegocioPublico {
  slug: string;
  nombre: string;
}

export interface OpinionCreada {
  id: string;
  urlResenaGoogle: string | null;
}

export interface Token {
  token: string;
  caducaEn: string;
}

export interface Yo {
  id: string;
  nombre: string;
  email: string;
  negocios: { slug: string; nombre: string; rol: Rol }[];
}

export interface NegocioDelPanel {
  slug: string;
  nombre: string;
  urlResenaGoogle: string | null;
  rol: Rol;
}

/** Los temas en los que la IA reparte las opiniones (lista cerrada, igual que en el backend) */
export type Tema = 'ATENCION' | 'ESPERA' | 'CALIDAD' | 'PRECIO' | 'LIMPIEZA' | 'AMBIENTE' | 'OTRO';

export interface Opinion {
  id: string;
  puntuacion: number;
  comentario: string | null;
  creadaEn: string;
  /** null mientras la IA no la haya clasificado, o si no tiene comentario */
  tema: Tema | null;
  sentimiento: 'POSITIVO' | 'NEUTRO' | 'NEGATIVO' | null;
  urgente: boolean;
}

export interface Informe {
  dias: number;
  total: number;
  porTema: Record<Tema, number>;
  urgentes: Opinion[];
  /** Opiniones del periodo que la IA aún no ha clasificado */
  pendientes: number;
  iaActiva: boolean;
  resumen: string | null;
  resumenGeneradoEn: string | null;
}

export interface Pagina<T> {
  contenido: T[];
  pagina: number;
  tamano: number;
  total: number;
}

export interface Resumen {
  total: number;
  media: number | null;
  porPuntuacion: Record<'1' | '2' | '3' | '4' | '5', number>;
}

export interface Miembro {
  usuarioId: string;
  nombre: string;
  email: string;
  rol: Rol;
  desde: string;
}

/** Todas las llamadas al backend. Devuelven promesas: las pantallas solo hacen una petición cada vez. */
@Injectable({ providedIn: 'root' })
export class Api {
  private readonly http = inject(HttpClient);

  negocio(slug: string) {
    return firstValueFrom(this.http.get<NegocioPublico>(`/api/negocios/${enc(slug)}`));
  }

  opinar(slug: string, puntuacion: number, comentario: string) {
    return firstValueFrom(
      this.http.post<OpinionCreada>(`/api/negocios/${enc(slug)}/opiniones`, {
        puntuacion,
        comentario,
      }),
    );
  }

  entrar(email: string, contrasena: string) {
    return firstValueFrom(this.http.post<Token>('/api/auth/login', { email, contrasena }));
  }

  registrar(nombre: string, email: string, contrasena: string) {
    return firstValueFrom(
      this.http.post<Token>('/api/auth/registro', { nombre, email, contrasena }),
    );
  }

  yo() {
    return firstValueFrom(this.http.get<Yo>('/api/yo'));
  }

  crearNegocio(nombre: string, slug: string, urlResenaGoogle: string | null) {
    return firstValueFrom(
      this.http.post<NegocioDelPanel>('/api/negocios', { nombre, slug, urlResenaGoogle }),
    );
  }

  opiniones(slug: string, pagina: number, tamano = 20) {
    return firstValueFrom(
      this.http.get<Pagina<Opinion>>(`/api/negocios/${enc(slug)}/opiniones`, {
        params: { pagina, tamano },
      }),
    );
  }

  resumen(slug: string) {
    return firstValueFrom(this.http.get<Resumen>(`/api/negocios/${enc(slug)}/opiniones/resumen`));
  }

  informe(slug: string, dias: number) {
    return firstValueFrom(
      this.http.get<Informe>(`/api/negocios/${enc(slug)}/opiniones/informe`, { params: { dias } }),
    );
  }

  miembros(slug: string) {
    return firstValueFrom(this.http.get<Miembro[]>(`/api/negocios/${enc(slug)}/miembros`));
  }

  anadirMiembro(slug: string, email: string) {
    return firstValueFrom(
      this.http.post<Miembro>(`/api/negocios/${enc(slug)}/miembros`, { email }),
    );
  }

  quitarMiembro(slug: string, usuarioId: string) {
    return firstValueFrom(
      this.http.delete<void>(`/api/negocios/${enc(slug)}/miembros/${enc(usuarioId)}`),
    );
  }
}

const enc = encodeURIComponent;

/** El backend responde con Problem Details: "detail" para el mensaje y "errores" campo a campo. */
export function mensajeDeError(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return 'No hay conexión con el servidor. Inténtalo de nuevo en un momento';
    }
    const detalle = error.error?.detail;
    if (typeof detalle === 'string' && detalle) {
      return detalle;
    }
  }
  return 'Algo ha fallado. Inténtalo de nuevo';
}

export function erroresDeCampos(error: unknown): Record<string, string> {
  return error instanceof HttpErrorResponse && error.error?.errores ? error.error.errores : {};
}
