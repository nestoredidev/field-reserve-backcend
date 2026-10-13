package nestoredidev.cajacancha.infrastructure.adapter.output.persistence;

import nestoredidev.common.domain.EstadoCaja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface CajaCanchaRepository extends JpaRepository<CajaCanchaEntity, Long> {

    Optional<CajaCanchaEntity> findFirstByEstado(EstadoCaja estado);

    @Query(value = "select cobros_efectivo as cobrosEfectivo, cobros_digitales as cobrosDigitales, "
            + "otros_ingresos_efectivo as otrosIngresosEfectivo, egresos_efectivo as egresosEfectivo, "
            + "efectivo_esperado as efectivoEsperado, diferencia as diferencia "
            + "from cancha.v_caja_resumen where caja_id = :cajaId", nativeQuery = true)
    Optional<ResumenRow> resumen(@Param("cajaId") Long cajaId);

    interface ResumenRow {
        BigDecimal getCobrosEfectivo();

        BigDecimal getCobrosDigitales();

        BigDecimal getOtrosIngresosEfectivo();

        BigDecimal getEgresosEfectivo();

        BigDecimal getEfectivoEsperado();

        BigDecimal getDiferencia();
    }
}
