import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Informe as DatosInforme } from '../api/api';
import { Informe } from './informe';

describe('Informe', () => {
  let fixture: ComponentFixture<Informe>;
  let servidor: HttpTestingController;
  let pantalla: HTMLElement;

  const base: DatosInforme = {
    dias: 7,
    total: 5,
    porTema: { ATENCION: 1, ESPERA: 3, CALIDAD: 0, PRECIO: 0, LIMPIEZA: 1, AMBIENTE: 0, OTRO: 0 },
    urgentes: [
      {
        id: 'u1',
        puntuacion: 1,
        comentario: 'Había una cucaracha',
        creadaEn: new Date().toISOString(),
        tema: 'LIMPIEZA',
        sentimiento: 'NEGATIVO',
        urgente: true,
      },
    ],
    pendientes: 0,
    iaActiva: true,
    resumen: 'Tres personas se quejan de la espera.',
    resumenGeneradoEn: new Date().toISOString(),
  };

  async function abrir(datos: DatosInforme) {
    TestBed.configureTestingModule({
      imports: [Informe],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    servidor = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Informe);
    fixture.componentRef.setInput('slug', 'bar-la-plaza');
    pantalla = fixture.nativeElement;
    fixture.detectChanges();
    await fixture.whenStable();
    servidor
      .expectOne((r) => r.url === '/api/negocios/bar-la-plaza/opiniones/informe')
      .flush(datos);
    await fixture.whenStable();
    fixture.detectChanges();
  }

  afterEach(() => servidor.verify());

  it('enseña el resumen de la IA, lo urgente y los temas de más a menos', async () => {
    await abrir(base);

    expect(pantalla.querySelector('.resumen-ia')!.textContent).toContain(
      'Tres personas se quejan de la espera.',
    );
    expect(pantalla.querySelector('.urgentes')!.textContent).toContain('Había una cucaracha');
    const temas = [...pantalla.querySelectorAll('.temas .nombre')].map((e) =>
      e.textContent!.trim(),
    );
    expect(temas).toEqual(['Espera', 'Atención', 'Limpieza']); // sin los temas a cero
  });

  it('avisa de las opiniones que la IA aún está analizando', async () => {
    await abrir({ ...base, pendientes: 2 });

    expect(pantalla.querySelector('.analizando')!.textContent).toContain(
      'Analizando 2 opiniones nuevas',
    );
  });

  it('sin IA no hay resumen ni aviso, pero sí las cifras', async () => {
    await abrir({
      ...base,
      iaActiva: false,
      resumen: null,
      resumenGeneradoEn: null,
      pendientes: 3,
    });

    expect(pantalla.querySelector('.resumen-ia')).toBeNull();
    expect(pantalla.querySelector('.analizando')).toBeNull();
    expect(pantalla.querySelectorAll('.temas li').length).toBe(3);
  });

  it('pide otro periodo al cambiarlo', async () => {
    await abrir(base);

    pantalla.querySelectorAll<HTMLButtonElement>('.periodos button')[1].click();
    fixture.detectChanges();
    await fixture.whenStable();

    const peticion = servidor.expectOne((r) => r.url.endsWith('/informe'));
    expect(peticion.request.params.get('dias')).toBe('30');
    peticion.flush({ ...base, dias: 30 });
  });
});
