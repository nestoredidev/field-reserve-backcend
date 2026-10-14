package nestoredidev.support;

import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Inserta filas con SQL nativo (las mismas columnas de db/schema.sql) y devuelve el id generado. Así los tests
 * de repository no dependen de los constructores de las entidades ni del adaptador.
 */
public class TestData {
    public static final OffsetDateTime BASE = OffsetDateTime.parse("2026-10-13T10:00:00-05:00");

    private final EntityManager em;
    private int secuencia = 0;

    public TestData(EntityManager em) {
        this.em = em;
    }

    // ---------------------------------------------------------------- helpers

    private Long insert(String tabla, String columnas, String valores) {
        Number id = (Number) em.createNativeQuery("with ins as (insert into " + tabla + " (" + columnas
                + ") values (" + valores + ") returning id) select id from ins").getSingleResult();
        return id.longValue();
    }

    public int siguiente() {
        return ++secuencia;
    }

    static String q(String s) {
        return s == null ? "null" : "'" + s.replace("'", "''") + "'";
    }

    static String ts(OffsetDateTime t) {
        return t == null ? "null" : "timestamptz '" + t.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) + "'";
    }

    static String n(Object o) {
        return o == null ? "null" : o.toString();
    }

    public static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    // ---------------------------------------------------------------- core

    public Long usuario(String username, String rol, boolean activo) {
        return insert("core.usuario", "nombre, username, password_hash, rol, activo",
                q("Nombre " + username) + ", " + q(username) + ", 'hash', " + q(rol) + ", " + activo);
    }

    public Long usuario() {
        return usuario("user" + siguiente(), "ADMIN", true);
    }

    // ---------------------------------------------------------------- cancha

    public Long cancha(String nombre, boolean activa) {
        return insert("cancha.cancha", "nombre, descripcion, activa", q(nombre) + ", 'desc', " + activa);
    }

    public Long cancha() {
        return cancha("Cancha " + siguiente(), true);
    }

    /** dia null = tarifa general; horas en formato HH:mm. */
    public Long tarifa(Long canchaId, Integer dia, String horaInicio, String horaFin, String precio, boolean activa) {
        return insert("cancha.tarifa", "cancha_id, dia_semana, hora_inicio, hora_fin, precio_hora, activa",
                canchaId + ", " + n(dia) + ", time " + q(horaInicio) + ", time " + q(horaFin) + ", " + precio + ", "
                        + activa);
    }

    public Long cliente(String nombre, String telefono) {
        return insert("cancha.cliente", "nombre, telefono", q(nombre) + ", " + q(telefono));
    }

    public Long cliente() {
        return cliente("Cliente " + siguiente(), "9" + String.format("%08d", siguiente()));
    }

    /** Reserva de un cliente con monto positivo. */
    public Long reserva(Long canchaId, Long clienteId, String estado, OffsetDateTime inicio, OffsetDateTime fin,
                        String monto, OffsetDateTime fechaLimiteAdelanto) {
        return insert("cancha.reserva", "cancha_id, cliente_id, tipo, estado, inicio, fin, monto_total, "
                        + "fecha_limite_adelanto, origen",
                canchaId + ", " + clienteId + ", 'RESERVA', " + q(estado) + ", " + ts(inicio) + ", " + ts(fin) + ", "
                        + monto + ", " + ts(fechaLimiteAdelanto) + ", 'PRESENCIAL'");
    }

    public Long mantenimiento(Long canchaId, String estado, OffsetDateTime inicio, OffsetDateTime fin) {
        return insert("cancha.reserva", "cancha_id, cliente_id, tipo, estado, inicio, fin, monto_total, origen",
                canchaId + ", null, 'MANTENIMIENTO', " + q(estado) + ", " + ts(inicio) + ", " + ts(fin)
                        + ", 0, 'PRESENCIAL'");
    }

    public Long cajaCancha(String estado, String apertura, Long usuarioId) {
        return caja("cancha.caja", estado, apertura, usuarioId);
    }

    public Long cajaBodega(String estado, String apertura, Long usuarioId) {
        return caja("bodega.caja", estado, apertura, usuarioId);
    }

    private Long caja(String tabla, String estado, String apertura, Long usuarioId) {
        boolean cerrada = "CERRADA".equals(estado);
        return insert(tabla, "fecha, monto_apertura, monto_cierre_contado, estado, abierta_por, cerrada_por, "
                        + "abierta_at, cerrada_at",
                "date '2026-10-13', " + apertura + ", " + (cerrada ? apertura : "null") + ", " + q(estado) + ", "
                        + usuarioId + ", " + (cerrada ? usuarioId : "null") + ", " + ts(BASE) + ", "
                        + (cerrada ? ts(BASE.plusHours(8)) : "null"));
    }

    public Long pagoReserva(Long reservaId, Long cajaId, String tipo, String metodo, String monto, String nro,
                            Long usuarioId, OffsetDateTime fecha) {
        return insert("cancha.pago_reserva", "reserva_id, caja_id, tipo, metodo, monto, nro_operacion, "
                        + "registrado_por, fecha",
                reservaId + ", " + cajaId + ", " + q(tipo) + ", " + q(metodo) + ", " + monto + ", " + q(nro) + ", "
                        + usuarioId + ", " + ts(fecha));
    }

    public Long movimientoCancha(Long cajaId, String tipo, String metodo, String monto, Long usuarioId) {
        return movimiento("cancha.movimiento_caja", cajaId, tipo, metodo, monto, usuarioId);
    }

    public Long movimientoBodega(Long cajaId, String tipo, String metodo, String monto, Long usuarioId) {
        return movimiento("bodega.movimiento_caja", cajaId, tipo, metodo, monto, usuarioId);
    }

    private Long movimiento(String tabla, Long cajaId, String tipo, String metodo, String monto, Long usuarioId) {
        return insert(tabla, "caja_id, tipo, metodo, concepto, monto, registrado_por",
                cajaId + ", " + q(tipo) + ", " + q(metodo) + ", 'concepto', " + monto + ", " + usuarioId);
    }

    // ---------------------------------------------------------------- bodega

    public Long categoria(String nombre) {
        return insert("bodega.categoria", "nombre", q(nombre));
    }

    public Long categoria() {
        return categoria("Categoria " + siguiente());
    }

    public Long proveedor(String nombre, String ruc, boolean activo) {
        return insert("bodega.proveedor", "nombre, ruc, activo", q(nombre) + ", " + q(ruc) + ", " + activo);
    }

    public Long producto(String codigoBarras, String nombre, Long categoriaId, String tipo, int stock, int stockMinimo,
                         Long envaseId, boolean activo) {
        return insert("bodega.producto", "codigo_barras, nombre, categoria_id, tipo, precio_compra, precio_venta, "
                        + "stock, stock_minimo, envase_id, activo",
                q(codigoBarras) + ", " + q(nombre) + ", " + categoriaId + ", " + q(tipo) + ", 2.00, 3.00, " + stock
                        + ", " + stockMinimo + ", " + n(envaseId) + ", " + activo);
    }

    public Long venta(Long cajaId, String estado, String total, Long usuarioId) {
        return insert("bodega.venta", "caja_id, estado, total, registrado_por, fecha",
                cajaId + ", " + q(estado) + ", " + total + ", " + usuarioId + ", " + ts(BASE));
    }

    public Long detalleVenta(Long ventaId, Long productoId, int cantidad, String precio, String costo) {
        return insert("bodega.detalle_venta", "venta_id, producto_id, cantidad, precio_unitario, costo_unitario",
                ventaId + ", " + productoId + ", " + cantidad + ", " + precio + ", " + costo);
    }

    public Long pagoVenta(Long ventaId, String metodo, String monto, String nro) {
        return insert("bodega.pago_venta", "venta_id, metodo, monto, nro_operacion",
                ventaId + ", " + q(metodo) + ", " + monto + ", " + q(nro));
    }

    public Long compra(Long proveedorId, String estado, String total, Long usuarioId) {
        return insert("bodega.compra", "proveedor_id, estado, total, registrado_por",
                n(proveedorId) + ", " + q(estado) + ", " + total + ", " + usuarioId);
    }

    public Long detalleCompra(Long compraId, Long productoId, int unidades, String costo) {
        return insert("bodega.detalle_compra", "compra_id, producto_id, cantidad_cajas, cantidad_unidades, costo_unitario",
                compraId + ", " + productoId + ", 0, " + unidades + ", " + costo);
    }

    public Long movimientoInventario(Long productoId, String tipo, int cantidad, int anterior, int resultante,
                                     Long compraId, Long ventaId, Long usuarioId) {
        return insert("bodega.movimiento_inventario", "producto_id, tipo, cantidad, stock_anterior, stock_resultante, "
                        + "compra_id, venta_id, registrado_por",
                productoId + ", " + q(tipo) + ", " + cantidad + ", " + anterior + ", " + resultante + ", "
                        + n(compraId) + ", " + n(ventaId) + ", " + usuarioId);
    }
}
