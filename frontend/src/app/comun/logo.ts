import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

/** El nombre lleva "IA" dentro: op-I-n-A. Esas dos letras van en ámbar, el color de las notas. */
@Component({
  selector: 'app-logo',
  imports: [RouterLink],
  template: `
    <a class="logo" [routerLink]="enlace()">
      <img src="favicon.svg" alt="" />
      <span>op<span class="ia">I</span>n<span class="ia">A</span></span>
    </a>
  `,
  styles: `
    .logo {
      display: inline-flex;
      align-items: center;
      gap: 0.45rem;
      font:
        800 1.3rem/1 'Bricolage Grotesque',
        system-ui,
        sans-serif;
      letter-spacing: -0.01em;
      color: var(--tinta);
      text-decoration: none;
    }
    img {
      width: 26px;
      height: 26px;
    }
    .ia {
      color: var(--ambar-texto);
    }
  `,
})
export class Logo {
  readonly enlace = input('/');
}
