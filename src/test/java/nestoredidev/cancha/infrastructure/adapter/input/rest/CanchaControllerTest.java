package nestoredidev.cancha.infrastructure.adapter.input.rest;

import nestoredidev.cancha.application.ports.input.CanchaServicePort;
import nestoredidev.cancha.application.ports.input.TarifaServicePort;
import nestoredidev.cancha.application.ports.input.dto.CambiarPrecioCommand;
import nestoredidev.cancha.application.ports.input.dto.CanchaCommand;
import nestoredidev.cancha.application.ports.input.dto.CanchaDto;
import nestoredidev.cancha.application.ports.input.dto.CreateTarifaCommand;
import nestoredidev.cancha.application.ports.input.dto.TarifaDto;
import nestoredidev.cancha.domain.exception.CanchaNotFoundException;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;
import nestoredidev.common.exception.ConflictException;
import nestoredidev.common.exception.DomainException;
import nestoredidev.support.AbstractControllerTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CanchaController.class)
class CanchaControllerTest extends AbstractControllerTest {

    @MockitoBean
    private CanchaServicePort canchaService;
    @MockitoBean
    private TarifaServicePort tarifaService;

    private static CanchaDto cancha(Long id, String nombre, boolean activa) {
        return new CanchaDto(id, nombre, "Grass", activa, OffsetDateTime.parse("2026-01-01T00:00:00-05:00"));
    }

    private static TarifaDto tarifa(Long id, String precio) {
        return new TarifaDto(id, 1L, (short) 1, LocalTime.of(18, 0), LocalTime.of(23, 0), new BigDecimal(precio), true,
                OffsetDateTime.parse("2026-01-01T00:00:00-05:00"));
    }

    private static final String JSON_CANCHA = "{\"nombre\":\"Cancha 1\",\"descripcion\":\"Grass\"}";
    private static final String JSON_TARIFA = "{\"diaSemana\":1,\"horaInicio\":\"18:00\",\"horaFin\":\"23:00\",\"precioHora\":60}";

    // ================================================================ canchas

    @Test
    void sinToken_devuelve401() throws Exception {
        mvc.perform(get("/api/v2/canchas")).andExpect(status().isUnauthorized());
    }

    @Test
    void crear_admin_devuelve201() throws Exception {
        when(canchaService.crear(any(CanchaCommand.class))).thenReturn(cancha(1L, "Cancha 1", true));

        mvc.perform(comoAdmin(post("/api/v2/canchas").contentType(MediaType.APPLICATION_JSON).content(JSON_CANCHA)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Cancha creada"))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].nombre").value("Cancha 1"))
                .andExpect(jsonPath("$.data[0].activa").value(true));

        verify(canchaService).crear(new CanchaCommand("Cancha 1", "Grass"));
    }

    @Test
    void crear_encargado_devuelve403() throws Exception {
        mvc.perform(comoEncargado(post("/api/v2/canchas").contentType(MediaType.APPLICATION_JSON).content(JSON_CANCHA)))
                .andExpect(status().isForbidden());

        verify(canchaService, never()).crear(any());
    }

