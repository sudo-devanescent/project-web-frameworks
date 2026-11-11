# Hotel Management System

Sistema de gestión hotelera con panel web y API REST: reservas, habitaciones, huéspedes, check-in/out, pagos y finanzas.

## Requisitos

- JDK 21 (verificado con Temurin 21.0.12).
- PostgreSQL 17 (imagen de referencia `docker.io/library/postgres:17`).
- Gradle no necesita instalación: se usa el wrapper `./gradlew`.

## Base de datos

Crea el contenedor y carga los scripts en este orden sobre la base vacía (detalles en [db/README.md](db/README.md)):

```bash
podman run -d --name hostops-db \
  -e POSTGRES_DB=hostops -e POSTGRES_USER=hostops -e POSTGRES_PASSWORD=<contrasena> \
  -p 127.0.0.1:5432:5432 -v hostops-pgdata:/var/lib/postgresql/data \
  docker.io/library/postgres:17

podman exec -i hostops-db psql -U hostops -d hostops -v ON_ERROR_STOP=1 < db/hostops_schema.sql
podman exec -i hostops-db psql -U hostops -d hostops -v ON_ERROR_STOP=1 < db/hostops_seed.sql
```

`db/hostops_migration_001_auditoria_pagos.sql` **no** hace falta en una base nueva: su contenido ya está en
`hostops_schema.sql`; sólo aplica a bases pobladas antiguas (es idempotente).

La conexión se configura por variables de entorno (`src/main/resources/application.yaml`), que deben coincidir con el contenedor:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/hostops  # opcional (es el valor por defecto)
export DB_USER=hostops                                  # opcional (valor por defecto)
export DB_PASSWORD=<la misma de POSTGRES_PASSWORD>      # obligatoria
```

Hibernate usa `ddl-auto=validate`: la aplicación no modifica el esquema.

## Ejecución

```bash
./gradlew bootRun
```

Abre <http://localhost:8080/> (puerto 8080). La pantalla inicial es el login.

## Acceso

| Rol | Usuario | Contraseña | Qué puede hacer |
| --- | --- | --- | --- |
| administrador | `jvidaurre` | `HostOps2026` | Usuarios, catálogos, compensaciones |
| recepcionista | `rsantos` | `HostOps2026` | Recepción, reservas y pagos |
| recepcionista | `pcarrasco` | `HostOps2026` | Recepción, reservas y pagos |

Credenciales de desarrollo del seed (`db/hostops_seed.sql`). Para crear otro usuario: `POST /api/usuarios`
(requiere sesión de administrador) o bien actualiza `contrasena_hash` con un hash BCrypt.

## Tests

```bash
./gradlew test
```

Requiere PostgreSQL levantado con esquema y seed cargados:
las pruebas de integración inician sesión con los usuarios del seed.

## Estructura básica

```text
src/main/java/com/hostops/hotelmanagement/
├── controllers/   # WebController y api/ (auth, reservas, habitaciones, pagos, finanzas...)
├── services/      # lógica de negocio
├── repositories/  # Spring Data JPA
├── models/        # entidades JPA
├── security/      # sesión y usuario autenticado
└── config/        # SecurityConfig y configuración
src/main/resources/
├── templates/index.html   # shell de la SPA (Thymeleaf)
└── static/                # css/ y js/ (SPA vanilla, sin paso de build)
db/                        # hostops_schema.sql, hostops_seed.sql, migración 001
```
