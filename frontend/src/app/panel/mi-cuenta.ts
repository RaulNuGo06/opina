import { Injectable, inject, signal } from '@angular/core';
import { Api, Yo } from '../api/api';

/** Quién ha entrado y a qué negocios tiene acceso. Lo comparten todas las pantallas del panel. */
@Injectable({ providedIn: 'root' })
export class MiCuenta {
  private readonly api = inject(Api);

  readonly yo = signal<Yo | null>(null);

  async cargar() {
    this.yo.set(await this.api.yo());
  }

  olvidar() {
    this.yo.set(null);
  }
}
