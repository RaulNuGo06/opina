import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Api, Miembro, Opinion, Resumen, mensajeDeError } from '../api/api';
import { MiCuenta } from './mi-cuenta';
import { haceCuanto, nota } from './textos';

const POR_PAGINA = 20;

@Component({
  selector: 'app-negocio-panel',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './negocio.html',
  styleUrl: './negocio.css',
})
export class NegocioPanel implements OnInit {
  private readonly api = inject(Api);
  private readonly cuenta = inject(MiCuenta);

  /** Llega de la ruta /panel/:slug */
  readonly slug = input.required<string>();

  protected readonly negocio = computed(() =>
    this.cuenta.yo()?.negocios.find((n) => n.slug === this.slug()),
  );
  protected readonly esDueno = computed(() => this.negocio()?.rol === 'DUENO');
  protected readonly enlacePublico = computed(() => `${location.origin}/n/${this.slug()}`);

  protected readonly resumen = signal<Resumen | null>(null);
  protected readonly opiniones = signal<Opinion[]>([]);
  protected readonly total = signal(0);
  protected readonly pagina = signal(0);
  protected readonly cargandoMas = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly miembros = signal<Miembro[]>([]);
  protected readonly emailNuevo = new FormControl('', { nonNullable: true });
  protected readonly formEquipo = new FormGroup({ email: this.emailNuevo });
  protected readonly errorEquipo = signal<string | null>(null);
  protected readonly copiado = signal(false);

  /** Las cinco filas del reparto, de 5 a 1, con su ancho relativo a la nota más repetida */
  protected readonly reparto = computed(() => {
    const r = this.resumen();
    if (!r) return [];
    const maximo = Math.max(1, ...Object.values(r.porPuntuacion));
    return ([5, 4, 3, 2, 1] as const).map((n) => {
      const cuantas = r.porPuntuacion[`${n}`];
      return { nota: n, cuantas, ancho: (cuantas / maximo) * 100 };
    });
  });

  protected readonly hayMas = computed(() => this.opiniones().length < this.total());
  protected readonly haceCuanto = haceCuanto;
  protected readonly nota = nota;

  async ngOnInit() {
    try {
      const [resumen, primera] = await Promise.all([
        this.api.resumen(this.slug()),
        this.api.opiniones(this.slug(), 0, POR_PAGINA),
      ]);
      this.resumen.set(resumen);
      this.opiniones.set(primera.contenido);
      this.total.set(primera.total);
      if (this.esDueno()) {
        this.miembros.set(await this.api.miembros(this.slug()));
      }
    } catch (e) {
      this.error.set(mensajeDeError(e));
    }
  }

  protected async cargarMas() {
    this.cargandoMas.set(true);
    try {
      const siguiente = await this.api.opiniones(this.slug(), this.pagina() + 1, POR_PAGINA);
      this.pagina.update((p) => p + 1);
      this.opiniones.update((actuales) => [...actuales, ...siguiente.contenido]);
      this.total.set(siguiente.total);
    } catch (e) {
      this.error.set(mensajeDeError(e));
    } finally {
      this.cargandoMas.set(false);
    }
  }

  protected async copiarEnlace() {
    try {
      await navigator.clipboard.writeText(this.enlacePublico());
      this.copiado.set(true);
      setTimeout(() => this.copiado.set(false), 2000);
    } catch {
      // Sin permiso para el portapapeles: el enlace sigue visible para copiarlo a mano
    }
  }

  protected async anadir() {
    this.errorEquipo.set(null);
    try {
      const nuevo = await this.api.anadirMiembro(this.slug(), this.emailNuevo.value);
      this.miembros.update((m) => [...m, nuevo]);
      this.emailNuevo.reset();
    } catch (e) {
      this.errorEquipo.set(mensajeDeError(e));
    }
  }

  protected async quitar(miembro: Miembro) {
    if (
      !confirm(`¿Quitar a ${miembro.nombre} del panel? Dejará de ver las opiniones al momento.`)
    ) {
      return;
    }
    this.errorEquipo.set(null);
    try {
      await this.api.quitarMiembro(this.slug(), miembro.usuarioId);
      this.miembros.update((m) => m.filter((x) => x.usuarioId !== miembro.usuarioId));
    } catch (e) {
      this.errorEquipo.set(mensajeDeError(e));
    }
  }

  /** 1-2 queja, 3 regular, 4-5 contento: el color del número en cada opinión */
  protected tono(puntuacion: number) {
    return puntuacion <= 2 ? 'queja' : puntuacion === 3 ? 'regular' : 'contento';
  }
}
