-- =====================================================================
--  SISTEMA CAMPO DEPORTIVO + BODEGA
--  PostgreSQL 14+
--  Tres esquemas: core (usuarios), cancha (alquileres), bodega (bebidas)
--  Cancha y bodega NO tienen claves foráneas entre sí: dinero separado.
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS btree_gist;   -- para exclusion constraints

CREATE SCHEMA IF NOT EXISTS core;
CREATE SCHEMA IF NOT EXISTS cancha;
CREATE SCHEMA IF NOT EXISTS bodega;

-- Rango de horas (time) para validar que las tarifas no se crucen
CREATE TYPE core.timerange AS RANGE (subtype = time);

-- Actualiza updated_at automáticamente
CREATE OR REPLACE FUNCTION core.fn_set_updated_at()
    RETURNS trigger AS $$
BEGIN
    NEW.updated_at := now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


-- =====================================================================
--  CORE: usuarios del sistema
-- =====================================================================
CREATE TABLE core.usuario (
                              id             BIGSERIAL PRIMARY KEY,
                              nombre         VARCHAR(100) NOT NULL,
                              username       VARCHAR(50)  NOT NULL UNIQUE,
                              password_hash  VARCHAR(255) NOT NULL,                 -- BCrypt desde Spring Security
                              rol            VARCHAR(20)  NOT NULL DEFAULT 'ADMIN'
                                  CHECK (rol IN ('ADMIN', 'ENCARGADO')),
                              activo         BOOLEAN      NOT NULL DEFAULT TRUE,
                              created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
                              updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE TRIGGER trg_usuario_upd BEFORE UPDATE ON core.usuario
    FOR EACH ROW EXECUTE FUNCTION core.fn_set_updated_at();


-- =====================================================================
--  MÓDULO CANCHA
-- =====================================================================

CREATE TABLE cancha.cancha (
                               id           BIGSERIAL PRIMARY KEY,
                               nombre       VARCHAR(80) NOT NULL UNIQUE,
                               descripcion  VARCHAR(255),
                               activa       BOOLEAN     NOT NULL DEFAULT TRUE,
                               created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Tarifas por franja horaria.
-- dia_semana: 1=lunes ... 7=domingo (ISO). NULL = aplica todos los días.
-- Una tarifa de un día específico tiene prioridad sobre la general (se resuelve en Java).
CREATE TABLE cancha.tarifa (
                               id           BIGSERIAL PRIMARY KEY,
                               cancha_id    BIGINT        NOT NULL REFERENCES cancha.cancha(id),
                               dia_semana   SMALLINT      CHECK (dia_semana BETWEEN 1 AND 7),
                               hora_inicio  TIME          NOT NULL,
                               hora_fin     TIME          NOT NULL,
                               precio_hora  NUMERIC(10,2) NOT NULL CHECK (precio_hora > 0),
                               activa       BOOLEAN       NOT NULL DEFAULT TRUE,
                               created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
                               CONSTRAINT chk_tarifa_horas CHECK (hora_fin > hora_inicio),
    -- Dos franjas activas del mismo día y cancha no pueden cruzarse
                               CONSTRAINT ex_tarifa_sin_cruce EXCLUDE USING gist (
                                   cancha_id WITH =,
                                   COALESCE(dia_semana, 0) WITH =,
                                   core.timerange(hora_inicio, hora_fin) WITH &&
                                   ) WHERE (activa)
);

CREATE TABLE cancha.cliente (
                                id          BIGSERIAL PRIMARY KEY,
                                nombre      VARCHAR(100) NOT NULL,
                                telefono    VARCHAR(15)  NOT NULL UNIQUE,              -- su WhatsApp
                                notas       VARCHAR(255),                              -- "equipo Los Halcones", etc.
                                created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
                                updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE TRIGGER trg_cliente_upd BEFORE UPDATE ON cancha.cliente
    FOR EACH ROW EXECUTE FUNCTION core.fn_set_updated_at();

-- Reservas Y bloqueos de mantenimiento en la misma tabla:
-- así la misma regla impide cruces entre ambos.
CREATE TABLE cancha.reserva (
                                id                     BIGSERIAL PRIMARY KEY,
                                cancha_id              BIGINT        NOT NULL REFERENCES cancha.cancha(id),
                                cliente_id             BIGINT        REFERENCES cancha.cliente(id),
                                tipo                   VARCHAR(15)   NOT NULL DEFAULT 'RESERVA'
                                    CHECK (tipo IN ('RESERVA', 'MANTENIMIENTO')),
                                estado                 VARCHAR(15)   NOT NULL DEFAULT 'PENDIENTE'
                                    CHECK (estado IN ('PENDIENTE',   -- sin adelanto aún
                                                      'CONFIRMADA',  -- con adelanto o confirmada
                                                      'COMPLETADA',  -- jugó y pagó todo
                                                      'CANCELADA',
                                                      'NO_ASISTIO')),
                                inicio                 TIMESTAMPTZ   NOT NULL,
                                fin                    TIMESTAMPTZ   NOT NULL,
                                monto_total            NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (monto_total >= 0),
                                fecha_limite_adelanto  TIMESTAMPTZ,                    -- si vence sin pago → CANCELADA
                                origen                 VARCHAR(15)   NOT NULL DEFAULT 'PRESENCIAL'
                                    CHECK (origen IN ('PRESENCIAL', 'WHATSAPP', 'LLAMADA', 'WEB')),
                                observacion            VARCHAR(255),
                                motivo_cancelacion     VARCHAR(255),
                                creado_por             BIGINT        REFERENCES core.usuario(id),
                                created_at             TIMESTAMPTZ   NOT NULL DEFAULT now(),
                                updated_at             TIMESTAMPTZ   NOT NULL DEFAULT now(),

                                CONSTRAINT chk_reserva_horas CHECK (fin > inicio),
    -- Una reserva necesita cliente y precio; un mantenimiento no lleva ni cliente ni cobro
                                CONSTRAINT chk_reserva_tipo CHECK (
                                    (tipo = 'RESERVA'       AND cliente_id IS NOT NULL AND monto_total > 0)
                                        OR (tipo = 'MANTENIMIENTO' AND cliente_id IS NULL     AND monto_total = 0)
                                    ),
    -- LA REGLA CLAVE: nunca dos ocupaciones cruzadas en la misma cancha
                                CONSTRAINT ex_reserva_sin_cruce EXCLUDE USING gist (
                                    cancha_id WITH =,
                                    tstzrange(inicio, fin, '[)') WITH &&
                                    ) WHERE (estado NOT IN ('CANCELADA'))
);
CREATE INDEX idx_reserva_inicio  ON cancha.reserva (inicio);
CREATE INDEX idx_reserva_cliente ON cancha.reserva (cliente_id);
CREATE INDEX idx_reserva_estado  ON cancha.reserva (estado) WHERE estado = 'PENDIENTE';
CREATE TRIGGER trg_reserva_upd BEFORE UPDATE ON cancha.reserva
    FOR EACH ROW EXECUTE FUNCTION core.fn_set_updated_at();

-- Caja exclusiva de la cancha
CREATE TABLE cancha.caja (
                             id                     BIGSERIAL PRIMARY KEY,
                             fecha                  DATE          NOT NULL DEFAULT CURRENT_DATE,
                             monto_apertura         NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (monto_apertura >= 0),
                             monto_cierre_contado   NUMERIC(10,2) CHECK (monto_cierre_contado >= 0), -- efectivo contado al cerrar
                             estado                 VARCHAR(10)   NOT NULL DEFAULT 'ABIERTA'
                                 CHECK (estado IN ('ABIERTA', 'CERRADA')),
                             observacion            VARCHAR(255),
                             abierta_por            BIGINT        NOT NULL REFERENCES core.usuario(id),
                             cerrada_por            BIGINT        REFERENCES core.usuario(id),
                             abierta_at             TIMESTAMPTZ   NOT NULL DEFAULT now(),
                             cerrada_at             TIMESTAMPTZ,
                             CONSTRAINT chk_caja_cierre CHECK (
                                 (estado = 'ABIERTA' AND cerrada_at IS NULL)
                                     OR (estado = 'CERRADA' AND cerrada_at IS NOT NULL AND monto_cierre_contado IS NOT NULL)
                                 )
);
-- Solo puede haber UNA caja abierta a la vez
CREATE UNIQUE INDEX uq_cancha_caja_abierta ON cancha.caja (estado) WHERE estado = 'ABIERTA';

CREATE TABLE cancha.pago_reserva (
                                     id              BIGSERIAL PRIMARY KEY,
                                     reserva_id      BIGINT        NOT NULL REFERENCES cancha.reserva(id),
                                     caja_id         BIGINT        NOT NULL REFERENCES cancha.caja(id),
                                     tipo            VARCHAR(10)   NOT NULL CHECK (tipo IN ('ADELANTO', 'SALDO', 'DEVOLUCION')),
                                     metodo          VARCHAR(10)   NOT NULL CHECK (metodo IN ('EFECTIVO', 'YAPE', 'PLIN', 'TRANSFERENCIA')),
                                     monto           NUMERIC(10,2) NOT NULL CHECK (monto > 0),   -- DEVOLUCION se resta en las vistas
                                     nro_operacion   VARCHAR(30),                               -- código Yape/Plin
                                     voucher_url     VARCHAR(500),                              -- captura del pago (opcional)
                                     registrado_por  BIGINT        NOT NULL REFERENCES core.usuario(id),
                                     fecha           TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX idx_pago_reserva_reserva ON cancha.pago_reserva (reserva_id);
CREATE INDEX idx_pago_reserva_caja    ON cancha.pago_reserva (caja_id);

-- Ingresos/egresos manuales de la caja de cancha (pago de luz, compra de pelota, etc.)
CREATE TABLE cancha.movimiento_caja (
                                        id              BIGSERIAL PRIMARY KEY,
                                        caja_id         BIGINT        NOT NULL REFERENCES cancha.caja(id),
                                        tipo            VARCHAR(10)   NOT NULL CHECK (tipo IN ('INGRESO', 'EGRESO')),
                                        metodo          VARCHAR(10)   NOT NULL CHECK (metodo IN ('EFECTIVO', 'YAPE', 'PLIN', 'TRANSFERENCIA')),
                                        concepto        VARCHAR(150)  NOT NULL,
                                        monto           NUMERIC(10,2) NOT NULL CHECK (monto > 0),
                                        registrado_por  BIGINT        NOT NULL REFERENCES core.usuario(id),
                                        fecha           TIMESTAMPTZ   NOT NULL DEFAULT now()
);


-- =====================================================================
--  MÓDULO BODEGA
-- =====================================================================

CREATE TABLE bodega.categoria (
                                  id      BIGSERIAL PRIMARY KEY,
                                  nombre  VARCHAR(60) NOT NULL UNIQUE                     -- Cerveza, Gaseosa, Agua, Envases...
);

CREATE TABLE bodega.producto (
                                 id                 BIGSERIAL PRIMARY KEY,
                                 codigo_barras      VARCHAR(30) UNIQUE,                  -- para escaneo con cámara
                                 nombre             VARCHAR(120)  NOT NULL,
                                 categoria_id       BIGINT        NOT NULL REFERENCES bodega.categoria(id),
                                 tipo               VARCHAR(10)   NOT NULL DEFAULT 'PRODUCTO'
                                     CHECK (tipo IN ('PRODUCTO', 'ENVASE')),
                                 precio_compra      NUMERIC(10,2) NOT NULL CHECK (precio_compra >= 0),  -- costo por UNIDAD
                                 precio_venta       NUMERIC(10,2) NOT NULL CHECK (precio_venta  >= 0),
                                 stock              INTEGER       NOT NULL DEFAULT 0 CHECK (stock >= 0),
                                 stock_minimo       INTEGER       NOT NULL DEFAULT 0 CHECK (stock_minimo >= 0),
                                 unidades_por_caja  INTEGER       NOT NULL DEFAULT 1 CHECK (unidades_por_caja >= 1),
                                 envase_id          BIGINT        REFERENCES bodega.producto(id),  -- envase retornable asociado
                                 activo             BOOLEAN       NOT NULL DEFAULT TRUE,
                                 created_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
                                 updated_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
                                 CONSTRAINT chk_envase_no_recursivo CHECK (envase_id IS NULL OR envase_id <> id)
);
CREATE INDEX idx_producto_categoria ON bodega.producto (categoria_id);
CREATE TRIGGER trg_producto_upd BEFORE UPDATE ON bodega.producto
    FOR EACH ROW EXECUTE FUNCTION core.fn_set_updated_at();

CREATE TABLE bodega.proveedor (
                                  id          BIGSERIAL PRIMARY KEY,
                                  nombre      VARCHAR(120) NOT NULL,
                                  ruc         VARCHAR(11)  UNIQUE CHECK (ruc ~ '^[0-9]{11}$'),
                                  telefono    VARCHAR(15),
                                  activo      BOOLEAN      NOT NULL DEFAULT TRUE,
                                  created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE bodega.compra (
                               id                BIGSERIAL PRIMARY KEY,
                               proveedor_id      BIGINT        REFERENCES bodega.proveedor(id),
                               nro_comprobante   VARCHAR(30),
                               fecha             TIMESTAMPTZ   NOT NULL DEFAULT now(),
                               total             NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (total >= 0),
                               estado            VARCHAR(10)   NOT NULL DEFAULT 'REGISTRADA'
                                   CHECK (estado IN ('REGISTRADA', 'ANULADA')),
                               registrado_por    BIGINT        NOT NULL REFERENCES core.usuario(id),
                               created_at        TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- La compra se puede registrar por cajas; cantidad_unidades es lo que suma al stock
CREATE TABLE bodega.detalle_compra (
                                       id                 BIGSERIAL PRIMARY KEY,
                                       compra_id          BIGINT        NOT NULL REFERENCES bodega.compra(id) ON DELETE CASCADE,
                                       producto_id        BIGINT        NOT NULL REFERENCES bodega.producto(id),
                                       cantidad_cajas     INTEGER       NOT NULL DEFAULT 0 CHECK (cantidad_cajas >= 0),
                                       cantidad_unidades  INTEGER       NOT NULL CHECK (cantidad_unidades > 0),
                                       costo_unitario     NUMERIC(10,2) NOT NULL CHECK (costo_unitario >= 0),
                                       subtotal           NUMERIC(12,2) GENERATED ALWAYS AS (cantidad_unidades * costo_unitario) STORED
);
CREATE INDEX idx_det_compra_compra ON bodega.detalle_compra (compra_id);

-- Caja exclusiva de la bodega
CREATE TABLE bodega.caja (
                             id                     BIGSERIAL PRIMARY KEY,
                             fecha                  DATE          NOT NULL DEFAULT CURRENT_DATE,
                             monto_apertura         NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (monto_apertura >= 0),
                             monto_cierre_contado   NUMERIC(10,2) CHECK (monto_cierre_contado >= 0),
                             estado                 VARCHAR(10)   NOT NULL DEFAULT 'ABIERTA'
                                 CHECK (estado IN ('ABIERTA', 'CERRADA')),
                             observacion            VARCHAR(255),
                             abierta_por            BIGINT        NOT NULL REFERENCES core.usuario(id),
                             cerrada_por            BIGINT        REFERENCES core.usuario(id),
                             abierta_at             TIMESTAMPTZ   NOT NULL DEFAULT now(),
                             cerrada_at             TIMESTAMPTZ,
                             CONSTRAINT chk_caja_cierre CHECK (
                                 (estado = 'ABIERTA' AND cerrada_at IS NULL)
                                     OR (estado = 'CERRADA' AND cerrada_at IS NOT NULL AND monto_cierre_contado IS NOT NULL)
                                 )
);
CREATE UNIQUE INDEX uq_bodega_caja_abierta ON bodega.caja (estado) WHERE estado = 'ABIERTA';

CREATE TABLE bodega.venta (
                              id              BIGSERIAL PRIMARY KEY,
                              caja_id         BIGINT        NOT NULL REFERENCES bodega.caja(id),
                              fecha           TIMESTAMPTZ   NOT NULL DEFAULT now(),
                              total           NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (total >= 0),
                              estado          VARCHAR(10)   NOT NULL DEFAULT 'REGISTRADA'
                                  CHECK (estado IN ('REGISTRADA', 'ANULADA')),
                              motivo_anulacion VARCHAR(255),
                              registrado_por  BIGINT        NOT NULL REFERENCES core.usuario(id)
);
CREATE INDEX idx_venta_fecha ON bodega.venta (fecha);
CREATE INDEX idx_venta_caja  ON bodega.venta (caja_id);

-- costo_unitario se copia del producto al vender: así la ganancia histórica
-- no cambia aunque luego suba el precio de compra
CREATE TABLE bodega.detalle_venta (
                                      id               BIGSERIAL PRIMARY KEY,
                                      venta_id         BIGINT        NOT NULL REFERENCES bodega.venta(id) ON DELETE CASCADE,
                                      producto_id      BIGINT        NOT NULL REFERENCES bodega.producto(id),
                                      cantidad         INTEGER       NOT NULL CHECK (cantidad > 0),
                                      precio_unitario  NUMERIC(10,2) NOT NULL CHECK (precio_unitario >= 0),
                                      costo_unitario   NUMERIC(10,2) NOT NULL CHECK (costo_unitario  >= 0),
                                      subtotal         NUMERIC(12,2) GENERATED ALWAYS AS (cantidad * precio_unitario) STORED
);
CREATE INDEX idx_det_venta_venta    ON bodega.detalle_venta (venta_id);
CREATE INDEX idx_det_venta_producto ON bodega.detalle_venta (producto_id);

-- Una venta puede pagarse con varios métodos (parte efectivo, parte Yape)
CREATE TABLE bodega.pago_venta (
                                   id             BIGSERIAL PRIMARY KEY,
                                   venta_id       BIGINT        NOT NULL REFERENCES bodega.venta(id) ON DELETE CASCADE,
                                   metodo         VARCHAR(10)   NOT NULL CHECK (metodo IN ('EFECTIVO', 'YAPE', 'PLIN', 'TRANSFERENCIA')),
                                   monto          NUMERIC(10,2) NOT NULL CHECK (monto > 0),
                                   nro_operacion  VARCHAR(30),
                                   fecha          TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX idx_pago_venta_venta ON bodega.pago_venta (venta_id);

CREATE TABLE bodega.movimiento_caja (
                                        id              BIGSERIAL PRIMARY KEY,
                                        caja_id         BIGINT        NOT NULL REFERENCES bodega.caja(id),
                                        tipo            VARCHAR(10)   NOT NULL CHECK (tipo IN ('INGRESO', 'EGRESO')),
                                        metodo          VARCHAR(10)   NOT NULL CHECK (metodo IN ('EFECTIVO', 'YAPE', 'PLIN', 'TRANSFERENCIA')),
                                        concepto        VARCHAR(150)  NOT NULL,
                                        monto           NUMERIC(10,2) NOT NULL CHECK (monto > 0),
                                        registrado_por  BIGINT        NOT NULL REFERENCES core.usuario(id),
                                        fecha           TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- Kardex: historial de TODO lo que mueve el stock
CREATE TABLE bodega.movimiento_inventario (
                                              id                BIGSERIAL PRIMARY KEY,
                                              producto_id       BIGINT       NOT NULL REFERENCES bodega.producto(id),
                                              tipo              VARCHAR(25)  NOT NULL CHECK (tipo IN (
                                                                                                      'ENTRADA_COMPRA', 'SALIDA_VENTA',
                                                                                                      'ANULACION_VENTA', 'ANULACION_COMPRA',
                                                                                                      'AJUSTE_POSITIVO', 'AJUSTE_NEGATIVO',
                                                                                                      'MERMA',                         -- botella rota, producto vencido
                                                                                                      'DEVOLUCION_ENVASE',             -- cliente/proveedor devuelve envases
                                                                                                      'SALIDA_ENVASE')),               -- envases entregados al proveedor
                                              cantidad          INTEGER      NOT NULL CHECK (cantidad > 0),
                                              stock_anterior    INTEGER      NOT NULL,
                                              stock_resultante  INTEGER      NOT NULL CHECK (stock_resultante >= 0),
                                              compra_id         BIGINT       REFERENCES bodega.compra(id),
                                              venta_id          BIGINT       REFERENCES bodega.venta(id),
                                              motivo            VARCHAR(255),
                                              registrado_por    BIGINT       NOT NULL REFERENCES core.usuario(id),
                                              fecha             TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_mov_inv_producto ON bodega.movimiento_inventario (producto_id, fecha);


-- =====================================================================
--  VISTAS DE APOYO (reportes y validaciones rápidas)
-- =====================================================================

-- Cuánto se ha pagado y cuánto falta en cada reserva
CREATE VIEW cancha.v_reserva_saldo AS
SELECT r.id AS reserva_id, r.cancha_id, r.cliente_id, r.inicio, r.fin, r.estado,
       r.monto_total,
       COALESCE(SUM(CASE WHEN p.tipo = 'DEVOLUCION' THEN -p.monto ELSE p.monto END), 0) AS pagado,
       r.monto_total
           - COALESCE(SUM(CASE WHEN p.tipo = 'DEVOLUCION' THEN -p.monto ELSE p.monto END), 0) AS saldo
FROM cancha.reserva r
         LEFT JOIN cancha.pago_reserva p ON p.reserva_id = r.id
WHERE r.tipo = 'RESERVA'
GROUP BY r.id;

-- Efectivo que DEBERÍA haber en la caja de cancha (para comparar al cerrar)
CREATE VIEW cancha.v_caja_resumen AS
SELECT c.id AS caja_id, c.fecha, c.estado, c.monto_apertura,
       COALESCE(p.efectivo, 0)  AS cobros_efectivo,
       COALESCE(p.digital, 0)   AS cobros_digitales,
       COALESCE(m.ingresos, 0)  AS otros_ingresos_efectivo,
       COALESCE(m.egresos, 0)   AS egresos_efectivo,
       c.monto_apertura + COALESCE(p.efectivo, 0) + COALESCE(m.ingresos, 0)
           - COALESCE(m.egresos, 0)                              AS efectivo_esperado,
       c.monto_cierre_contado,
       c.monto_cierre_contado - (c.monto_apertura + COALESCE(p.efectivo, 0)
                                     + COALESCE(m.ingresos, 0) - COALESCE(m.egresos, 0))   AS diferencia
FROM cancha.caja c
         LEFT JOIN (
    SELECT caja_id,
           SUM(CASE WHEN metodo = 'EFECTIVO'
                        THEN CASE WHEN tipo = 'DEVOLUCION' THEN -monto ELSE monto END END) AS efectivo,
           SUM(CASE WHEN metodo <> 'EFECTIVO'
                        THEN CASE WHEN tipo = 'DEVOLUCION' THEN -monto ELSE monto END END) AS digital
    FROM cancha.pago_reserva GROUP BY caja_id
) p ON p.caja_id = c.id
         LEFT JOIN (
    SELECT caja_id,
           SUM(CASE WHEN tipo = 'INGRESO' AND metodo = 'EFECTIVO' THEN monto END) AS ingresos,
           SUM(CASE WHEN tipo = 'EGRESO'  AND metodo = 'EFECTIVO' THEN monto END) AS egresos
    FROM cancha.movimiento_caja GROUP BY caja_id
) m ON m.caja_id = c.id;

-- Lo mismo para la bodega (solo ventas no anuladas)
CREATE VIEW bodega.v_caja_resumen AS
SELECT c.id AS caja_id, c.fecha, c.estado, c.monto_apertura,
       COALESCE(p.efectivo, 0)  AS ventas_efectivo,
       COALESCE(p.digital, 0)   AS ventas_digitales,
       COALESCE(m.ingresos, 0)  AS otros_ingresos_efectivo,
       COALESCE(m.egresos, 0)   AS egresos_efectivo,
       c.monto_apertura + COALESCE(p.efectivo, 0) + COALESCE(m.ingresos, 0)
           - COALESCE(m.egresos, 0)                              AS efectivo_esperado,
       c.monto_cierre_contado,
       c.monto_cierre_contado - (c.monto_apertura + COALESCE(p.efectivo, 0)
                                     + COALESCE(m.ingresos, 0) - COALESCE(m.egresos, 0))   AS diferencia
FROM bodega.caja c
         LEFT JOIN (
    SELECT v.caja_id,
           SUM(CASE WHEN pv.metodo =  'EFECTIVO' THEN pv.monto END) AS efectivo,
           SUM(CASE WHEN pv.metodo <> 'EFECTIVO' THEN pv.monto END) AS digital
    FROM bodega.pago_venta pv
             JOIN bodega.venta v ON v.id = pv.venta_id AND v.estado = 'REGISTRADA'
    GROUP BY v.caja_id
) p ON p.caja_id = c.id
         LEFT JOIN (
    SELECT caja_id,
           SUM(CASE WHEN tipo = 'INGRESO' AND metodo = 'EFECTIVO' THEN monto END) AS ingresos,
           SUM(CASE WHEN tipo = 'EGRESO'  AND metodo = 'EFECTIVO' THEN monto END) AS egresos
    FROM bodega.movimiento_caja GROUP BY caja_id
) m ON m.caja_id = c.id;

-- Ganancia por producto (ventas no anuladas)
CREATE VIEW bodega.v_ganancia_producto AS
SELECT p.id AS producto_id, p.nombre,
       SUM(d.cantidad)                                    AS unidades_vendidas,
       SUM(d.subtotal)                                    AS total_vendido,
       SUM(d.cantidad * d.costo_unitario)                 AS costo_total,
       SUM(d.subtotal) - SUM(d.cantidad * d.costo_unitario) AS ganancia
FROM bodega.detalle_venta d
         JOIN bodega.venta v    ON v.id = d.venta_id AND v.estado = 'REGISTRADA'
         JOIN bodega.producto p ON p.id = d.producto_id
GROUP BY p.id, p.nombre;

-- Productos que hay que volver a pedir
CREATE VIEW bodega.v_stock_bajo AS
SELECT id, nombre, stock, stock_minimo
FROM bodega.producto
WHERE activo AND stock <= stock_minimo;
