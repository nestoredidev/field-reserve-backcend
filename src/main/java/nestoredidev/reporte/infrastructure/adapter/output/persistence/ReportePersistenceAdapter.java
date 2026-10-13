package nestoredidev.reporte.infrastructure.adapter.output.persistence;

import lombok.RequiredArgsConstructor;
import nestoredidev.common.domain.Money;
import nestoredidev.reporte.application.ports.input.dto.GananciaProductoDto;
import nestoredidev.reporte.application.ports.input.dto.ResumenGeneralDto;
import nestoredidev.reporte.application.ports.output.ReportePersistencePort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** Consultas de solo lectura: este es el único lugar donde se leen ambos módulos a la vez (RN-G01). */
@Component
@RequiredArgsConstructor
public class ReportePersistenceAdapter implements ReportePersistencePort {
    private final JdbcClient jdbc;

    @Override
    public List<GananciaProductoDto> gananciaPorProducto(OffsetDateTime desde, OffsetDateTime hasta) {
        return jdbc.sql("""
                        SELECT p.id, p.nombre,
                               SUM(d.cantidad)                                     AS unidades,
                               SUM(d.subtotal)                                     AS total_vendido,
                               SUM(d.cantidad * d.costo_unitario)                  AS costo_total,
                               SUM(d.subtotal) - SUM(d.cantidad * d.costo_unitario) AS ganancia
                        FROM bodega.detalle_venta d
                                 JOIN bodega.venta v ON v.id = d.venta_id AND v.estado = 'REGISTRADA'
                                 JOIN bodega.producto p ON p.id = d.producto_id
                        WHERE v.fecha >= :desde AND v.fecha < :hasta
                        GROUP BY p.id, p.nombre
                        ORDER BY ganancia DESC, p.nombre
                        """)
                .param("desde", desde).param("hasta", hasta)
                .query((rs, n) -> new GananciaProductoDto(rs.getLong("id"), rs.getString("nombre"),
                        rs.getLong("unidades"), Money.of(rs.getBigDecimal("total_vendido")),
                        Money.of(rs.getBigDecimal("costo_total")), Money.of(rs.getBigDecimal("ganancia"))))
                .list();
    }

    @Override
    public ResumenGeneralDto.Cancha resumenCancha(OffsetDateTime desde, OffsetDateTime hasta) {
        BigDecimal efectivo = suma("""
                SELECT SUM(CASE WHEN tipo = 'DEVOLUCION' THEN -monto ELSE monto END) FROM cancha.pago_reserva
                WHERE metodo = 'EFECTIVO' AND fecha >= :desde AND fecha < :hasta""", desde, hasta);
        BigDecimal digital = suma("""
                SELECT SUM(CASE WHEN tipo = 'DEVOLUCION' THEN -monto ELSE monto END) FROM cancha.pago_reserva
                WHERE metodo <> 'EFECTIVO' AND fecha >= :desde AND fecha < :hasta""", desde, hasta);
        BigDecimal ingresos = suma("""
                SELECT SUM(monto) FROM cancha.movimiento_caja
                WHERE tipo = 'INGRESO' AND fecha >= :desde AND fecha < :hasta""", desde, hasta);
        BigDecimal egresos = suma("""
                SELECT SUM(monto) FROM cancha.movimiento_caja
                WHERE tipo = 'EGRESO' AND fecha >= :desde AND fecha < :hasta""", desde, hasta);
        long completadas = jdbc.sql("""
                        SELECT COUNT(*) FROM cancha.reserva
                        WHERE tipo = 'RESERVA' AND estado = 'COMPLETADA' AND inicio >= :desde AND inicio < :hasta""")
                .param("desde", desde).param("hasta", hasta).query(Long.class).single();
        return new ResumenGeneralDto.Cancha(efectivo, digital, ingresos, egresos, completadas);
    }

    @Override
    public ResumenGeneralDto.Bodega resumenBodega(OffsetDateTime desde, OffsetDateTime hasta) {
        BigDecimal ventas = suma("""
                SELECT SUM(total) FROM bodega.venta
                WHERE estado = 'REGISTRADA' AND fecha >= :desde AND fecha < :hasta""", desde, hasta);
        long cantidad = jdbc.sql("""
                        SELECT COUNT(*) FROM bodega.venta
                        WHERE estado = 'REGISTRADA' AND fecha >= :desde AND fecha < :hasta""")
                .param("desde", desde).param("hasta", hasta).query(Long.class).single();
        BigDecimal ganancia = suma("""
                SELECT SUM(d.subtotal - d.cantidad * d.costo_unitario)
                FROM bodega.detalle_venta d JOIN bodega.venta v ON v.id = d.venta_id AND v.estado = 'REGISTRADA'
                WHERE v.fecha >= :desde AND v.fecha < :hasta""", desde, hasta);
        BigDecimal compras = suma("""
                SELECT SUM(total) FROM bodega.compra
                WHERE estado = 'REGISTRADA' AND fecha >= :desde AND fecha < :hasta""", desde, hasta);
        BigDecimal ingresos = suma("""
                SELECT SUM(monto) FROM bodega.movimiento_caja
                WHERE tipo = 'INGRESO' AND fecha >= :desde AND fecha < :hasta""", desde, hasta);
        BigDecimal egresos = suma("""
                SELECT SUM(monto) FROM bodega.movimiento_caja
                WHERE tipo = 'EGRESO' AND fecha >= :desde AND fecha < :hasta""", desde, hasta);
        return new ResumenGeneralDto.Bodega(ventas, cantidad, ganancia, compras, ingresos, egresos);
    }

    private BigDecimal suma(String sql, OffsetDateTime desde, OffsetDateTime hasta) {
        BigDecimal value = jdbc.sql(sql).param("desde", desde).param("hasta", hasta)
                .query(BigDecimal.class).optional().orElse(null);
        return value == null ? Money.ZERO : Money.of(value);
    }
}
