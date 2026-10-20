-- =====================================================================
-- HostOps - Sistema de Gestión Hotelera
-- Script DDL para PostgreSQL (proyecto académico)
-- =====================================================================
-- Contenido:
--   0. Extensiones
--   1. Tipos ENUM
--   2. Tablas (ordenadas por dependencia)
--   3. Índices
--   4. Funciones y triggers (reglas de negocio)
--   5. Vistas (saldo de reserva y ocupación)
--   6. Datos iniciales (roles y canales)
--   7. Script de reinicio (comentado)
--
-- Decisiones de diseño:
--   * Un solo hotel.
--   * Claves primarias: INTEGER GENERATED ALWAYS AS IDENTITY.
--   * Valores controlados: tipos ENUM.
--   * El estado vive en detalle_reserva; la reserva no lo guarda.
--   * Todas las FK usan ON DELETE RESTRICT (nada se borra en cascada).
-- =====================================================================

BEGIN;

-- ---------------------------------------------------------------------
-- 0. EXTENSIONES
-- ---------------------------------------------------------------------
-- btree_gist permite combinar igualdad (=) y solapamiento (&&) de rangos
-- en una misma restricción EXCLUDE (necesaria para evitar dobles reservas).
CREATE EXTENSION IF NOT EXISTS btree_gist;


-- ---------------------------------------------------------------------
-- 1. TIPOS ENUM
-- ---------------------------------------------------------------------
CREATE TYPE tipo_habitacion AS ENUM
    ('simple', 'doble', 'matrimonial', 'triple', 'suite');

CREATE TYPE estado_habitacion AS ENUM
    ('disponible', 'ocupada', 'limpieza', 'mantenimiento');

-- Estado de cada habitación dentro de una reserva
CREATE TYPE estado_detalle AS ENUM
    ('confirmada', 'en_estadia', 'finalizada', 'cancelada');

CREATE TYPE tipo_movimiento_pago AS ENUM
    ('anticipo', 'saldo', 'reembolso');

CREATE TYPE metodo_pago AS ENUM
    ('efectivo', 'tarjeta', 'transferencia', 'billetera_digital');


-- ---------------------------------------------------------------------
-- 2. TABLAS
-- ---------------------------------------------------------------------

-- 2.1 Rol: perfiles de acceso del personal (administrador, recepcionista)
CREATE TABLE rol (
    id_rol   INTEGER GENERATED ALWAYS AS IDENTITY,
    nombre   VARCHAR(50) NOT NULL,
    CONSTRAINT pk_rol        PRIMARY KEY (id_rol),
    CONSTRAINT uq_rol_nombre UNIQUE (nombre)
);

-- 2.2 Usuario: empleados que operan el sistema
CREATE TABLE usuario (
    id_usuario      INTEGER GENERATED ALWAYS AS IDENTITY,
    id_rol          INTEGER      NOT NULL,
    nombre_usuario  VARCHAR(50)  NOT NULL,
    nombre_completo VARCHAR(120) NOT NULL,
    correo          VARCHAR(120) NOT NULL,
    contrasena_hash VARCHAR(255) NOT NULL,  -- nunca se guarda la contraseña en claro
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_usuario        PRIMARY KEY (id_usuario),
    CONSTRAINT uq_usuario_nombre UNIQUE (nombre_usuario),
    CONSTRAINT uq_usuario_correo UNIQUE (correo),
    CONSTRAINT fk_usuario_rol    FOREIGN KEY (id_rol)
        REFERENCES rol (id_rol) ON DELETE RESTRICT
);

-- 2.3 Canal: origen de la reserva (teléfono, WhatsApp, presencial, etc.)
CREATE TABLE canal (
    id_canal INTEGER GENERATED ALWAYS AS IDENTITY,
    nombre   VARCHAR(50) NOT NULL,
    CONSTRAINT pk_canal        PRIMARY KEY (id_canal),
    CONSTRAINT uq_canal_nombre UNIQUE (nombre)
);

