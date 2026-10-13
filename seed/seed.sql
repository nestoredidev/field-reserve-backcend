-- =====================================================================
--  SEED: datos de prueba (TODOS FALSOS) para desarrollo
--  Requisito: haber ejecutado antes bd.sql. Ejecutar UNA sola vez.
--
--  Usuarios (contraseña entre paréntesis):
--    admin      (Admin12345)    ADMIN
--    encargado  (Encargado123)  ENCARGADO
--
--  Las fechas son relativas a hoy (hora de Lima), así siempre hay
--  reservas "de ayer", "de mañana", etc.
-- =====================================================================
CREATE EXTENSION IF NOT EXISTS pgcrypto;   -- crypt()/gen_salt('bf') generan BCrypt compatible con Spring

-- hora local de Lima, `dias` días desde hoy, a las `hora` (admite 20.5 = 20:30)
CREATE OR REPLACE FUNCTION pg_temp.lima(dias int, hora numeric) RETURNS timestamptz AS $$
    SELECT ((date_trunc('day', now() AT TIME ZONE 'America/Lima') + make_interval(days => dias)
             + make_interval(secs => (hora * 3600)::int)) AT TIME ZONE 'America/Lima');
$$ LANGUAGE sql;

DO $seed$
DECLARE
    v_admin  bigint;
    v_enc    bigint;
    v_c1     bigint := (SELECT id FROM cancha.cancha WHERE nombre = 'Cancha 1');
    v_c2     bigint;
    cl1 bigint; cl2 bigint; cl3 bigint; cl4 bigint;
    caja_ayer bigint; caja_hoy bigint;
    r bigint;
    -- bodega
    cat_cerveza bigint := (SELECT id FROM bodega.categoria WHERE nombre = 'Cerveza');
    cat_gaseosa bigint := (SELECT id FROM bodega.categoria WHERE nombre = 'Gaseosa');
    cat_agua    bigint := (SELECT id FROM bodega.categoria WHERE nombre = 'Agua');
    cat_envase  bigint := (SELECT id FROM bodega.categoria WHERE nombre = 'Envases');
    p_env bigint; p_pil bigint; p_cus bigint; p_inca bigint; p_coca bigint; p_agua bigint;
    prov bigint; compra bigint; venta bigint;
    bcaja bigint;
