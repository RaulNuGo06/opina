-- Lo que la IA deduce de cada opinión. Los temas son una lista cerrada para poder contarlos y compararlos.
alter table opinion
    add column estado_ia      varchar(10) not null default 'PENDIENTE'
        check (estado_ia in ('PENDIENTE', 'HECHA', 'OMITIDA', 'FALLIDA')),
    add column intentos_ia    smallint    not null default 0,
    add column tema           varchar(12)
        check (tema in ('ATENCION', 'ESPERA', 'CALIDAD', 'PRECIO', 'LIMPIEZA', 'AMBIENTE', 'OTRO')),
    add column sentimiento    varchar(8)
        check (sentimiento in ('POSITIVO', 'NEUTRO', 'NEGATIVO')),
    add column urgente        boolean     not null default false,
    add column clasificada_en timestamptz;

-- Las opiniones que ya existían sin comentario no tienen nada que clasificar
update opinion set estado_ia = 'OMITIDA' where comentario is null;

-- El proceso en segundo plano solo busca las pendientes: índice parcial, pequeño aunque haya millones de opiniones
create index opinion_pendiente_ia on opinion (creada_en) where estado_ia = 'PENDIENTE';

-- Resumen escrito por la IA. Se guarda para no pagar dos veces por lo mismo:
-- solo se regenera si cambia la "huella" (cuántas opiniones hay en el periodo y cuál es la última).
create table resumen_ia (
    negocio_id   uuid        not null references negocio (id) on delete cascade,
    dias         smallint    not null,
    huella       varchar(80) not null,
    texto        text        not null,
    generado_en  timestamptz not null default now(),
    primary key (negocio_id, dias)
);