-- 2.4 Habitación: atributos directos (tipo, capacidad, tarifa y estado actual)
CREATE TABLE habitacion (
    id_habitacion INTEGER GENERATED ALWAYS AS IDENTITY,
    numero        VARCHAR(10)       NOT NULL,
    tipo          tipo_habitacion   NOT NULL,
    capacidad     SMALLINT          NOT NULL,
    tarifa        NUMERIC(10,2)     NOT NULL,   -- tarifa vigente por noche
    estado        estado_habitacion NOT NULL DEFAULT 'disponible',
    CONSTRAINT pk_habitacion        PRIMARY KEY (id_habitacion),
    CONSTRAINT uq_habitacion_numero UNIQUE (numero),
    CONSTRAINT ck_habitacion_capacidad CHECK (capacidad > 0),
    CONSTRAINT ck_habitacion_tarifa    CHECK (tarifa >= 0)
);

-- 2.5 Bloqueo de habitación: periodos fuera de servicio (mantenimiento)
--     El rango es inclusivo en ambos extremos: [fecha_inicio, fecha_fin].
CREATE TABLE bloqueo_habitacion (
    id_bloqueo    INTEGER GENERATED ALWAYS AS IDENTITY,
    id_habitacion INTEGER NOT NULL,
    fecha_inicio  DATE    NOT NULL,
    fecha_fin     DATE    NOT NULL,
    motivo        TEXT    NOT NULL,
    CONSTRAINT pk_bloqueo PRIMARY KEY (id_bloqueo),
    CONSTRAINT fk_bloqueo_habitacion FOREIGN KEY (id_habitacion)
        REFERENCES habitacion (id_habitacion) ON DELETE RESTRICT,
    CONSTRAINT ck_bloqueo_fechas CHECK (fecha_fin >= fecha_inicio),
    -- Una misma habitación no puede tener dos bloqueos que se solapen
    CONSTRAINT ex_bloqueo_sin_solape EXCLUDE USING gist (
        id_habitacion WITH =,
        daterange(fecha_inicio, fecha_fin, '[]') WITH &&
    )
);

-- 2.6 Huésped: datos personales y de contacto
--     La identidad se define por (tipo_documento, numero_documento).
CREATE TABLE huesped (
    id_huesped       INTEGER GENERATED ALWAYS AS IDENTITY,
    tipo_documento   VARCHAR(20)  NOT NULL,   -- p. ej.: DNI, pasaporte, CE
    numero_documento VARCHAR(30)  NOT NULL,
    nombres          VARCHAR(100) NOT NULL,
    apellidos        VARCHAR(100) NOT NULL,
    telefono         VARCHAR(30)  NOT NULL,
    correo           VARCHAR(120),            -- opcional
    nacionalidad     VARCHAR(60)  NOT NULL,
    fecha_nacimiento DATE         NOT NULL,
    preferencias     TEXT,                    -- texto libre (opcional)
    CONSTRAINT pk_huesped           PRIMARY KEY (id_huesped),
    CONSTRAINT uq_huesped_documento UNIQUE (tipo_documento, numero_documento)
);

-- 2.7 Reserva: cabecera. No guarda estado ni fechas (viven en el detalle).
CREATE TABLE reserva (
    id_reserva              INTEGER GENERATED ALWAYS AS IDENTITY,
    id_huesped_titular      INTEGER     NOT NULL,   -- responsable de la reserva
    id_canal                INTEGER     NOT NULL,
    id_usuario_creador      INTEGER     NOT NULL,
    id_usuario_modificador  INTEGER,                -- NULL si nunca se modificó
    fecha_creacion          TIMESTAMPTZ NOT NULL DEFAULT now(),
    fecha_modificacion      TIMESTAMPTZ,            -- la actualiza un trigger
    CONSTRAINT pk_reserva PRIMARY KEY (id_reserva),
    CONSTRAINT fk_reserva_titular FOREIGN KEY (id_huesped_titular)
        REFERENCES huesped (id_huesped) ON DELETE RESTRICT,
    CONSTRAINT fk_reserva_canal FOREIGN KEY (id_canal)
        REFERENCES canal (id_canal) ON DELETE RESTRICT,
    CONSTRAINT fk_reserva_creador FOREIGN KEY (id_usuario_creador)
        REFERENCES usuario (id_usuario) ON DELETE RESTRICT,
    CONSTRAINT fk_reserva_modificador FOREIGN KEY (id_usuario_modificador)
        REFERENCES usuario (id_usuario) ON DELETE RESTRICT
);

