import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Opinar } from './opinar';

describe('Opinar', () => {
  let fixture: ComponentFixture<Opinar>;
  let servidor: HttpTestingController;
  let pantalla: HTMLElement;

  async function abrir(slug = 'bar-la-plaza') {
    TestBed.configureTestingModule({
      imports: [Opinar],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    servidor = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Opinar);
    fixture.componentRef.setInput('slug', slug);
    pantalla = fixture.nativeElement;
    fixture.detectChanges();
  }

  async function responder(url: string, cuerpo: object, estado = 200) {
    servidor
      .expectOne(url)
      .flush(cuerpo, { status: estado, statusText: estado === 200 ? 'OK' : 'Error' });
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function elegirNota(n: number) {
    pantalla
      .querySelector<HTMLInputElement>(`input[type=radio][value="${n}"]`)!
      .dispatchEvent(new Event('change'));
    fixture.detectChanges();
  }

  async function enviar() {
    pantalla.querySelector('form')!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  afterEach(() => servidor.verify());

  it('saluda con el nombre del negocio', async () => {
    await abrir();
    await responder('/api/negocios/bar-la-plaza', { slug: 'bar-la-plaza', nombre: 'Bar La Plaza' });

    expect(pantalla.textContent).toContain('Bar La Plaza');
    expect(pantalla.querySelectorAll('input[type=radio]').length).toBe(5);
  });

  it('avisa si el enlace no es de ningún negocio', async () => {
    await abrir('no-existe');
    await responder('/api/negocios/no-existe', { detail: 'No existe' }, 404);

    expect(pantalla.querySelector('h1')!.textContent).toContain('Este enlace no funciona');
  });

  it('no envía nada hasta elegir una nota', async () => {
    await abrir();
    await responder('/api/negocios/bar-la-plaza', { slug: 'bar-la-plaza', nombre: 'Bar La Plaza' });

    await enviar();

    servidor.expectNone('/api/negocios/bar-la-plaza/opiniones');
    expect(pantalla.textContent).toContain('Elige una nota del 1 al 5');
  });

  it('envía la nota y el comentario, y ofrece Google aunque la nota sea baja', async () => {
    await abrir();
    await responder('/api/negocios/bar-la-plaza', { slug: 'bar-la-plaza', nombre: 'Bar La Plaza' });
    elegirNota(1);
    const comentario = pantalla.querySelector('textarea')!;
    comentario.value = 'La comida llegó fría';
    comentario.dispatchEvent(new Event('input'));

    await enviar();
    const peticion = servidor.expectOne('/api/negocios/bar-la-plaza/opiniones');
    expect(peticion.request.body).toEqual({ puntuacion: 1, comentario: 'La comida llegó fría' });
    peticion.flush({ id: '1', urlResenaGoogle: 'https://g.page/r/bar/review' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(pantalla.querySelector('h1')!.textContent).toContain('¡Gracias!');
    expect(pantalla.querySelector<HTMLAnchorElement>('a.boton')!.href).toBe(
      'https://g.page/r/bar/review',
    );
  });
});
