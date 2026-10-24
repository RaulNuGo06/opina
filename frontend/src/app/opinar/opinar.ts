import { Component, OnInit, inject, input, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Api, NegocioPublico, mensajeDeError } from '../api/api';

type Estado = 'cargando' | 'no-existe' | 'formulario' | 'enviada';

export const NOTAS = [
  { valor: 1, texto: 'Muy mal' },
  { valor: 2, texto: 'Mal' },
  { valor: 3, texto: 'Normal' },
  { valor: 4, texto: 'Bien' },
  { valor: 5, texto: 'Genial' },
];

/** Lo que ve el cliente al acercar el móvil a la tarjeta o escanear el QR. Sin cuenta y en pocos segundos. */
@Component({
  selector: 'app-opinar',
  imports: [ReactiveFormsModule],
  templateUrl: './opinar.html',
  styleUrl: './opinar.css',
})
export class Opinar implements OnInit {
  private readonly api = inject(Api);

  /** Llega de la ruta /n/:slug */
  readonly slug = input.required<string>();

  protected readonly notas = NOTAS;
  protected readonly estado = signal<Estado>('cargando');
  protected readonly negocio = signal<NegocioPublico | null>(null);
  protected readonly enviando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly urlGoogle = signal<string | null>(null);

  protected readonly nota = new FormControl<number | null>(null, Validators.required);
  protected readonly comentario = new FormControl('', {
    nonNullable: true,
    validators: Validators.maxLength(1000),
  });
  // Con [formGroup] en el <form>, Angular se encarga del envío y el navegador no recarga la página
  protected readonly formulario = new FormGroup({ nota: this.nota, comentario: this.comentario });
  protected readonly elegida = signal<number | null>(null);

  async ngOnInit() {
    try {
      this.negocio.set(await this.api.negocio(this.slug()));
      this.estado.set('formulario');
    } catch {
      this.estado.set('no-existe');
    }
  }

  protected elegir(valor: number) {
    this.nota.setValue(valor);
    this.elegida.set(valor);
  }

  protected async enviar() {
    if (this.nota.invalid || this.comentario.invalid || this.enviando()) {
      this.nota.markAsTouched();
      return;
    }
    this.enviando.set(true);
    this.error.set(null);
    try {
      const creada = await this.api.opinar(this.slug(), this.nota.value!, this.comentario.value);
      // El enlace a Google se ofrece sea cual sea la nota: filtrar a los descontentos va contra sus normas
      this.urlGoogle.set(creada.urlResenaGoogle);
      this.estado.set('enviada');
    } catch (e) {
      this.error.set(mensajeDeError(e));
    } finally {
      this.enviando.set(false);
    }
  }
}
