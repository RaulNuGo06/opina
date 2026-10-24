import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { Api, Informe as DatosInforme, Tema, mensajeDeError } from '../api/api';
import { TEMAS, haceCuanto } from './textos';

const PERIODOS = [7, 30, 90] as const;

/** Lo que dicen los clientes en un periodo: resumen escrito por la IA, lo urgente y los temas. */
@Component({
  selector: 'app-informe',
  templateUrl: './informe.html',
  styleUrl: './informe.css',
})
export class Informe {
  private readonly api = inject(Api);

  readonly slug = input.required<string>();

  protected readonly periodos = PERIODOS;
  protected readonly dias = signal<number>(7);
  protected readonly datos = signal<DatosInforme | null>(null);
  protected readonly cargando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly temasNombre = TEMAS;
  protected readonly haceCuanto = haceCuanto;

  /** Solo los temas que aparecen, del más al menos repetido, con su ancho relativo al primero */
  protected readonly temas = computed(() => {
    const d = this.datos();
    if (!d) return [];
    const filas = (Object.entries(d.porTema) as [Tema, number][])
      .filter(([, cuantas]) => cuantas > 0)
      .sort((a, b) => b[1] - a[1]);
    const maximo = filas[0]?.[1] ?? 1;
    return filas.map(([tema, cuantas]) => ({ tema, cuantas, ancho: (cuantas / maximo) * 100 }));
  });

  constructor() {
    // Se vuelve a pedir el informe cada vez que cambia el negocio o el periodo
    effect(() => {
      this.cargar(this.slug(), this.dias());
    });
  }

  protected elegir(dias: number) {
    this.dias.set(dias);
  }

  protected actualizar() {
    this.cargar(this.slug(), this.dias());
  }

  private async cargar(slug: string, dias: number) {
    this.cargando.set(true);
    this.error.set(null);
    try {
      this.datos.set(await this.api.informe(slug, dias));
    } catch (e) {
      this.error.set(mensajeDeError(e));
    } finally {
      this.cargando.set(false);
    }
  }
}
