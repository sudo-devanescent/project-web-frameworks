-- =====================================================================
-- HostOps - Migración manual 001
-- Auditoría de pagos y cancelaciones
-- =====================================================================
-- Aplicar sobre una base ya poblada, con el esquema y el seed cargados:
--   podman exec -i hostops-db psql -U hostops -d hostops \
--       -v ON_ERROR_STOP=1 < db/hostops_migration_001_auditoria_pagos.sql
--
-- Efectos sobre datos existentes:
--   * pago.id_usuario y cancelacion.id_usuario se rellenan con el usuario
--     creador/modificador de la reserva. No se pierde ninguna fila.
--   * pago.id_pago_original queda NULL para los movimientos históricos.
--   * pago.motivo queda NULL para los movimientos históricos.
-- El script es idempotente: se puede ejecutar más de una vez.
-- =====================================================================

BEGIN;

-- 1. Columnas nuevas -------------------------------------------------
ALTER TABLE pago
    ADD COLUMN IF NOT EXISTS id_usuario INTEGER,
    ADD COLUMN IF NOT EXISTS id_pago_original INTEGER,
    ADD COLUMN IF NOT EXISTS motivo TEXT;

ALTER TABLE cancelacion
    ADD COLUMN IF NOT EXISTS id_usuario INTEGER;

-- 2. Backfill sin pérdida de datos ----------------------------------
UPDATE pago p
   SET id_usuario = r.id_usuario_creador
  FROM reserva r
 WHERE r.id_reserva = p.id_reserva
   AND p.id_usuario IS NULL;

UPDATE cancelacion c
   SET id_usuario = COALESCE(r.id_usuario_modificador, r.id_usuario_creador)
  FROM detalle_reserva d
  JOIN reserva r ON r.id_reserva = d.id_reserva
 WHERE d.id_detalle = c.id_detalle
   AND c.id_usuario IS NULL;

-- 3. Restricciones ---------------------------------------------------
ALTER TABLE pago
    ALTER COLUMN id_usuario SET NOT NULL;

ALTER TABLE cancelacion
    ALTER COLUMN id_usuario SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_pago_usuario') THEN
        ALTER TABLE pago
            ADD CONSTRAINT fk_pago_usuario FOREIGN KEY (id_usuario)
            REFERENCES usuario (id_usuario) ON DELETE RESTRICT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_pago_original') THEN
        ALTER TABLE pago
            ADD CONSTRAINT fk_pago_original FOREIGN KEY (id_pago_original)
            REFERENCES pago (id_pago) ON DELETE RESTRICT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_pago_no_autocompensacion') THEN
        ALTER TABLE pago
            ADD CONSTRAINT ck_pago_no_autocompensacion
            CHECK (id_pago_original IS NULL OR id_pago_original <> id_pago);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_cancelacion_usuario') THEN
        ALTER TABLE cancelacion
            ADD CONSTRAINT fk_cancelacion_usuario FOREIGN KEY (id_usuario)
            REFERENCES usuario (id_usuario) ON DELETE RESTRICT;
    END IF;
END
$$;

-- 4. Índices ---------------------------------------------------------
CREATE INDEX IF NOT EXISTS ix_pago_usuario ON pago (id_usuario);
CREATE INDEX IF NOT EXISTS ix_cancelacion_usuario ON cancelacion (id_usuario);
CREATE UNIQUE INDEX IF NOT EXISTS uq_pago_compensacion
    ON pago (id_pago_original) WHERE id_pago_original IS NOT NULL;

COMMIT;
