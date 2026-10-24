-- Las opiniones de demostración ya clasificadas, para que el panel enseñe temas y urgencias sin clave de API.
-- Con clave, las opiniones nuevas las clasifica la IA en segundo plano.
update opinion set estado_ia = 'OMITIDA'
where negocio_id = '6f1c2a3e-0000-4000-8000-000000000001' and comentario is null;

update opinion o
set tema = c.tema, sentimiento = c.sentimiento, urgente = c.urgente, estado_ia = 'HECHA', clasificada_en = now()
from (values
    ('Las tostadas de jamón%',          'CALIDAD',  'POSITIVO', false),
    ('Muy buen café%',                  'ATENCION', 'POSITIVO', false),
    ('Esperamos 25 minutos%',           'ESPERA',   'NEGATIVO', false),
    ('La comida bien, pero el baño%',   'LIMPIEZA', 'NEGATIVO', true),
    ('El menú del día%',                'PRECIO',   'POSITIVO', false),
    ('Nos cobraron dos cervezas%',      'PRECIO',   'NEGATIVO', true),
    ('Raciones generosas%',             'CALIDAD',  'POSITIVO', false),
    ('Otra vez mucha espera%',          'ESPERA',   'NEGATIVO', false),
    ('Camareros muy atentos%',          'ATENCION', 'POSITIVO', false),
    ('El mejor salmorejo%',             'CALIDAD',  'POSITIVO', false)
) as c (comienzo, tema, sentimiento, urgente)
where o.negocio_id = '6f1c2a3e-0000-4000-8000-000000000001' and o.comentario like c.comienzo;
