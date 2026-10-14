package nestoredidev.reporte.application.service;

import nestoredidev.common.exception.InvalidRequestException;
import nestoredidev.reporte.application.ports.input.dto.GananciaProductoDto;
import nestoredidev.reporte.application.ports.input.dto.ResumenGeneralDto;
import nestoredidev.reporte.application.ports.output.ReportePersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final LocalDate HOY = LocalDate.of(2026, 10, 13);

    @Mock
    private ReportePersistencePort persistence;

    private ReporteService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(HOY.atTime(10, 0).atZone(LIMA).toInstant(), LIMA);
        service = new ReporteService(persistence, clock);
    }

    private static OffsetDateTime inicio(LocalDate fecha) {
        return fecha.atStartOfDay(LIMA).toOffsetDateTime();
    }

    @Test
    void gananciaPorProducto_sinFechas_usaHoyYHastaExclusivoEsManiana() {
        List<GananciaProductoDto> esperado = List.of(
                new GananciaProductoDto(1L, "Agua", 3, new BigDecimal("9.00"), new BigDecimal("6.00"), new BigDecimal("3.00")));
        when(persistence.gananciaPorProducto(inicio(HOY), inicio(HOY.plusDays(1)))).thenReturn(esperado);

        List<GananciaProductoDto> resultado = service.gananciaPorProducto(null, null);

        assertThat(resultado).isEqualTo(esperado);
    }

    @Test
    void gananciaPorProducto_soloDesde_hastaSeIgualaADesde() {
        LocalDate desde = HOY.minusDays(5);
        when(persistence.gananciaPorProducto(inicio(desde), inicio(desde.plusDays(1)))).thenReturn(List.of());

        assertThat(service.gananciaPorProducto(desde, null)).isEmpty();

        verify(persistence).gananciaPorProducto(inicio(desde), inicio(desde.plusDays(1)));
    }

    @Test
    void gananciaPorProducto_rango_incluyeElDiaHastaCompleto() {
        LocalDate desde = HOY.minusDays(7);
        LocalDate hasta = HOY.minusDays(1);
        when(persistence.gananciaPorProducto(inicio(desde), inicio(hasta.plusDays(1)))).thenReturn(List.of());

        service.gananciaPorProducto(desde, hasta);

        verify(persistence).gananciaPorProducto(inicio(desde), inicio(hasta.plusDays(1)));
    }

    @Test
    void gananciaPorProducto_hastaAnteriorADesde_lanzaInvalidRequest() {
        assertThatThrownBy(() -> service.gananciaPorProducto(HOY, HOY.minusDays(1)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("hasta no puede ser anterior a desde");

        verify(persistence, never()).gananciaPorProducto(any(), any());
    }

    @Test
    void resumenGeneral_sinFechas_armaResumenConCanchaYBodegaSeparados() {
        OffsetDateTime ini = inicio(HOY);
        OffsetDateTime fin = inicio(HOY.plusDays(1));
        ResumenGeneralDto.Cancha cancha = new ResumenGeneralDto.Cancha(
                new BigDecimal("100.00"), new BigDecimal("50.00"), BigDecimal.ZERO, new BigDecimal("10.00"), 2);
        ResumenGeneralDto.Bodega bodega = new ResumenGeneralDto.Bodega(
                new BigDecimal("80.00"), 4, new BigDecimal("30.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        when(persistence.resumenCancha(ini, fin)).thenReturn(cancha);
        when(persistence.resumenBodega(ini, fin)).thenReturn(bodega);

        ResumenGeneralDto resumen = service.resumenGeneral(null, null);

        assertThat(resumen.desde()).isEqualTo(HOY);
        assertThat(resumen.hasta()).isEqualTo(HOY);
        assertThat(resumen.cancha()).isSameAs(cancha);
        assertThat(resumen.bodega()).isSameAs(bodega);
    }

    @Test
    void resumenGeneral_hastaAnteriorADesde_lanzaInvalidRequest() {
        assertThatThrownBy(() -> service.resumenGeneral(HOY, HOY.minusDays(2)))
                .isInstanceOf(InvalidRequestException.class);

        verify(persistence, never()).resumenCancha(any(), any());
        verify(persistence, never()).resumenBodega(any(), any());
    }
}
