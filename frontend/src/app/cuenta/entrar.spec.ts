import { destinoSeguro } from './entrar';

describe('destinoSeguro', () => {
  it('vuelve a la página del panel desde la que se pidió entrar', () => {
    expect(destinoSeguro('/panel/bar-la-plaza')).toBe('/panel/bar-la-plaza');
  });

  it('no sigue enlaces que lleven fuera del panel', () => {
    expect(destinoSeguro('https://web-falsa.com')).toBe('/panel');
    expect(destinoSeguro('//web-falsa.com/panel')).toBe('/panel');
    expect(destinoSeguro(undefined)).toBe('/panel');
  });
});
