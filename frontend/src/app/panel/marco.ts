import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { mensajeDeError } from '../api/api';
import { Sesion } from '../sesion/sesion';
import { MiCuenta } from './mi-cuenta';

/** Cabecera común del panel. Carga la cuenta una vez; las pantallas de dentro la leen de MiCuenta. */
@Component({
  selector: 'app-marco',
  imports: [RouterOutlet, RouterLink],
  template: `
    <header class="barra">
      <a class="logo" routerLink="/panel"><img src="favicon.svg" alt="" />Opina</a>
      @if (cuenta.yo(); as yo) {
        <span class="quien">{{ yo.nombre }}</span>
      }
      <button class="salir" type="button" (click)="salir()">Salir</button>
    </header>

    <div class="contenido">
      @if (error()) {
        <p class="aviso-error" role="alert">{{ error() }}</p>
      } @else if (cuenta.yo()) {
        <router-outlet />
      } @else {
        <p class="cargando">Cargando…</p>
      }
    </div>
  `,
  styles: `
    .barra {
      display: flex;
      align-items: center;
      gap: 1rem;
      padding: 0.85rem 1rem;
      border-bottom: 1px solid var(--linea);
      background: var(--tarjeta);
    }
    .logo {
      display: inline-flex;
      align-items: center;
      gap: 0.45rem;
      font:
        800 1.2rem/1 'Bricolage Grotesque',
        system-ui,
        sans-serif;
      color: var(--tinta);
      text-decoration: none;
      margin-right: auto;
    }
    .logo img {
      width: 24px;
      height: 24px;
    }
    .quien {
      color: var(--tinta-suave);
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      max-width: 40vw;
    }
    .salir {
      font: inherit;
      color: var(--tinta);
      background: none;
      border: 1.5px solid var(--linea);
      border-radius: var(--radio);
      padding: 0.35rem 0.8rem;
      cursor: pointer;
    }
    .contenido {
      max-width: 60rem;
      margin: 0 auto;
      padding: 2rem 1rem 4rem;
    }
    .cargando {
      color: var(--tinta-suave);
    }
  `,
})
export class Marco implements OnInit {
  protected readonly cuenta = inject(MiCuenta);
  private readonly sesion = inject(Sesion);
  private readonly router = inject(Router);

  protected readonly error = signal<string | null>(null);

  async ngOnInit() {
    try {
      await this.cuenta.cargar();
    } catch (e) {
      this.error.set(mensajeDeError(e));
    }
  }

  protected salir() {
    this.sesion.cerrar();
    this.cuenta.olvidar();
    this.router.navigateByUrl('/entrar');
  }
}