-- 2.8 Detalle de reserva: una habitación dentro de una reserva
--     Las noches ocupadas son el rango [entrada, salida): la noche de la
--     fecha de salida no se cuenta.
CREATE TABLE detalle_reserva (
    id_detalle              INTEGER GENERATED ALWAYS AS IDENTITY,
    id_reserva              INTEGER        NOT NULL,
    id_habitacion           INTEGER        NOT NULL,
    fecha_entrada_prevista  DATE           NOT NULL,
    fecha_salida_prevista   DATE           NOT NULL,
    checkin_real            TIMESTAMPTZ,
    checkout_real           TIMESTAMPTZ,
    tarifa_noche            NUMERIC(10,2)  NOT NULL,  -- tarifa congelada (histórico)
    estado                  estado_detalle NOT NULL DEFAULT 'confirmada',
    CONSTRAINT pk_detalle_reserva PRIMARY KEY (id_detalle),
    CONSTRAINT fk_detalle_reserva FOREIGN KEY (id_reserva)
        REFERENCES reserva (id_reserva) ON DELETE RESTRICT,
    CONSTRAINT fk_detalle_habitacion FOREIGN KEY (id_habitacion)
        REFERENCES habitacion (id_habitacion) ON DELETE RESTRICT,
    CONSTRAINT ck_detalle_fechas_previstas
        CHECK (fecha_salida_prevista > fecha_entrada_prevista),
    CONSTRAINT ck_detalle_tarifa CHECK (tarifa_noche >= 0),
    CONSTRAINT ck_detalle_checkout_posterior
        CHECK (checkout_real IS NULL OR checkin_real IS NULL
               OR checkout_real >= checkin_real),
    -- Coherencia entre el estado y las fechas reales de check-in/out
    CONSTRAINT ck_detalle_estado_fechas CHECK (
           (estado = 'confirmada' AND checkin_real IS NULL     AND checkout_real IS NULL)
        OR (estado = 'en_estadia' AND checkin_real IS NOT NULL AND checkout_real IS NULL)
        OR (estado = 'finalizada' AND checkin_real IS NOT NULL AND checkout_real IS NOT NULL)
        OR (estado = 'cancelada'  AND checkin_real IS NULL     AND checkout_real IS NULL)
    ),
    -- REGLA CLAVE: una habitación no puede tener dos detalles no cancelados
    -- con fechas que se solapen (evita las reservas duplicadas).
    CONSTRAINT ex_detalle_sin_solape EXCLUDE USING gist (
        id_habitacion WITH =,
        daterange(fecha_entrada_prevista, fecha_salida_prevista, '[)') WITH &&
    ) WHERE (estado <> 'cancelada')
);

-- 2.9 Detalle-huésped (N:M): huéspedes alojados en cada habitación reservada
CREATE TABLE detalle_huesped (
    id_detalle INTEGER NOT NULL,
    id_huesped INTEGER NOT NULL,
    CONSTRAINT pk_detalle_huesped PRIMARY KEY (id_detalle, id_huesped),
    CONSTRAINT fk_dh_detalle FOREIGN KEY (id_detalle)
        REFERENCES detalle_reserva (id_detalle) ON DELETE RESTRICT,
    CONSTRAINT fk_dh_huesped FOREIGN KEY (id_huesped)
        REFERENCES huesped (id_huesped) ON DELETE RESTRICT
);

-- 2.10 Cancelación: una por detalle como máximo (relación 1 : 0..1)
CREATE TABLE cancelacion (
    id_cancelacion    INTEGER GENERATED ALWAYS AS IDENTITY,
    id_detalle        INTEGER       NOT NULL,
    fecha_cancelacion TIMESTAMPTZ   NOT NULL DEFAULT now(),
    motivo            TEXT          NOT NULL,
    penalidad         NUMERIC(10,2) NOT NULL DEFAULT 0,
    CONSTRAINT pk_cancelacion PRIMARY KEY (id_cancelacion),
    CONSTRAINT uq_cancelacion_detalle UNIQUE (id_detalle),
    CONSTRAINT fk_cancelacion_detalle FOREIGN KEY (id_detalle)
        REFERENCES detalle_reserva (id_detalle) ON DELETE RESTRICT,
    CONSTRAINT ck_cancelacion_penalidad CHECK (penalidad >= 0)
);

