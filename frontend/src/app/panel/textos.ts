import { Tema } from '../api/api';

/** "Bar La Plaza (Almendralejo)" → "bar-la-plaza-almendralejo": mismas reglas que el backend para el enlace. */
export function aSlug(nombre: string): string {
  return nombre
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '') // quita tildes: "Peluquería" → "Peluqueria"
    .toLowerCase()
    .replace(/ñ/g, 'n')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 60)
    .replace(/-+$/, '');
}

/** Cómo se llama cada tema en el panel */
export const TEMAS: Record<Tema, string> = {
  ATENCION: 'Atención',
  ESPERA: 'Espera',
  CALIDAD: 'Calidad',
  PRECIO: 'Precio',
  LIMPIEZA: 'Limpieza',
  AMBIENTE: 'Ambiente',
  OTRO: 'Otros',
};

const relativo = new Intl.RelativeTimeFormat('es', { numeric: 'auto' });

const PASOS: [Intl.RelativeTimeFormatUnit, number][] = [
  ['year', 365 * 24 * 3600],
  ['month', 30 * 24 * 3600],
  ['week', 7 * 24 * 3600],
  ['day', 24 * 3600],
  ['hour', 3600],
  ['minute', 60],
];

/** "hace 5 minutos", "ayer", "hace 3 semanas" */
export function haceCuanto(fecha: string, ahora = Date.now()): string {
  const segundos = Math.round((new Date(fecha).getTime() - ahora) / 1000);
  for (const [unidad, tamano] of PASOS) {
    if (Math.abs(segundos) >= tamano) {
      return relativo.format(Math.round(segundos / tamano), unidad);
    }
  }
  return 'ahora mismo';
}

const unDecimal = new Intl.NumberFormat('es', {
  minimumFractionDigits: 1,
  maximumFractionDigits: 1,
});

/** 3.8 → "3,8" */
export function nota(media: number): string {
  return unDecimal.format(media);
}
