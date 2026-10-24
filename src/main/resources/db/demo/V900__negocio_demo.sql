-- Datos de demostración: solo se cargan con el perfil "demo".
-- Bar La Plaza es un negocio inventado.
insert into negocio (id, slug, nombre, url_resena_google)
values ('6f1c2a3e-0000-4000-8000-000000000001', 'bar-la-plaza', 'Bar La Plaza',
        'https://search.google.com/local/writereview?placeid=DEMO');

insert into opinion (negocio_id, puntuacion, comentario, creada_en)
select '6f1c2a3e-0000-4000-8000-000000000001', o.puntuacion, o.comentario, now() - o.hace
from (values
    (5, 'Las tostadas de jamón, espectaculares. Volveremos.',                 interval '2 hours'),
    (4, 'Muy buen café y trato cercano. La terraza se llena pronto.',          interval '5 hours'),
    (2, 'Esperamos 25 minutos un sábado para que nos tomaran nota.',            interval '1 day'),
    (5, null,                                                                   interval '1 day 3 hours'),
    (3, 'La comida bien, pero el baño estaba sin jabón.',                      interval '2 days'),
    (5, 'El menú del día tiene una relación calidad-precio muy buena.',         interval '3 days'),
    (1, 'Nos cobraron dos cervezas de más y no nos lo corrigieron.',            interval '4 days'),
    (4, 'Raciones generosas. Un poco de ruido a la hora de comer.',             interval '5 days'),
    (2, 'Otra vez mucha espera en la terraza el sábado por la noche.',          interval '6 days'),
    (5, 'Camareros muy atentos con mi hijo celíaco.',                           interval '8 days'),
    (4, null,                                                                   interval '9 days'),
    (5, 'El mejor salmorejo de Almendralejo.',                                  interval '12 days')
) as o (puntuacion, comentario, hace);