-- 2.11 Pago: movimientos de dinero asociados a la reserva
--      El monto siempre es positivo; el tipo (anticipo, saldo, reembolso)
--      indica si el dinero entra o sale.
CREATE TABLE pago (
    id_pago    INTEGER GENERATED ALWAYS AS IDENTITY,
    id_reserva INTEGER              NOT NULL,
    tipo       tipo_movimiento_pago NOT NULL,
    metodo     metodo_pago          NOT NULL,
    fecha_pago TIMESTAMPTZ          NOT NULL DEFAULT now(),
    monto      NUMERIC(10,2)        NOT NULL,
    CONSTRAINT pk_pago PRIMARY KEY (id_pago),
    CONSTRAINT fk_pago_reserva FOREIGN KEY (id_reserva)
        REFERENCES reserva (id_reserva) ON DELETE RESTRICT,
    CONSTRAINT ck_pago_monto CHECK (monto > 0)
);


-- ---------------------------------------------------------------------
-- 3. ÍNDICES
-- ---------------------------------------------------------------------
-- PostgreSQL no indexa automáticamente las columnas de clave foránea.
-- (La búsqueda por habitación en detalle_reserva ya la cubre el índice
-- GiST de la restricción ex_detalle_sin_solape.)
CREATE INDEX ix_usuario_rol            ON usuario (id_rol);
CREATE INDEX ix_reserva_titular        ON reserva (id_huesped_titular);
CREATE INDEX ix_reserva_canal          ON reserva (id_canal);
CREATE INDEX ix_reserva_creador        ON reserva (id_usuario_creador);
CREATE INDEX ix_reserva_modificador    ON reserva (id_usuario_modificador);
CREATE INDEX ix_detalle_reserva        ON detalle_reserva (id_reserva);
CREATE INDEX ix_detalle_fecha_entrada  ON detalle_reserva (fecha_entrada_prevista);
CREATE INDEX ix_detalle_huesped_huesp  ON detalle_huesped (id_huesped);
CREATE INDEX ix_pago_reserva           ON pago (id_reserva);
CREATE INDEX ix_huesped_apellidos      ON huesped (apellidos, nombres);


-- ---------------------------------------------------------------------
-- 4. FUNCIONES Y TRIGGERS (reglas que un CHECK no puede expresar)
-- ---------------------------------------------------------------------

-- 4.1 Actualiza la fecha de modificación de la reserva en cada UPDATE
CREATE FUNCTION fn_actualizar_fecha_modificacion()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    NEW.fecha_modificacion := now();
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_reserva_fecha_modificacion
    BEFORE UPDATE ON reserva
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_fecha_modificacion();


-- 4.2 Un detalle no puede cruzarse con un bloqueo de mantenimiento
CREATE FUNCTION fn_validar_detalle_vs_bloqueo()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.estado <> 'cancelada' AND EXISTS (
        SELECT 1
        FROM bloqueo_habitacion b
        WHERE b.id_habitacion = NEW.id_habitacion
          AND daterange(b.fecha_inicio, b.fecha_fin, '[]')
              && daterange(NEW.fecha_entrada_prevista, NEW.fecha_salida_prevista, '[)')
    ) THEN
        RAISE EXCEPTION
            'La habitación % tiene un bloqueo que se cruza con las fechas de la reserva',
            NEW.id_habitacion
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_detalle_vs_bloqueo
    BEFORE INSERT OR UPDATE OF id_habitacion, fecha_entrada_prevista,
                               fecha_salida_prevista, estado
    ON detalle_reserva
    FOR EACH ROW EXECUTE FUNCTION fn_validar_detalle_vs_bloqueo();


-- 4.3 Un bloqueo no puede cruzarse con reservas activas (confirmadas o en estadía)
CREATE FUNCTION fn_validar_bloqueo_vs_detalle()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM detalle_reserva d
        WHERE d.id_habitacion = NEW.id_habitacion
          AND d.estado IN ('confirmada', 'en_estadia')
          AND daterange(d.fecha_entrada_prevista, d.fecha_salida_prevista, '[)')
              && daterange(NEW.fecha_inicio, NEW.fecha_fin, '[]')
    ) THEN
        RAISE EXCEPTION
            'El bloqueo se cruza con reservas activas de la habitación %',
            NEW.id_habitacion
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_bloqueo_vs_detalle
    BEFORE INSERT OR UPDATE OF id_habitacion, fecha_inicio, fecha_fin
    ON bloqueo_habitacion
    FOR EACH ROW EXECUTE FUNCTION fn_validar_bloqueo_vs_detalle();


