import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Api, erroresDeCampos, mensajeDeError } from '../api/api';
import { Sesion } from '../sesion/sesion';

@Component({
  selector: 'app-registro',
  imports: [ReactiveFormsModule, RouterLink],
  styleUrl: './cuenta.css',
  template: `
    <main class="acceso">
      <a class="logo" routerLink="/"><img src="favicon.svg" alt="" />Opina</a>
      <h1>Crea tu cuenta</h1>
      <p class="intro">Después darás de alta tu negocio o te añadirán al de tu empresa.</p>

      <form [formGroup]="formulario" (ngSubmit)="registrar()" novalidate>
        <div class="campo">
          <label for="nombre">Tu nombre</label>
          <input id="nombre" formControlName="nombre" autocomplete="name" />
          @if (errores()['nombre']; as e) {
            <span class="error">{{ e }}</span>
          }
        </div>
        <div class="campo">
          <label for="email">Email</label>
          <input id="email" type="email" formControlName="email" autocomplete="email" />
          @if (errores()['email']; as e) {
            <span class="error">{{ e }}</span>
          }
        </div>
        <div class="campo">
          <label for="contrasena">Contraseña</label>
          <input
            id="contrasena"
            type="password"
            formControlName="contrasena"
            autocomplete="new-password"
          />
          @if (errores()['contrasena']; as e) {
            <span class="error">{{ e }}</span>
          } @else {
            <span class="ayuda">Al menos 10 caracteres</span>
          }
        </div>

        @if (error()) {
          <p class="aviso-error" role="alert">{{ error() }}</p>
        }
        <button class="boton" type="submit" [disabled]="enviando()">
          {{ enviando() ? 'Creando cuenta…' : 'Crear cuenta' }}
        </button>
      </form>

      <p class="cambiar">¿Ya tienes cuenta? <a routerLink="/entrar">Entra</a></p>
    </main>
  `,
})
export class Registro {
  private readonly api = inject(Api);
  private readonly sesion = inject(Sesion);
  private readonly router = inject(Router);

  protected readonly enviando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly errores = signal<Record<string, string>>({});

  // Sin validadores aquí: las reglas están en el servidor, que devuelve un mensaje por campo
  protected readonly formulario = inject(FormBuilder).nonNullable.group({
    nombre: [''],
    email: [''],
    contrasena: [''],
  });

  protected async registrar() {
    this.enviando.set(true);
    this.error.set(null);
    this.errores.set({});
    try {
      const { nombre, email, contrasena } = this.formulario.getRawValue();
      this.sesion.iniciar(await this.api.registrar(nombre, email, contrasena));
      await this.router.navigateByUrl('/panel');
    } catch (e) {
      const campos = erroresDeCampos(e);
      this.errores.set(campos);
      if (Object.keys(campos).length === 0) {
        this.error.set(mensajeDeError(e));
      }
    } finally {
      this.enviando.set(false);
    }
  }
}
