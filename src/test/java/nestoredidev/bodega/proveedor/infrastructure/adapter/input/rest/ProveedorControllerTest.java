package nestoredidev.bodega.proveedor.infrastructure.adapter.input.rest;

import nestoredidev.bodega.proveedor.application.ports.input.ProveedorServicePort;
import nestoredidev.bodega.proveedor.application.ports.input.dto.ProveedorCommand;
import nestoredidev.bodega.proveedor.application.ports.input.dto.ProveedorDto;
import nestoredidev.bodega.proveedor.domain.exception.ProveedorNotFoundException;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;
import nestoredidev.common.exception.ConflictException;
import nestoredidev.support.AbstractControllerTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProveedorController.class)
class ProveedorControllerTest extends AbstractControllerTest {

    private static final String BASE = "/api/v2/bodega/proveedores";
    private static final String JSON = "{\"nombre\":\"Backus\",\"ruc\":\"20123456789\",\"telefono\":\"987654321\"}";

    @MockitoBean
    private ProveedorServicePort service;

    private static ProveedorDto proveedor(Long id, boolean activo) {
        return new ProveedorDto(id, "Backus", "20123456789", "987654321", activo, OffsetDateTime.parse("2026-10-01T10:00:00-05:00"));
    }

    @Test
    void sinToken_devuelve401() throws Exception {
        mvc.perform(get(BASE)).andExpect(status().isUnauthorized());
    }

    @Test
    void crear_encargadoPuede_devuelve201() throws Exception {
        when(service.crear(any(ProveedorCommand.class))).thenReturn(proveedor(1L, true));

        mvc.perform(comoEncargado(post(BASE).contentType(MediaType.APPLICATION_JSON).content(JSON)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Proveedor creado"))
                .andExpect(jsonPath("$.data[0].ruc").value("20123456789"));

        verify(service).crear(new ProveedorCommand("Backus", "20123456789", "987654321"));
    }

    @Test
    void crear_rucInvalidoONombreVacio_devuelve400() throws Exception {
        mvc.perform(comoEncargado(post(BASE).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Backus\",\"ruc\":\"123\"}"))).andExpect(status().isBadRequest());
        mvc.perform(comoEncargado(post(BASE).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"\"}"))).andExpect(status().isBadRequest());

        verify(service, never()).crear(any());
    }

    @Test
    void crear_sinRucNiTelefono_esValido() throws Exception {
        when(service.crear(any())).thenReturn(proveedor(1L, true));

        mvc.perform(comoEncargado(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Informal\"}")))
                .andExpect(status().isCreated());
    }

    @Test
    void crear_rucDuplicado_devuelve409() throws Exception {
        when(service.crear(any())).thenThrow(new ConflictException("RN-PRV-01: ya existe un proveedor con el RUC 20123456789"));

        mvc.perform(comoEncargado(post(BASE).contentType(MediaType.APPLICATION_JSON).content(JSON)))
                .andExpect(status().isConflict());
    }

    @Test
    void listar_pasaFiltrosYOrdenPorDefecto() throws Exception {
        when(service.listar(any(), any(), any(PageQuery.class)))
                .thenReturn(new PageResult<>(List.of(proveedor(1L, true)), 0, 10, 1, 1));

        mvc.perform(comoEncargado(get(BASE + "?q=back&activo=true"))).andExpect(status().isOk());

        verify(service).listar("back", true, new PageQuery(0, 10, "nombre", "asc"));
    }

    @Test
    void obtener_inexistente_devuelve404() throws Exception {
        when(service.obtener(9L)).thenThrow(new ProveedorNotFoundException(9L));

        mvc.perform(comoEncargado(get(BASE + "/9"))).andExpect(status().isNotFound());
    }

    @Test
    void actualizar_encargadoPuede() throws Exception {
        when(service.actualizar(any(), any())).thenReturn(proveedor(1L, true));

        mvc.perform(comoEncargado(put(BASE + "/1").contentType(MediaType.APPLICATION_JSON).content(JSON)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.message").value("Proveedor actualizado"));
    }

    @Test
    void activarYDesactivar_soloAdmin() throws Exception {
        when(service.activar(1L)).thenReturn(proveedor(1L, true));
        when(service.desactivar(1L)).thenReturn(proveedor(1L, false));

        mvc.perform(comoAdmin(patch(BASE + "/1/activar"))).andExpect(status().isOk());
        mvc.perform(comoAdmin(patch(BASE + "/1/desactivar")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].activo").value(false));
        mvc.perform(comoEncargado(patch(BASE + "/1/activar"))).andExpect(status().isForbidden());
        mvc.perform(comoEncargado(patch(BASE + "/1/desactivar"))).andExpect(status().isForbidden());
    }
}