BEGIN
    -- ---------------------------------------------------------------- usuarios
    UPDATE core.usuario SET password_hash = crypt('Admin12345', gen_salt('bf', 10))
     WHERE username = 'admin' RETURNING id INTO v_admin;
    IF v_admin IS NULL THEN
        INSERT INTO core.usuario (nombre, username, password_hash, rol)
        VALUES ('Dueño', 'admin', crypt('Admin12345', gen_salt('bf', 10)), 'ADMIN') RETURNING id INTO v_admin;
    END IF;
    INSERT INTO core.usuario (nombre, username, password_hash, rol)
    VALUES ('María Encargada', 'encargado', crypt('Encargado123', gen_salt('bf', 10)), 'ENCARGADO')
    ON CONFLICT (username) DO UPDATE SET password_hash = EXCLUDED.password_hash
    RETURNING id INTO v_enc;

    -- ---------------------------------------------------------------- cancha: catálogo
    INSERT INTO cancha.cancha (nombre, descripcion) VALUES ('Cancha 2', 'Losa deportiva techada')
    ON CONFLICT (nombre) DO NOTHING;
    SELECT id INTO v_c2 FROM cancha.cancha WHERE nombre = 'Cancha 2';
    IF NOT EXISTS (SELECT 1 FROM cancha.tarifa WHERE cancha_id = v_c2) THEN
        INSERT INTO cancha.tarifa (cancha_id, dia_semana, hora_inicio, hora_fin, precio_hora) VALUES
            (v_c2, NULL, '09:00', '18:00', 25.00),
            (v_c2, NULL, '18:00', '23:00', 50.00);
    END IF;

    INSERT INTO cancha.cliente (nombre, telefono, notas) VALUES
        ('Carlos Mendoza',  '987111222', 'Equipo Los Halcones'),
        ('Lucía Fernández', '987333444', 'Juega los viernes'),
        ('Pedro Quispe',    '987555666', NULL),
        ('Ana Torres',      '987777888', 'Pide factura')
    ON CONFLICT (telefono) DO NOTHING;
    SELECT id INTO cl1 FROM cancha.cliente WHERE telefono = '987111222';
    SELECT id INTO cl2 FROM cancha.cliente WHERE telefono = '987333444';
    SELECT id INTO cl3 FROM cancha.cliente WHERE telefono = '987555666';
    SELECT id INTO cl4 FROM cancha.cliente WHERE telefono = '987777888';

    -- ---------------------------------------------------------------- cancha: cajas
    INSERT INTO cancha.caja (fecha, monto_apertura, monto_cierre_contado, estado, observacion,
                             abierta_por, cerrada_por, abierta_at, cerrada_at)
    VALUES ((now() AT TIME ZONE 'America/Lima')::date - 1, 50, 170, 'CERRADA', NULL,
            v_enc, v_enc, pg_temp.lima(-1, 8), pg_temp.lima(-1, 23))
    RETURNING id INTO caja_ayer;
    INSERT INTO cancha.caja (fecha, monto_apertura, estado, abierta_por, abierta_at)
    VALUES ((now() AT TIME ZONE 'America/Lima')::date, 50, 'ABIERTA', v_enc, pg_temp.lima(0, 7))
    RETURNING id INTO caja_hoy;

    -- ---------------------------------------------------------------- reservas de ayer (cerradas)
    -- COMPLETADA: 18:00-20:00 = 60 + 60
    INSERT INTO cancha.reserva (cancha_id, cliente_id, estado, inicio, fin, monto_total, origen, creado_por)
    VALUES (v_c1, cl1, 'COMPLETADA', pg_temp.lima(-1, 18), pg_temp.lima(-1, 20), 120, 'WHATSAPP', v_enc)
    RETURNING id INTO r;
    INSERT INTO cancha.pago_reserva (reserva_id, caja_id, tipo, metodo, monto, registrado_por, fecha) VALUES
        (r, caja_ayer, 'ADELANTO', 'EFECTIVO', 60, v_enc, pg_temp.lima(-1, 12));
    INSERT INTO cancha.pago_reserva (reserva_id, caja_id, tipo, metodo, monto, nro_operacion, registrado_por, fecha) VALUES
        (r, caja_ayer, 'SALDO', 'YAPE', 60, 'SEED-YAPE-0001', v_enc, pg_temp.lima(-1, 18));
    -- NO_ASISTIO: 20:00-21:00 = 80, adelanto 40 no se devuelve
    INSERT INTO cancha.reserva (cancha_id, cliente_id, estado, inicio, fin, monto_total, origen, creado_por)
    VALUES (v_c1, cl3, 'NO_ASISTIO', pg_temp.lima(-1, 20), pg_temp.lima(-1, 21), 80, 'LLAMADA', v_enc)
    RETURNING id INTO r;
    INSERT INTO cancha.pago_reserva (reserva_id, caja_id, tipo, metodo, monto, registrado_por, fecha)
    VALUES (r, caja_ayer, 'ADELANTO', 'EFECTIVO', 40, v_enc, pg_temp.lima(-1, 15));
    -- CANCELADA: sin pagos
    INSERT INTO cancha.reserva (cancha_id, cliente_id, estado, inicio, fin, monto_total, origen,
                                motivo_cancelacion, creado_por)
    VALUES (v_c1, cl2, 'CANCELADA', pg_temp.lima(-1, 10), pg_temp.lima(-1, 11), 30, 'PRESENCIAL',
            'El cliente avisó que no podía venir', v_enc);
    INSERT INTO cancha.movimiento_caja (caja_id, tipo, metodo, concepto, monto, registrado_por, fecha)
    VALUES (caja_ayer, 'EGRESO', 'EFECTIVO', 'Pago de luz', 20, v_enc, pg_temp.lima(-1, 22));

    -- ---------------------------------------------------------------- reservas de hoy y próximos días
    -- CONFIRMADA mañana 20:00-22:00 = 160, adelanto 80 en la caja de hoy
    INSERT INTO cancha.reserva (cancha_id, cliente_id, estado, inicio, fin, monto_total, origen, creado_por)
    VALUES (v_c1, cl1, 'CONFIRMADA', pg_temp.lima(1, 20), pg_temp.lima(1, 22), 160, 'WHATSAPP', v_enc)
    RETURNING id INTO r;
    INSERT INTO cancha.pago_reserva (reserva_id, caja_id, tipo, metodo, monto, registrado_por)
    VALUES (r, caja_hoy, 'ADELANTO', 'EFECTIVO', 80, v_enc);
    -- PENDIENTE mañana 17:00-18:00 = 60 (límite de adelanto: la hora de inicio)
    INSERT INTO cancha.reserva (cancha_id, cliente_id, estado, inicio, fin, monto_total,
                                fecha_limite_adelanto, origen, creado_por)
    VALUES (v_c1, cl4, 'PENDIENTE', pg_temp.lima(1, 17), pg_temp.lima(1, 18), 60,
            pg_temp.lima(1, 17), 'WEB', v_enc);
    -- CONFIRMADA pasado mañana 08:00-10:00 = 60, pagada completa por Yape
    INSERT INTO cancha.reserva (cancha_id, cliente_id, estado, inicio, fin, monto_total, origen, creado_por)
    VALUES (v_c1, cl3, 'CONFIRMADA', pg_temp.lima(2, 8), pg_temp.lima(2, 10), 60, 'PRESENCIAL', v_enc)
    RETURNING id INTO r;
    INSERT INTO cancha.pago_reserva (reserva_id, caja_id, tipo, metodo, monto, nro_operacion, registrado_por)
    VALUES (r, caja_hoy, 'SALDO', 'PLIN', 60, 'SEED-PLIN-0002', v_enc);
    -- Cancha 2, PENDIENTE mañana 09:00-10:00 = 25
    INSERT INTO cancha.reserva (cancha_id, cliente_id, estado, inicio, fin, monto_total,
                                fecha_limite_adelanto, origen, creado_por)
    VALUES (v_c2, cl2, 'PENDIENTE', pg_temp.lima(1, 9), pg_temp.lima(1, 10), 25,
            pg_temp.lima(1, 9), 'LLAMADA', v_enc);
    -- Mantenimiento en 3 días 10:00-12:00
    INSERT INTO cancha.reserva (cancha_id, tipo, estado, inicio, fin, monto_total, observacion, creado_por)
    VALUES (v_c1, 'MANTENIMIENTO', 'CONFIRMADA', pg_temp.lima(3, 10), pg_temp.lima(3, 12), 0,
            'Pintado de líneas', v_admin);
    INSERT INTO cancha.movimiento_caja (caja_id, tipo, metodo, concepto, monto, registrado_por)
    VALUES (caja_hoy, 'EGRESO', 'EFECTIVO', 'Compra de una pelota', 35, v_enc);

    -- ---------------------------------------------------------------- bodega: catálogo
    INSERT INTO bodega.proveedor (nombre, ruc, telefono) VALUES
        ('Distribuidora Andina SAC', '20512345678', '014445566'),
        ('Bebidas del Sur EIRL',     '20698765432', '987000111')
    ON CONFLICT (ruc) DO NOTHING;
    SELECT id INTO prov FROM bodega.proveedor WHERE ruc = '20512345678';

    INSERT INTO bodega.producto (nombre, categoria_id, tipo, precio_compra, precio_venta, stock_minimo, unidades_por_caja)
    VALUES ('Envase cerveza 620ml', cat_envase, 'ENVASE', 0, 2.50, 10, 1) RETURNING id INTO p_env;
    INSERT INTO bodega.producto (codigo_barras, nombre, categoria_id, precio_compra, precio_venta, stock_minimo,
                                 unidades_por_caja, envase_id)
    VALUES ('7750000000011', 'Cerveza Pilsen 620ml', cat_cerveza, 4.00, 7.00, 12, 12, p_env) RETURNING id INTO p_pil;
    INSERT INTO bodega.producto (codigo_barras, nombre, categoria_id, precio_compra, precio_venta, stock_minimo,
                                 unidades_por_caja, envase_id)
    VALUES ('7750000000028', 'Cerveza Cusqueña 620ml', cat_cerveza, 5.00, 8.00, 12, 12, p_env) RETURNING id INTO p_cus;
    INSERT INTO bodega.producto (codigo_barras, nombre, categoria_id, precio_compra, precio_venta, stock_minimo, unidades_por_caja)
    VALUES ('7750000000035', 'Inca Kola 500ml', cat_gaseosa, 2.00, 3.50, 12, 12) RETURNING id INTO p_inca;
    INSERT INTO bodega.producto (codigo_barras, nombre, categoria_id, precio_compra, precio_venta, stock_minimo, unidades_por_caja)
    VALUES ('7750000000042', 'Coca Cola 500ml', cat_gaseosa, 2.00, 3.50, 12, 12) RETURNING id INTO p_coca;
    INSERT INTO bodega.producto (codigo_barras, nombre, categoria_id, precio_compra, precio_venta, stock_minimo, unidades_por_caja)
    VALUES ('7750000000059', 'Agua San Luis 625ml', cat_agua, 1.00, 2.00, 10, 15) RETURNING id INTO p_agua;

    -- Kardex: todo cambio de stock pasa por esta función (stock + movimiento_inventario)
    CREATE OR REPLACE FUNCTION pg_temp.mov(p bigint, tipo text, cant int, usr bigint, motivo text,
                                           c bigint DEFAULT NULL, v bigint DEFAULT NULL) RETURNS void AS $f$
    DECLARE ant int; sgn int;
    BEGIN
        SELECT stock INTO ant FROM bodega.producto WHERE id = p FOR UPDATE;
        sgn := CASE WHEN tipo IN ('ENTRADA_COMPRA','ANULACION_VENTA','AJUSTE_POSITIVO','DEVOLUCION_ENVASE') THEN 1 ELSE -1 END;
        UPDATE bodega.producto SET stock = ant + sgn * cant WHERE id = p;
        INSERT INTO bodega.movimiento_inventario
            (producto_id, tipo, cantidad, stock_anterior, stock_resultante, compra_id, venta_id, motivo, registrado_por)
        VALUES (p, tipo, cant, ant, ant + sgn * cant, c, v, motivo, usr);
    END $f$ LANGUAGE plpgsql;

    -- Inventario inicial (RN-PRO-03)
    PERFORM pg_temp.mov(p_env,  'AJUSTE_POSITIVO', 40, v_admin, 'Inventario inicial');
    PERFORM pg_temp.mov(p_pil,  'AJUSTE_POSITIVO', 24, v_admin, 'Inventario inicial');
    PERFORM pg_temp.mov(p_cus,  'AJUSTE_POSITIVO', 30, v_admin, 'Inventario inicial');
    PERFORM pg_temp.mov(p_inca, 'AJUSTE_POSITIVO', 20, v_admin, 'Inventario inicial');
    PERFORM pg_temp.mov(p_coca, 'AJUSTE_POSITIVO', 18, v_admin, 'Inventario inicial');
    PERFORM pg_temp.mov(p_agua, 'AJUSTE_POSITIVO',  4, v_admin, 'Inventario inicial');   -- queda en stock bajo

    -- ---------------------------------------------------------------- bodega: caja, compra y ventas
    INSERT INTO bodega.caja (fecha, monto_apertura, estado, abierta_por, abierta_at)
    VALUES ((now() AT TIME ZONE 'America/Lima')::date, 300, 'ABIERTA', v_enc, pg_temp.lima(0, 7))
    RETURNING id INTO bcaja;

    -- Compra: 2 cajas de Pilsen (24 u a 4.20) + 1 caja de Inca Kola (12 u a 2.10) = 126.00
    INSERT INTO bodega.compra (proveedor_id, nro_comprobante, total, registrado_por)
    VALUES (prov, 'F001-000123', 126.00, v_enc) RETURNING id INTO compra;
    INSERT INTO bodega.detalle_compra (compra_id, producto_id, cantidad_cajas, cantidad_unidades, costo_unitario) VALUES
        (compra, p_pil,  2, 24, 4.20),
        (compra, p_inca, 1, 12, 2.10);
    PERFORM pg_temp.mov(p_pil,  'ENTRADA_COMPRA', 24, v_enc, 'Compra #' || compra, compra);
    PERFORM pg_temp.mov(p_inca, 'ENTRADA_COMPRA', 12, v_enc, 'Compra #' || compra, compra);
    UPDATE bodega.producto SET precio_compra = 4.20 WHERE id = p_pil;
    UPDATE bodega.producto SET precio_compra = 2.10 WHERE id = p_inca;
    INSERT INTO bodega.movimiento_caja (caja_id, tipo, metodo, concepto, monto, registrado_por)
    VALUES (bcaja, 'EGRESO', 'EFECTIVO', 'Compra #' || compra, 126.00, v_enc);

    -- Venta 1: 2 Pilsen sin devolver envase (+2 envases de garantía) + 1 Inca Kola = 14 + 5 + 3.5 = 22.50
    INSERT INTO bodega.venta (caja_id, total, registrado_por) VALUES (bcaja, 22.50, v_enc) RETURNING id INTO venta;
    INSERT INTO bodega.detalle_venta (venta_id, producto_id, cantidad, precio_unitario, costo_unitario) VALUES
        (venta, p_pil,  2, 7.00, 4.20),
        (venta, p_inca, 1, 3.50, 2.10),
        (venta, p_env,  2, 2.50, 0.00);
    INSERT INTO bodega.pago_venta (venta_id, metodo, monto, nro_operacion) VALUES
        (venta, 'EFECTIVO', 10.00, NULL),
        (venta, 'YAPE',     12.50, 'SEED-YAPE-1001');
    PERFORM pg_temp.mov(p_pil,  'SALIDA_VENTA', 2, v_enc, 'Venta #' || venta, NULL, venta);
    PERFORM pg_temp.mov(p_inca, 'SALIDA_VENTA', 1, v_enc, 'Venta #' || venta, NULL, venta);
    PERFORM pg_temp.mov(p_env,  'SALIDA_VENTA', 2, v_enc, 'Venta #' || venta, NULL, venta);

    -- Venta 2: 3 Cusqueña entregando el envase vacío (+3 envases al stock) = 24.00 en efectivo
    INSERT INTO bodega.venta (caja_id, total, registrado_por) VALUES (bcaja, 24.00, v_enc) RETURNING id INTO venta;
    INSERT INTO bodega.detalle_venta (venta_id, producto_id, cantidad, precio_unitario, costo_unitario)
    VALUES (venta, p_cus, 3, 8.00, 5.00);
    INSERT INTO bodega.pago_venta (venta_id, metodo, monto) VALUES (venta, 'EFECTIVO', 24.00);
    PERFORM pg_temp.mov(p_cus, 'SALIDA_VENTA', 3, v_enc, 'Venta #' || venta, NULL, venta);
    PERFORM pg_temp.mov(p_env, 'DEVOLUCION_ENVASE', 3, v_enc, 'Envase vacío recibido en venta #' || venta, NULL, venta);

    -- Venta 3: 2 Coca Cola = 7.00 con Plin
    INSERT INTO bodega.venta (caja_id, total, registrado_por) VALUES (bcaja, 7.00, v_enc) RETURNING id INTO venta;
    INSERT INTO bodega.detalle_venta (venta_id, producto_id, cantidad, precio_unitario, costo_unitario)
    VALUES (venta, p_coca, 2, 3.50, 2.00);
    INSERT INTO bodega.pago_venta (venta_id, metodo, monto, nro_operacion) VALUES (venta, 'PLIN', 7.00, 'SEED-PLIN-1002');
    PERFORM pg_temp.mov(p_coca, 'SALIDA_VENTA', 2, v_enc, 'Venta #' || venta, NULL, venta);

    -- Merma registrada por el dueño
    PERFORM pg_temp.mov(p_agua, 'MERMA', 1, v_admin, 'Botella rota');
END
$seed$;