    @Test
    void crear_sinNombre_devuelve400() throws Exception {
        mvc.perform(comoAdmin(post("/api/v2/canchas").contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\" \"}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value(org.hamcrest.Matchers.containsString("nombre")));
    }

    @Test
    void crear_nombreDuplicado_devuelve409() throws Exception {
        when(canchaService.crear(any())).thenThrow(new ConflictException("RN-CAN-01: ya existe una cancha llamada 'Cancha 1'"));

        mvc.perform(comoAdmin(post("/api/v2/canchas").contentType(MediaType.APPLICATION_JSON).content(JSON_CANCHA)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("RN-CAN-01")));
    }

    @Test
    void listar_cualquierUsuarioAutenticadoPuede_yPasaElFiltroActiva() throws Exception {
        when(canchaService.listar(any(), any(PageQuery.class))).thenReturn(
                new PageResult<>(List.of(cancha(1L, "Cancha 1", true)), 0, 10, 1, 1));

        mvc.perform(comoEncargado(get("/api/v2/canchas?activa=true")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Canchas obtenidas"))
                .andExpect(jsonPath("$.data[0].nombre").value("Cancha 1"))
                .andExpect(jsonPath("$.total_elements").value(1));

        verify(canchaService).listar(true, new PageQuery(0, 10, "nombre", "asc"));
    }

    @Test
    void listar_sinFiltro_pasaNulo() throws Exception {
        when(canchaService.listar(any(), any())).thenReturn(new PageResult<>(List.of(), 0, 10, 0, 0));

        mvc.perform(comoEncargado(get("/api/v2/canchas"))).andExpect(status().isOk());

        verify(canchaService).listar(null, new PageQuery(0, 10, "nombre", "asc"));
    }

    @Test
    void listar_ordenNoPermitido_devuelve400() throws Exception {
        mvc.perform(comoEncargado(get("/api/v2/canchas?sortBy=descripcion"))).andExpect(status().isBadRequest());
    }

    @Test
    void obtener_existenteEInexistente() throws Exception {
        when(canchaService.obtener(1L)).thenReturn(cancha(1L, "Cancha 1", true));
        when(canchaService.obtener(9L)).thenThrow(new CanchaNotFoundException(9L));

        mvc.perform(comoEncargado(get("/api/v2/canchas/1")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").value(1));
        mvc.perform(comoEncargado(get("/api/v2/canchas/9")))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Cancha con id 9 no existe"));
    }

    @Test
    void actualizar_soloAdmin() throws Exception {
        when(canchaService.actualizar(eq(1L), any())).thenReturn(cancha(1L, "Cancha 1", true));

        mvc.perform(comoAdmin(put("/api/v2/canchas/1").contentType(MediaType.APPLICATION_JSON).content(JSON_CANCHA)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.message").value("Cancha actualizada"));
        mvc.perform(comoEncargado(put("/api/v2/canchas/1").contentType(MediaType.APPLICATION_JSON).content(JSON_CANCHA)))
                .andExpect(status().isForbidden());
    }

    @Test
    void activar_soloAdmin() throws Exception {
        when(canchaService.activar(1L)).thenReturn(cancha(1L, "Cancha 1", true));

        mvc.perform(comoAdmin(patch("/api/v2/canchas/1/activar")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.message").value("Cancha activada"));
        mvc.perform(comoEncargado(patch("/api/v2/canchas/1/activar"))).andExpect(status().isForbidden());
    }

    @Test
    void desactivar_admin_devuelve200() throws Exception {
        when(canchaService.desactivar(1L)).thenReturn(cancha(1L, "Cancha 1", false));

        mvc.perform(comoAdmin(patch("/api/v2/canchas/1/desactivar")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].activa").value(false));
    }

    @Test
    void desactivar_conReservasFuturas_devuelve422() throws Exception {
        when(canchaService.desactivar(1L)).thenThrow(new DomainException("RN-CAN-03: la cancha tiene reservas futuras"));

        mvc.perform(comoAdmin(patch("/api/v2/canchas/1/desactivar")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("RN-CAN-03")));
    }

    @Test
    void desactivar_encargado_devuelve403() throws Exception {
        mvc.perform(comoEncargado(patch("/api/v2/canchas/1/desactivar"))).andExpect(status().isForbidden());

        verify(canchaService, never()).desactivar(any());
    }

    // ================================================================ tarifas

    @Test
    void crearTarifa_admin_devuelve201ConLasHorasParseadas() throws Exception {
        when(tarifaService.crear(eq(1L), any(CreateTarifaCommand.class))).thenReturn(tarifa(7L, "60.00"));

        mvc.perform(comoAdmin(post("/api/v2/canchas/1/tarifas").contentType(MediaType.APPLICATION_JSON).content(JSON_TARIFA)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Tarifa creada"))
                .andExpect(jsonPath("$.data[0].id").value(7))
                .andExpect(jsonPath("$.data[0].horaInicio").value("18:00:00"))
                .andExpect(jsonPath("$.data[0].precioHora").value(60.0));

        verify(tarifaService).crear(1L, new CreateTarifaCommand((short) 1, LocalTime.of(18, 0), LocalTime.of(23, 0),
                new BigDecimal("60")));
    }

    @Test
    void crearTarifa_encargado_devuelve403() throws Exception {
        mvc.perform(comoEncargado(post("/api/v2/canchas/1/tarifas").contentType(MediaType.APPLICATION_JSON).content(JSON_TARIFA)))
                .andExpect(status().isForbidden());
    }

    @Test
    void crearTarifa_conDiaFueraDeRangoOPrecioCero_devuelve400() throws Exception {
        mvc.perform(comoAdmin(post("/api/v2/canchas/1/tarifas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"diaSemana\":8,\"horaInicio\":\"18:00\",\"horaFin\":\"23:00\",\"precioHora\":60}")))
                .andExpect(status().isBadRequest());
        mvc.perform(comoAdmin(post("/api/v2/canchas/1/tarifas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"horaInicio\":\"18:00\",\"horaFin\":\"23:00\",\"precioHora\":0}")))
                .andExpect(status().isBadRequest());
        mvc.perform(comoAdmin(post("/api/v2/canchas/1/tarifas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"precioHora\":10}")))
                .andExpect(status().isBadRequest());

        verify(tarifaService, never()).crear(any(), any());
    }

    @Test
    void crearTarifa_queSeCruza_devuelve409() throws Exception {
        when(tarifaService.crear(eq(1L), any())).thenThrow(new ConflictException("RN-TAR-01: la franja se cruza"));

        mvc.perform(comoAdmin(post("/api/v2/canchas/1/tarifas").contentType(MediaType.APPLICATION_JSON).content(JSON_TARIFA)))
                .andExpect(status().isConflict());
    }

    @Test
    void listarTarifas_porDefectoSoloActivas() throws Exception {
        when(tarifaService.listar(1L, true)).thenReturn(List.of(tarifa(7L, "60.00"), tarifa(8L, "80.00")));

        mvc.perform(comoEncargado(get("/api/v2/canchas/1/tarifas")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Tarifas obtenidas"))
                .andExpect(jsonPath("$.data.length()").value(2));

        verify(tarifaService).listar(1L, true);
    }

    @Test
    void listarTarifas_conSoloActivasFalse_incluyeInactivas() throws Exception {
        when(tarifaService.listar(1L, false)).thenReturn(List.of());

        mvc.perform(comoEncargado(get("/api/v2/canchas/1/tarifas?soloActivas=false"))).andExpect(status().isOk());

        verify(tarifaService).listar(1L, false);
    }

    @Test
    void listarTarifas_canchaInexistente_devuelve404() throws Exception {
        when(tarifaService.listar(9L, true)).thenThrow(new CanchaNotFoundException(9L));

        mvc.perform(comoEncargado(get("/api/v2/canchas/9/tarifas"))).andExpect(status().isNotFound());
    }

    @Test
    void obtenerTarifa_devuelve200() throws Exception {
        when(tarifaService.obtener(7L)).thenReturn(tarifa(7L, "60.00"));

        mvc.perform(comoEncargado(get("/api/v2/tarifas/7")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].canchaId").value(1));
    }

    @Test
    void desactivarTarifa_soloAdmin() throws Exception {
        when(tarifaService.desactivar(7L)).thenReturn(tarifa(7L, "60.00"));

        mvc.perform(comoAdmin(patch("/api/v2/tarifas/7/desactivar"))).andExpect(status().isOk());
        mvc.perform(comoEncargado(patch("/api/v2/tarifas/7/desactivar"))).andExpect(status().isForbidden());
    }

    @Test
    void cambiarPrecio_admin_devuelve201ConLaTarifaNueva() throws Exception {
        when(tarifaService.cambiarPrecio(eq(7L), any(CambiarPrecioCommand.class))).thenReturn(tarifa(9L, "75.00"));

        mvc.perform(comoAdmin(post("/api/v2/tarifas/7/cambiar-precio").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"precioHora\":75}")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Tarifa anterior desactivada y nueva tarifa creada"))
                .andExpect(jsonPath("$.data[0].id").value(9));

        verify(tarifaService).cambiarPrecio(7L, new CambiarPrecioCommand(new BigDecimal("75")));
    }

    @Test
    void cambiarPrecio_sinPrecioOEncargado_devuelve400Y403() throws Exception {
        mvc.perform(comoAdmin(post("/api/v2/tarifas/7/cambiar-precio").contentType(MediaType.APPLICATION_JSON).content("{}")))
                .andExpect(status().isBadRequest());
        mvc.perform(comoEncargado(post("/api/v2/tarifas/7/cambiar-precio").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"precioHora\":75}")))
                .andExpect(status().isForbidden());
    }
}
