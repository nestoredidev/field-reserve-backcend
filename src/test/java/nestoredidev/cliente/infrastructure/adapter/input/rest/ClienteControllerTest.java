package nestoredidev.cliente.infrastructure.adapter.input.rest;

import nestoredidev.cliente.application.ports.input.ClienteServicePort;
import nestoredidev.cliente.application.ports.input.dto.ClienteCommand;
import nestoredidev.cliente.application.ports.input.dto.ClienteDto;
import nestoredidev.cliente.domain.exception.ClienteNotFoundException;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClienteController.class)
class ClienteControllerTest extends AbstractControllerTest {

    @MockitoBean
    private ClienteServicePort service;

    private static ClienteDto cliente(Long id) {
        return new ClienteDto(id, "Ana Torres", "911111111", "vip", OffsetDateTime.parse("2026-10-01T10:00:00-05:00"));
    }

    private static final String JSON = "{\"nombre\":\"Ana Torres\",\"telefono\":\"911111111\",\"notas\":\"vip\"}";

    @Test
    void sinToken_devuelve401() throws Exception {
        mvc.perform(get("/api/v2/clientes")).andExpect(status().isUnauthorized());
    }

    @Test
    void crear_encargadoPuede_devuelve201() throws Exception {
        when(service.crear(any(ClienteCommand.class))).thenReturn(cliente(1L));

        mvc.perform(comoEncargado(post("/api/v2/clientes").contentType(MediaType.APPLICATION_JSON).content(JSON)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Cliente creado"))
                .andExpect(jsonPath("$.data[0].telefono").value("911111111"));

        verify(service).crear(new ClienteCommand("Ana Torres", "911111111", "vip"));
    }

    @Test
    void crear_telefonoInvalidoONombreVacio_devuelve400() throws Exception {
        mvc.perform(comoEncargado(post("/api/v2/clientes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana\",\"telefono\":\"811111111\"}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value(org.hamcrest.Matchers.containsString("telefono")));
        mvc.perform(comoEncargado(post("/api/v2/clientes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"telefono\":\"911111111\"}")))
                .andExpect(status().isBadRequest());

        verify(service, never()).crear(any());
    }

    @Test
    void crear_telefonoDuplicado_devuelve409() throws Exception {
        when(service.crear(any())).thenThrow(new ConflictException("RN-CLI-02: ya existe un cliente con el teléfono 911111111"));

        mvc.perform(comoEncargado(post("/api/v2/clientes").contentType(MediaType.APPLICATION_JSON).content(JSON)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("RN-CLI-02")));
    }

    @Test
    void listar_pasaLaBusquedaYElOrdenPorDefecto() throws Exception {
        when(service.listar(any(), any(PageQuery.class)))
                .thenReturn(new PageResult<>(List.of(cliente(1L)), 0, 10, 1, 1));

        mvc.perform(comoEncargado(get("/api/v2/clientes?q=ana")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].nombre").value("Ana Torres"));

        verify(service).listar("ana", new PageQuery(0, 10, "nombre", "asc"));
    }

    @Test
    void listar_ordenNoPermitido_devuelve400() throws Exception {
        mvc.perform(comoEncargado(get("/api/v2/clientes?sortBy=notas"))).andExpect(status().isBadRequest());
    }

    @Test
    void obtener_existenteEInexistente() throws Exception {
        when(service.obtener(1L)).thenReturn(cliente(1L));
        when(service.obtener(9L)).thenThrow(ClienteNotFoundException.porId(9L));

        mvc.perform(comoEncargado(get("/api/v2/clientes/1"))).andExpect(status().isOk());
        mvc.perform(comoEncargado(get("/api/v2/clientes/9"))).andExpect(status().isNotFound());
    }

    @Test
    void obtenerPorTelefono_devuelve200() throws Exception {
        when(service.obtenerPorTelefono("911111111")).thenReturn(cliente(1L));

        mvc.perform(comoEncargado(get("/api/v2/clientes/telefono/911111111")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    void obtenerPorTelefono_inexistente_devuelve404() throws Exception {
        when(service.obtenerPorTelefono("900000000")).thenThrow(new ClienteNotFoundException("No existe"));

        mvc.perform(comoEncargado(get("/api/v2/clientes/telefono/900000000"))).andExpect(status().isNotFound());
    }

    @Test
    void actualizar_devuelve200() throws Exception {
        when(service.actualizar(eq(1L), any())).thenReturn(cliente(1L));

        mvc.perform(comoEncargado(put("/api/v2/clientes/1").contentType(MediaType.APPLICATION_JSON).content(JSON)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cliente actualizado"));
    }
}
