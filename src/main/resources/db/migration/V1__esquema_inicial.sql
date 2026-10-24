-- Un negocio recibe opiniones a través de su enlace público: /n/{slug}
create table negocio (
    id                uuid primary key default gen_random_uuid(),
    slug              varchar(60)  not null unique check (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
    nombre            varchar(120) not null,
    url_resena_google varchar(500),
    creado_en         timestamptz  not null default now()
);

create table opinion (
    id          uuid primary key default gen_random_uuid(),
    negocio_id  uuid        not null references negocio (id) on delete cascade,
    puntuacion  smallint    not null check (puntuacion between 1 and 5),
    comentario  varchar(1000),
    creada_en   timestamptz not null default now()
);

-- El panel siempre lista las opiniones de un negocio de la más nueva a la más antigua
create index opinion_negocio_fecha on opinion (negocio_id, creada_en desc);