-- 4.4 Capacidad: no se asignan más huéspedes que la capacidad de la habitación
--     Limitación: no se revalida si luego cambia la capacidad de la habitación
--     o el detalle se mueve a otra habitación.
CREATE FUNCTION fn_validar_capacidad_detalle()
RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    v_capacidad SMALLINT;
    v_ocupantes INTEGER;
BEGIN
    -- FOR UPDATE serializa asignaciones simultáneas sobre el mismo detalle
    SELECT h.capacidad
      INTO v_capacidad
      FROM detalle_reserva d
      JOIN habitacion h ON h.id_habitacion = d.id_habitacion
     WHERE d.id_detalle = NEW.id_detalle
       FOR UPDATE OF d;

    SELECT count(*)
      INTO v_ocupantes
      FROM detalle_huesped
     WHERE id_detalle = NEW.id_detalle;

    IF v_ocupantes + 1 > v_capacidad THEN
        RAISE EXCEPTION
            'Capacidad excedida: la habitación admite % huésped(es)', v_capacidad
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_capacidad_detalle
    BEFORE INSERT ON detalle_huesped
    FOR EACH ROW EXECUTE FUNCTION fn_validar_capacidad_detalle();


-- 4.5 Coherencia entre cancelación y estado del detalle
--     (a) Al registrar una cancelación, el detalle pasa a 'cancelada'.
--         Solo se puede cancelar un detalle en estado 'confirmada'.
CREATE FUNCTION fn_cancelar_detalle()
RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    v_estado estado_detalle;
BEGIN
    SELECT estado
      INTO v_estado
      FROM detalle_reserva
     WHERE id_detalle = NEW.id_detalle
       FOR UPDATE;

    IF v_estado IS NOT NULL AND v_estado <> 'confirmada' THEN
        RAISE EXCEPTION
            'Solo se puede cancelar un detalle confirmado (estado actual: %)', v_estado
            USING ERRCODE = 'check_violation';
    END IF;

    UPDATE detalle_reserva
       SET estado = 'cancelada'
     WHERE id_detalle = NEW.id_detalle;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_cancelacion_insert
    BEFORE INSERT ON cancelacion
    FOR EACH ROW EXECUTE FUNCTION fn_cancelar_detalle();

--     (b) Un detalle cancelado no puede volver a otro estado.
CREATE FUNCTION fn_proteger_detalle_cancelado()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.estado = 'cancelada' AND NEW.estado <> 'cancelada' THEN
        RAISE EXCEPTION 'Un detalle cancelado no puede cambiar de estado'
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_detalle_cancelado_final
    BEFORE UPDATE OF estado ON detalle_reserva
    FOR EACH ROW EXECUTE FUNCTION fn_proteger_detalle_cancelado();

--     (c) Un detalle 'cancelada' debe tener su registro en cancelacion.
--         Se evalúa al final de la transacción (DEFERRED), de modo que la
--         cancelación pueda insertarse después de marcar el estado.
CREATE FUNCTION fn_verificar_cancelacion_detalle()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.estado = 'cancelada'
       AND NOT EXISTS (SELECT 1 FROM cancelacion c WHERE c.id_detalle = NEW.id_detalle)
    THEN
        RAISE EXCEPTION
            'El detalle % está cancelado pero no tiene registro de cancelación',
            NEW.id_detalle
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NULL;
END;
$$;

CREATE CONSTRAINT TRIGGER trg_detalle_cancelado_con_registro
    AFTER INSERT OR UPDATE OF estado ON detalle_reserva
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION fn_verificar_cancelacion_detalle();

--     (d) Las cancelaciones no se eliminan (conservan el historial).
CREATE FUNCTION fn_impedir_borrar_cancelacion()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Las cancelaciones no se pueden eliminar'
        USING ERRCODE = 'restrict_violation';
END;
$$;

CREATE TRIGGER trg_cancelacion_no_delete
    BEFORE DELETE ON cancelacion
    FOR EACH ROW EXECUTE FUNCTION fn_impedir_borrar_cancelacion();


-- ---------------------------------------------------------------------
-- 5. VISTAS
-- ---------------------------------------------------------------------

