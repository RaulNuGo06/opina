-- Cuenta de demostración, dueña de Bar La Plaza. Solo con el perfil "demo".
-- Email: dueno@barlaplaza.demo · Contraseña: demo-opina-2026 (aquí solo está su hash BCrypt)
insert into usuario (id, email, nombre, hash_contrasena)
values ('6f1c2a3e-0000-4000-8000-0000000000a1', 'dueno@barlaplaza.demo', 'Lucía (Bar La Plaza)',
        '{bcrypt}$2a$10$8Yh8tOniehbkJh5cIC6nce3nH69OnE1kV8srRPqJBGvooWTgcdAti');

insert into miembro (negocio_id, usuario_id, rol)
values ('6f1c2a3e-0000-4000-8000-000000000001', '6f1c2a3e-0000-4000-8000-0000000000a1', 'DUENO');
