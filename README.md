# Opina

[![CI](https://github.com/RaulNuGo06/opina/actions/workflows/ci.yml/badge.svg)](https://github.com/RaulNuGo06/opina/actions/workflows/ci.yml)

Opiniones de clientes para negocios locales, con resúmenes hechos por IA.

El cliente acerca el móvil a una tarjeta NFC o escanea un QR en la mesa, la peluquería o el gimnasio, y deja su opinión en 20 segundos. El negocio ve en un panel qué funciona y qué no. Por ejemplo: *"esta semana seis personas se quejan de la espera de los sábados"*.

> **Estado:** fase 1 de 4. Está hecha la API de negocios y opiniones. El panel en Angular, el inicio de sesión y la IA llegan en las siguientes fases ([hoja de ruta](#hoja-de-ruta)).

## Qué hace ya

| Método | Ruta | Para qué |
|---|---|---|
| `GET` | `/api/negocios/{slug}` | Nombre del negocio, para el formulario público |
| `POST` | `/api/negocios/{slug}/opiniones` | Guarda una opinión (nota de 1 a 5 y comentario opcional) |
| `GET` | `/api/negocios/{slug}/opiniones?pagina=0&tamano=20` | Opiniones de la más nueva a la más antigua |
| `GET` | `/api/negocios/{slug}/opiniones/resumen` | Total, nota media y cuántas hay de cada nota |

La documentación interactiva (Swagger UI) está en `/docs`.

```bash
curl -X POST localhost:8080/api/negocios/bar-la-plaza/opiniones \
  -H 'Content-Type: application/json' \
  -d '{"puntuacion": 2, "comentario": "Mucha espera en la terraza"}'
```
```json
{ "id": "a656f49e-…", "urlResenaGoogle": "https://search.google.com/local/writereview?placeid=…" }
```

Los errores siguen el estándar Problem Details (RFC 9457) e indican qué campo falla, para que el formulario pueda marcarlo:

```json
{
  "status": 400,
  "detail": "Hay datos que no son válidos",
  "errores": { "puntuacion": "La puntuación va de 1 a 5" }
}
```

## Decisiones técnicas

- **Sin filtrar a los clientes descontentos.** Muchas herramientas de reseñas solo mandan a Google a quien pone 4 o 5 estrellas. Eso se llama *review gating* y va contra las normas de Google. Opina devuelve el enlace de reseña sea cual sea la nota, y hay un test que lo comprueba (`tambienOfreceGoogleAQuienPuntuaBajo`). El valor para el negocio está en entender las opiniones, no en esconderlas.
- **El esquema lo controla Flyway, no Hibernate.** Las tablas, restricciones (`puntuacion between 1 and 5`, formato del slug) e índices se definen en SQL versionado. Hibernate arranca en modo `validate` y falla si las entidades no encajan con la base de datos.
- **Tests contra PostgreSQL real.** Testcontainers levanta la misma versión que en producción, así que lo que se prueba es lo que se despliega. Cada test crea su propio negocio, de modo que no dependen unos de otros. Uno de ellos comprueba que las opiniones de un negocio no aparecen en otro.
- **Validación en dos capas.** Bean Validation da mensajes claros a quien usa la API, y las restricciones de la base de datos protegen los datos aunque alguien se salte la API.
- **Organización por funcionalidad** (`negocio/`, `opinion/`) en lugar de por capas, para que cada parte se entienda leyendo una sola carpeta.

## Cómo arrancarlo

Necesitas Java 21 y PostgreSQL 17. Lo más sencillo para tener PostgreSQL es Docker:

```bash
docker compose up -d                                   # PostgreSQL en localhost:5432
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo # API en localhost:8080
```

El perfil `demo` carga **Bar La Plaza**, un bar inventado con 12 opiniones de ejemplo. Prueba con `GET /api/negocios/bar-la-plaza/opiniones/resumen`.

Para usar otra base de datos: `DB_URL`, `DB_USER` y `DB_PASSWORD`.

### Tests

```bash
./mvnw test                                                      # con Docker
DB_URL=jdbc:postgresql://localhost:5432/opina_test ./mvnw test   # sin Docker, contra un PostgreSQL propio
```

## Tecnologías

Java 21 · Spring Boot 4.1 · Spring Data JPA (Hibernate) · PostgreSQL 17 · Flyway · Bean Validation · springdoc-openapi · JUnit 5 · AssertJ · Testcontainers · GitHub Actions

## Hoja de ruta

- [x] **Fase 1:** API de negocios y opiniones, validación, tests y CI
- [ ] **Fase 2:** cuentas de negocio con Spring Security y JWT (dueño y empleado); el listado y el resumen pasan a ser privados
- [ ] **Fase 3:** formulario público y panel del negocio en Angular
- [ ] **Fase 4:** IA con la API de Claude: tema y urgencia de cada opinión, resumen semanal y propuesta de respuesta a cada queja
- [ ] Despliegue con una demo pública

## Autor

**Raúl Núñez González**: [portfolio](https://raulnugo06.github.io) · [LinkedIn](https://www.linkedin.com/in/ra%C3%BAl-n%C3%BA%C3%B1ez-gonz%C3%A1lez-17032a389) · [GitHub](https://github.com/RaulNuGo06)