-- 5.1 Saldo por reserva
--   total_alojamiento   = noches x tarifa_noche de los detalles no cancelados
--   total_a_pagar       = total_alojamiento + penalidades por cancelación
--   cobrado_neto        = anticipos + saldos - reembolsos ya registrados
--   diferencia          = total_a_pagar - cobrado_neto
--   saldo_pendiente     = lo que el huésped aún debe (>= 0)
--   reembolso_pendiente = lo que el hotel aún debe devolver (>= 0)
--   Las noches se calculan con las fechas PREVISTAS (no con el check-in/out real).
CREATE VIEW v_saldo_reserva AS
WITH aloj AS (
    SELECT id_reserva,
           sum((fecha_salida_prevista - fecha_entrada_prevista) * tarifa_noche) AS total
      FROM detalle_reserva
     WHERE estado <> 'cancelada'
     GROUP BY id_reserva
), pen AS (
    SELECT d.id_reserva, sum(c.penalidad) AS total
      FROM cancelacion c
      JOIN detalle_reserva d ON d.id_detalle = c.id_detalle
     GROUP BY d.id_reserva
), cob AS (
    SELECT id_reserva,
           sum(CASE WHEN tipo = 'reembolso' THEN -monto ELSE monto END) AS neto
      FROM pago
     GROUP BY id_reserva
), base AS (
    SELECT r.id_reserva,
           COALESCE(a.total, 0) AS total_alojamiento,
           COALESCE(p.total, 0) AS total_penalidades,
           COALESCE(c.neto,  0) AS cobrado_neto
      FROM reserva r
      LEFT JOIN aloj a ON a.id_reserva = r.id_reserva
      LEFT JOIN pen  p ON p.id_reserva = r.id_reserva
      LEFT JOIN cob  c ON c.id_reserva = r.id_reserva
)
SELECT id_reserva,
       total_alojamiento,
       total_penalidades,
       total_alojamiento + total_penalidades                      AS total_a_pagar,
       cobrado_neto,
       GREATEST(total_alojamiento + total_penalidades - cobrado_neto, 0) AS saldo_pendiente,
       GREATEST(cobrado_neto - (total_alojamiento + total_penalidades), 0) AS reembolso_pendiente
  FROM base;

-- 5.2 Noches ocupadas por mes (base para reportes de ocupación por temporada)
--     Cuenta cada noche de los detalles no cancelados, según fechas previstas.
CREATE VIEW v_noches_ocupadas_por_mes AS
SELECT date_trunc('month', n.noche)::date AS mes,
       count(*)                           AS noches_ocupadas
  FROM detalle_reserva d
  CROSS JOIN LATERAL generate_series(
        d.fecha_entrada_prevista::timestamp,
        (d.fecha_salida_prevista - 1)::timestamp,
        interval '1 day') AS n(noche)
 WHERE d.estado <> 'cancelada'
 GROUP BY 1
 ORDER BY 1;


-- ---------------------------------------------------------------------
-- 6. DATOS INICIALES
-- ---------------------------------------------------------------------
INSERT INTO rol (nombre) VALUES ('administrador'), ('recepcionista')
    ON CONFLICT (nombre) DO NOTHING;

INSERT INTO canal (nombre) VALUES ('presencial'), ('telefono'), ('whatsapp')
    ON CONFLICT (nombre) DO NOTHING;

COMMIT;


-- ---------------------------------------------------------------------
-- 7. SCRIPT DE REINICIO (comentado; útil para volver a empezar en pruebas)
-- ---------------------------------------------------------------------
-- BEGIN;
-- DROP VIEW  IF EXISTS v_noches_ocupadas_por_mes, v_saldo_reserva;
-- DROP TABLE IF EXISTS pago, cancelacion, detalle_huesped, detalle_reserva,
--                      reserva, huesped, bloqueo_habitacion, habitacion,
--                      canal, usuario, rol CASCADE;
-- DROP FUNCTION IF EXISTS fn_actualizar_fecha_modificacion(),
--                         fn_validar_detalle_vs_bloqueo(),
--                         fn_validar_bloqueo_vs_detalle(),
--                         fn_validar_capacidad_detalle(),
--                         fn_cancelar_detalle(),
--                         fn_proteger_detalle_cancelado(),
--                         fn_verificar_cancelacion_detalle(),
--                         fn_impedir_borrar_cancelacion();
-- DROP TYPE  IF EXISTS tipo_habitacion, estado_habitacion, estado_detalle,
--                      tipo_movimiento_pago, metodo_pago;
-- COMMIT;
