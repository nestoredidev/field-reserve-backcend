package nestoredidev.reporte.infrastructure.adapter.input.rest;

import nestoredidev.common.exception.InvalidRequestException;
import nestoredidev.reporte.application.ports.input.ReporteServicePort;
import nestoredidev.reporte.application.ports.input.dto.GananciaProductoDto;
import nestoredidev.reporte.application.ports.input.dto.ResumenGeneralDto;
import nestoredidev.support.AbstractControllerTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReporteController.class)
class ReporteControllerTest extends AbstractControllerTest {

    private static final String BASE = "/api/v2/reportes";

    @MockitoBean
    private ReporteServicePort service;

    @Test
    void sinToken_devuelve401() throws Exception {
        mvc.perform(get(BASE + "/resumen-general")).andExpect(status().isUnauthorized());
    }

    @Test
    void encargado_noPuedeVerReportes_403() throws Exception {
        mvc.perform(comoEncargado(get(BASE + "/ganancia-productos"))).andExpect(status().isForbidden());
        mvc.perform(comoEncargado(get(BASE + "/resumen-general"))).andExpect(status().isForbidden());

        verify(service, never()).gananciaPorProducto(any(), any());
        verify(service, never()).resumenGeneral(any(), any());
    }

    @Test
    void gananciaProductos_admin_pasaElRangoDeFechas() throws Exception {
        when(service.gananciaPorProducto(any(), any())).thenReturn(List.of(new GananciaProductoDto(1L, "Cristal", 10,
                new BigDecimal("35.00"), new BigDecimal("20.00"), new BigDecimal("15.00"))));

        mvc.perform(comoAdmin(get(BASE + "/ganancia-productos?desde=2026-10-01&hasta=2026-10-13")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Ganancia por producto"))
                .andExpect(jsonPath("$.data[0].nombre").value("Cristal"))
                .andExpect(jsonPath("$.data[0].ganancia").value(15.0));

        verify(service).gananciaPorProducto(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 13));
    }

    @Test
    void gananciaProductos_sinFechas_pasaNulos() throws Exception {
        when(service.gananciaPorProducto(any(), any())).thenReturn(List.of());

        mvc.perform(comoAdmin(get(BASE + "/ganancia-productos"))).andExpect(status().isOk());

        verify(service).gananciaPorProducto(null, null);
    }

    @Test
    void gananciaProductos_fechaMalFormada_devuelve400() throws Exception {
        mvc.perform(comoAdmin(get(BASE + "/ganancia-productos?desde=ayer"))).andExpect(status().isBadRequest());
    }

    @Test
    void resumenGeneral_admin_devuelveCanchaYBodegaPorSeparado() throws Exception {
        LocalDate d = LocalDate.of(2026, 10, 13);
        when(service.resumenGeneral(any(), any())).thenReturn(new ResumenGeneralDto(d, d,
                new ResumenGeneralDto.Cancha(new BigDecimal("100.00"), new BigDecimal("50.00"), BigDecimal.ZERO,
                        new BigDecimal("10.00"), 2),
                new ResumenGeneralDto.Bodega(new BigDecimal("80.00"), 4, new BigDecimal("30.00"), BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO)));

        mvc.perform(comoAdmin(get(BASE + "/resumen-general?desde=2026-10-13")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Resumen general"))
                .andExpect(jsonPath("$.data[0].desde").value("2026-10-13"))
                .andExpect(jsonPath("$.data[0].cancha.reservasCompletadas").value(2))
                .andExpect(jsonPath("$.data[0].bodega.ventasRegistradas").value(4));

        verify(service).resumenGeneral(d, null);
    }

    @Test
    void resumenGeneral_hastaAnteriorADesde_devuelve400() throws Exception {
        when(service.resumenGeneral(any(), any())).thenThrow(new InvalidRequestException("hasta no puede ser anterior a desde"));

        mvc.perform(comoAdmin(get(BASE + "/resumen-general?desde=2026-10-13&hasta=2026-10-01")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("hasta no puede ser anterior a desde"));
    }
}
