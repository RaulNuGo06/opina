import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Api, erroresDeCampos, mensajeDeError } from '../api/api';
import { MiCuenta } from './mi-cuenta';
import { aSlug } from './textos';

@Component({
  selector: 'app-mis-negocios',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './mis-negocios.html',
  styleUrl: './mis-negocios.css',
})
export class MisNegocios {
  private readonly api = inject(Api);
  private readonly router = inject(Router);
  protected readonly cuenta = inject(MiCuenta);

  protected readonly creando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly errores = signal<Record<string, string>>({});

  protected readonly formulario = inject(FormBuilder).nonNullable.group({
    nombre: [''],
    slug: [''],
    urlResenaGoogle: [''],
  });

  /** Mientras la persona no toque el enlace, se rellena solo a partir del nombre */
  private slugTocado = false;

  protected alCambiarNombre() {
    if (!this.slugTocado) {
      this.formulario.controls.slug.setValue(aSlug(this.formulario.controls.nombre.value));
    }
  }

  protected alCambiarSlug() {
    this.slugTocado = true;
  }

  protected async crear() {
    this.creando.set(true);
    this.error.set(null);
    this.errores.set({});
    try {
      const { nombre, slug, urlResenaGoogle } = this.formulario.getRawValue();
      const creado = await this.api.crearNegocio(nombre, slug, urlResenaGoogle.trim() || null);
      await this.cuenta.cargar();
      await this.router.navigate(['/panel', creado.slug]);
    } catch (e) {
      const campos = erroresDeCampos(e);
      this.errores.set(campos);
      if (Object.keys(campos).length === 0) {
        this.error.set(mensajeDeError(e));
      }
    } finally {
      this.creando.set(false);
    }
  }
}
