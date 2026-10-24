import { aSlug, haceCuanto, nota } from './textos';

describe('aSlug', () => {
  it('quita tildes, eñes, mayúsculas y signos', () => {
    expect(aSlug('Peluquería Ñandú (Zafra)')).toBe('peluqueria-nandu-zafra');
  });

  it('no deja guiones al principio, al final ni repetidos', () => {
    expect(aSlug('  ¡Bar   La Plaza!  ')).toBe('bar-la-plaza');
  });

  it('cumple el límite de 60 caracteres sin acabar en guion', () => {
    const slug = aSlug('a'.repeat(59) + ' bc');
    expect(slug.length).toBeLessThanOrEqual(60);
    expect(slug.endsWith('-')).toBe(false);
  });
});

describe('haceCuanto', () => {
  const ahora = new Date('2026-10-24T12:00:00Z').getTime();

  it('dice "ahora mismo" para menos de un minuto', () => {
    expect(haceCuanto('2026-10-24T11:59:30Z', ahora)).toBe('ahora mismo');
  });

  it('usa la unidad más grande que cabe', () => {
    expect(haceCuanto('2026-10-24T09:00:00Z', ahora)).toBe('hace 3 horas');
    expect(haceCuanto('2026-10-23T12:00:00Z', ahora)).toBe('ayer');
    expect(haceCuanto('2026-10-10T12:00:00Z', ahora)).toBe('hace 2 semanas');
  });
});

describe('nota', () => {
  it('muestra un decimal con coma', () => {
    expect(nota(3.8)).toBe('3,8');
    expect(nota(4)).toBe('4,0');
  });
});
