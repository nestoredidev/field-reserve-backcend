package nestoredidev.bodega.categoria.infrastructure.adapter.input.rest;

import nestoredidev.bodega.categoria.application.ports.input.CategoriaServicePort;
import nestoredidev.bodega.categoria.application.ports.input.dto.CategoriaCommand;
import nestoredidev.bodega.categoria.application.ports.input.dto.CategoriaDto;
import nestoredidev.bodega.categoria.domain.exception.CategoriaNotFoundException;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;
import nestoredidev.common.exception.ConflictException;
import nestoredidev.common.exception.DomainException;
import nestoredidev.support.AbstractControllerTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoriaController.class)
class CategoriaControllerTest extends AbstractControllerTest {

    private static final String BASE = "/api/v2/bodega/categorias";

    @MockitoBean
    private CategoriaServicePort service;

    @Test
    void sinToken_devuelve401() throws Exception {
        mvc.perform(get(BASE)).andExpect(status().isUnauthorized());
    }

    @Test
    void crear_admin_devuelve201() throws Exception {
        when(service.crear(any(CategoriaCommand.class))).thenReturn(new CategoriaDto(1L, "Cerveza"));

        mvc.perform(comoAdmin(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Cerveza\"}")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Categoría creada"))
                .andExpect(jsonPath("$.data[0].nombre").value("Cerveza"));

        verify(service).crear(new CategoriaCommand("Cerveza"));
    }

    @Test
    void crear_encargado_devuelve403() throws Exception {
        mvc.perform(comoEncargado(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Cerveza\"}")))
                .andExpect(status().isForbidden());

        verify(service, never()).crear(any());
    }

    @Test
    void crear_nombreVacioOMuyLargo_devuelve400() throws Exception {
        mvc.perform(comoAdmin(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"\"}")))
                .andExpect(status().isBadRequest());
        mvc.perform(comoAdmin(post(BASE).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"" + "x".repeat(61) + "\"}"))).andExpect(status().isBadRequest());
    }

    @Test
    void crear_nombreDuplicado_devuelve409() throws Exception {
        when(service.crear(any())).thenThrow(new ConflictException("RN-CAT-01: ya existe la categoría 'Cerveza'"));

        mvc.perform(comoAdmin(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Cerveza\"}")))
                .andExpect(status().isConflict());
    }

    @Test
    void listar_encargadoPuede() throws Exception {
        when(service.listar(any(PageQuery.class))).thenReturn(
                new PageResult<>(List.of(new CategoriaDto(1L, "Cerveza"), new CategoriaDto(2L, "Agua")), 0, 10, 2, 1));

        mvc.perform(comoEncargado(get(BASE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        verify(service).listar(new PageQuery(0, 10, "nombre", "asc"));
    }

    @Test
    void obtener_existenteEInexistente() throws Exception {
        when(service.obtener(1L)).thenReturn(new CategoriaDto(1L, "Cerveza"));
        when(service.obtener(9L)).thenThrow(new CategoriaNotFoundException(9L));

        mvc.perform(comoEncargado(get(BASE + "/1"))).andExpect(status().isOk());
        mvc.perform(comoEncargado(get(BASE + "/9"))).andExpect(status().isNotFound());
    }

    @Test
    void actualizar_soloAdmin() throws Exception {
        when(service.actualizar(any(), any())).thenReturn(new CategoriaDto(1L, "Nueva"));

        mvc.perform(comoAdmin(put(BASE + "/1").contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Nueva\"}")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.message").value("Categoría actualizada"));
        mvc.perform(comoEncargado(put(BASE + "/1").contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Nueva\"}")))
                .andExpect(status().isForbidden());
    }

    @Test
    void eliminar_admin_devuelve204() throws Exception {
        mvc.perform(comoAdmin(delete(BASE + "/1"))).andExpect(status().isNoContent());

        verify(service).eliminar(1L);
    }

    @Test
    void eliminar_conProductos_devuelve422() throws Exception {
        org.mockito.Mockito.doThrow(new DomainException("RN-CAT-02: no se elimina una categoría que tiene productos"))
                .when(service).eliminar(1L);

        mvc.perform(comoAdmin(delete(BASE + "/1")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("RN-CAT-02")));
    }

    @Test
    void eliminar_encargado_devuelve403() throws Exception {
        mvc.perform(comoEncargado(delete(BASE + "/1"))).andExpect(status().isForbidden());

        verify(service, never()).eliminar(any());
    }
}
