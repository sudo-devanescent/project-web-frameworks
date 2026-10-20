# Base de datos HostOps

Esquema y datos de ejemplo del sistema de gestión hotelera sobre **PostgreSQL 17**.

## Contenido

| Archivo | Descripción |
| --- | --- |
| `hostops_schema.sql` | DDL: extensiones, tipos ENUM, 11 tablas, índices, funciones/triggers y 2 vistas. Incluye los roles y canales iniciales. |
| `hostops_seed.sql` | Datos de ejemplo (usuarios, habitaciones, huéspedes, reservas, pagos, etc.). Debe ejecutarse **después** del esquema sobre una base recién creada. |

## Requisitos

- PostgreSQL 17 (la imagen de referencia es `postgres:17`).
- Extensión `btree_gist` (la crea el propio esquema).
- Los dos scripts están en LF. Se recomienda ejecutarlos con `ON_ERROR_STOP=1`.

## Crear la base con Podman/Docker

```bash
podman run -d --name hostops-db \
  -e POSTGRES_DB=hostops \
  -e POSTGRES_USER=hostops \
  -e POSTGRES_PASSWORD=<contraseña> \
  -p 127.0.0.1:5432:5432 \
  -v hostops-pgdata:/var/lib/postgresql/data \
  docker.io/library/postgres:17
```

## Cargar esquema y datos

Sobre una base vacía, en este orden:

```bash
podman exec -i hostops-db psql -U hostops -d hostops -v ON_ERROR_STOP=1 < db/hostops_schema.sql
podman exec -i hostops-db psql -U hostops -d hostops -v ON_ERROR_STOP=1 < db/hostops_seed.sql
```

`schema.sql` es idempotente en los datos iniciales (`ON CONFLICT DO NOTHING`), pero
**no** recrea tablas: para reiniciar usa la sección 7 comentada del propio script.

## Conexión desde la aplicación

La aplicación lee la conexión de variables de entorno (sin valores por defecto para la contraseña):

| Variable | Valor por defecto |
| --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/hostops` |
| `DB_USER` | `hostops` |
| `DB_PASSWORD` | *(sin valor por defecto; es obligatoria)* |

Hibernate está configurado con `ddl-auto=validate`: al arrancar comprueba que las
entidades coinciden con el esquema y **no** modifica la base de datos.

## Notas

- El modelo JPA vive en `src/main/java/com/hostops/hotelmanagement/models/`.
- Las reglas de negocio (CHECK, UNIQUE, EXCLUDE y triggers) se aplican en la base
  de datos; las entidades no las duplican.
- Las vistas `v_saldo_reserva` y `v_noches_ocupadas_por_mes` no tienen entidad asociada.
