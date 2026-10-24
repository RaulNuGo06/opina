import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { Sesion } from './sesion';
import { sesionInterceptor } from './sesion.interceptor';

describe('sesionInterceptor', () => {
  let http: HttpClient;
  let servidor: HttpTestingController;
  let sesion: Sesion;

  const dentroDeUnaHora = () => new Date(Date.now() + 3600_000).toISOString();

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([sesionInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    servidor = TestBed.inject(HttpTestingController);
    sesion = TestBed.inject(Sesion);
  });

  afterEach(() => servidor.verify());

  it('añade el token a las llamadas a la API', () => {
    sesion.iniciar({ token: 'abc', caducaEn: dentroDeUnaHora() });

    http.get('/api/yo').subscribe();

    expect(servidor.expectOne('/api/yo').request.headers.get('Authorization')).toBe('Bearer abc');
  });

  it('no manda el token a otras webs', () => {
    sesion.iniciar({ token: 'abc', caducaEn: dentroDeUnaHora() });

    http.get('https://otra-web.com/datos').subscribe();

    expect(
      servidor.expectOne('https://otra-web.com/datos').request.headers.has('Authorization'),
    ).toBe(false);
  });

  it('ignora un token caducado', () => {
    sesion.iniciar({ token: 'viejo', caducaEn: new Date(Date.now() - 1000).toISOString() });

    http.get('/api/yo').subscribe();

    expect(servidor.expectOne('/api/yo').request.headers.has('Authorization')).toBe(false);
  });

  it('si el servidor rechaza la sesión, la cierra y manda a entrar', async () => {
    const router = TestBed.inject(Router);
    const navegar = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    sesion.iniciar({ token: 'abc', caducaEn: dentroDeUnaHora() });

    const peticion = firstValueFrom(http.get('/api/yo'));
    servidor
      .expectOne('/api/yo')
      .flush({ detail: 'La sesión no es válida' }, { status: 401, statusText: 'Unauthorized' });

    await expect(peticion).rejects.toBeTruthy();
    expect(sesion.activa()).toBe(false);
    expect(localStorage.getItem('opina.sesion')).toBeNull();
    expect(navegar).toHaveBeenCalledWith(['/entrar'], expect.anything());
  });
});
