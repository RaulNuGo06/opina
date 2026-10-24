import { Component, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Api, mensajeDeError } from '../api/api';
import { Sesion } from '../sesion/sesion';

@Component({
  selector: 'app-entrar',
  imports: [ReactiveFormsModule, RouterLink],
  styleUrl: './cuenta.css',
  template: `
    <main class="acceso">
      <a class="logo" routerLink="/"><img src="favicon.svg" alt="" />Opina</a>
      <h1>Entra en tu panel</h1>
      <p class="intro">Para ver lo que opinan los clientes de tu negocio.</p>

      <form [formGroup]="formulario" (ngSubmit)="entrar()" novalidate>
        <div class="campo">
          <label for="email">Email</label>
          <input id="email" type="email" formControlName="email" autocomplete="email" />
        </div>
        <div class="campo">
          <label for="contrasena">Contraseña</label>
          <input
            id="contrasena"
            type="password"
            formControlName="contrasena"
            autocomplete="current-password"
          />
        </div>

        @if (error()) {
          <p class="aviso-error" role="alert">{{ error() }}</p>
        }
        <button class="boton" type="submit" [disabled]="enviando()">
          {{ enviando() ? 'Entrando…' : 'Entrar' }}
        </button>
      </form>

      <p class="cambiar">¿Aún no tienes cuenta? <a routerLink="/registro">Crea una</a></p>

      <p class="demo">
        Para probarlo: <code>dueno&#64;barlaplaza.demo</code> con la contraseña
        <code>demo-opina-2026</code>
        (solo con los datos de demostración).
      </p>
    </main>
  `,
})
export class Entrar {
  private readonly api = inject(Api);
  private readonly sesion = inject(Sesion);
  private readonly router = inject(Router);

  /** Adónde volver tras entrar (lo pone el guard del panel) */
  readonly volver = input<string>();

  protected readonly enviando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly formulario = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    contrasena: ['', Validators.required],
  });

  protected async entrar() {
    if (this.formulario.invalid) {
      this.error.set('Escribe tu email y tu contraseña');
      return;
    }
    this.enviando.set(true);
    this.error.set(null);
    try {
      const { email, contrasena } = this.formulario.getRawValue();
      this.sesion.iniciar(await this.api.entrar(email, contrasena));
      await this.router.navigateByUrl(destinoSeguro(this.volver()));
    } catch (e) {
      this.error.set(mensajeDeError(e));
    } finally {
      this.enviando.set(false);
    }
  }
}

/** Solo rutas internas del panel: evita que un enlace ?volver=https://otra-web lleve fuera tras entrar. */
export function destinoSeguro(volver: string | undefined): string {
  return volver && volver.startsWith('/panel') ? volver : '/panel';
}
