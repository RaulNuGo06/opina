import { Routes } from '@angular/router';
import { conSesion } from './sesion/sesion.guard';

export const routes: Routes = [
  // Lo que abre el cliente desde la tarjeta NFC o el QR
  {
    path: 'n/:slug',
    loadComponent: () => import('./opinar/opinar').then((m) => m.Opinar),
    title: 'Danos tu opinión',
  },

  {
    path: 'entrar',
    loadComponent: () => import('./cuenta/entrar').then((m) => m.Entrar),
    title: 'Entrar · Opina',
  },
  {
    path: 'registro',
    loadComponent: () => import('./cuenta/registro').then((m) => m.Registro),
    title: 'Crear cuenta · Opina',
  },

  // Panel del negocio: hace falta sesión
  {
    path: 'panel',
    canActivate: [conSesion],
    loadComponent: () => import('./panel/marco').then((m) => m.Marco),
    children: [
      {
        path: '',
        loadComponent: () => import('./panel/mis-negocios').then((m) => m.MisNegocios),
        title: 'Mis negocios · Opina',
      },
      {
        path: ':slug',
        loadComponent: () => import('./panel/negocio').then((m) => m.NegocioPanel),
        title: 'Panel · Opina',
      },
    ],
  },

  { path: '', pathMatch: 'full', redirectTo: 'panel' },
  { path: '**', redirectTo: 'panel' },
];
