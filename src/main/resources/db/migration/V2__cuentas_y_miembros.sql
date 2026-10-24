-- Personas que entran al panel. El email se guarda siempre en minúsculas.
create table usuario (
    id              uuid primary key default gen_random_uuid(),
    email           varchar(254) not null unique check (email = lower(email)),
    nombre          varchar(120) not null,
    hash_contrasena varchar(100) not null,
    creado_en       timestamptz  not null default now()
);

-- Quién puede ver el panel de cada negocio. Una persona puede llevar varios negocios.
--   DUENO:    ve las opiniones y gestiona quién más tiene acceso
--   EMPLEADO: ve las opiniones
create table miembro (
    id          uuid primary key default gen_random_uuid(),
    negocio_id  uuid        not null references negocio (id) on delete cascade,
    usuario_id  uuid        not null references usuario (id) on delete cascade,
    rol         varchar(10) not null check (rol in ('DUENO', 'EMPLEADO')),
    creado_en   timestamptz not null default now(),
    unique (negocio_id, usuario_id)
);

create index miembro_usuario on miembro (usuario_id);
