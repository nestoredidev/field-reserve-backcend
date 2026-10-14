package nestoredidev.usuario.infrastructure.adapter.input.rest;

import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;
import nestoredidev.common.domain.Rol;
import nestoredidev.common.exception.AuthenticationFailedException;
import nestoredidev.common.exception.ConflictException;
import nestoredidev.common.exception.DomainException;
import nestoredidev.support.AbstractControllerTest;
import nestoredidev.usuario.application.ports.input.UsuarioServicePort;
import nestoredidev.usuario.application.ports.input.dto.ChangePasswordCommand;
import nestoredidev.usuario.application.ports.input.dto.CreateUsuarioCommand;
import nestoredidev.usuario.application.ports.input.dto.LoginCommand;
import nestoredidev.usuario.application.ports.input.dto.LoginResponse;
import nestoredidev.usuario.application.ports.input.dto.UpdateUsuarioCommand;
import nestoredidev.usuario.application.ports.input.dto.UsuarioDto;
import nestoredidev.usuario.domain.exception.UsuarioNotFoundException;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({UsuarioController.class, AuthController.class})
class UsuarioControllerTest extends AbstractControllerTest {

    @MockitoBean
    private UsuarioServicePort service;

    private static UsuarioDto usuario(Long id, String username, Rol rol) {
        return new UsuarioDto(id, "Nombre " + id, username, rol, true, OffsetDateTime.parse("2026-10-01T10:00:00-05:00"));
    }

    private static final String CREAR = """
            {"nombre":"Juan","username":"juan.perez","password":"password123","rol":"ENCARGADO"}""";

    // ================================================================ seguridad

    @Test
    void sinToken_devuelve401ConMensajeJson() throws Exception {
        mvc.perform(get("/api/v2/usuarios"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("No autenticado"));
    }

    @Test
    void tokenInvalido_devuelve401() throws Exception {
        mvc.perform(get("/api/v2/usuarios").header("Authorization", "Bearer basura"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioInactivoConTokenVigente_devuelve401() throws Exception {
        mvc.perform(get("/api/v2/usuarios").header("Authorization", "Bearer inactivo-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void encargado_noPuedeAdministrarUsuarios_403() throws Exception {
        mvc.perform(comoEncargado(get("/api/v2/usuarios")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("No tienes permisos para esta acción"));

        mvc.perform(comoEncargado(post("/api/v2/usuarios").contentType(MediaType.APPLICATION_JSON).content(CREAR)))
                .andExpect(status().isForbidden());
        mvc.perform(comoEncargado(patch("/api/v2/usuarios/5/desactivar"))).andExpect(status().isForbidden());
        mvc.perform(comoEncargado(put("/api/v2/usuarios/5/password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"password123\"}"))).andExpect(status().isForbidden());
    }

    // ================================================================ crear

    @Test
    void crear_admin_devuelve201ConElUsuarioCreado() throws Exception {
        when(service.crear(any(CreateUsuarioCommand.class))).thenReturn(usuario(5L, "juan.perez", Rol.ENCARGADO));

        mvc.perform(comoAdmin(post("/api/v2/usuarios").contentType(MediaType.APPLICATION_JSON).content(CREAR)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.message").value("Usuario creado"))
                .andExpect(jsonPath("$.data[0].id").value(5))
                .andExpect(jsonPath("$.data[0].username").value("juan.perez"))
                .andExpect(jsonPath("$.data[0].rol").value("ENCARGADO"))
                .andExpect(jsonPath("$.data[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data[0].password").doesNotExist());

        verify(service).crear(new CreateUsuarioCommand("Juan", "juan.perez", "password123", Rol.ENCARGADO));
    }

    @Test
    void crear_conCuerpoInvalido_devuelve400ConLosErrores() throws Exception {
        mvc.perform(comoAdmin(post("/api/v2/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"username\":\"AB\",\"password\":\"corta\"}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(4)));

        verify(service, never()).crear(any());
    }

    @Test
    void crear_conJsonMalformado_devuelve400() throws Exception {
        mvc.perform(comoAdmin(post("/api/v2/usuarios").contentType(MediaType.APPLICATION_JSON).content("{no es json")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request"));
    }

    @Test
    void crear_conRolDesconocido_devuelve400() throws Exception {
        mvc.perform(comoAdmin(post("/api/v2/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Juan\",\"username\":\"juan.perez\",\"password\":\"password123\",\"rol\":\"DIOS\"}")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crear_usernameDuplicado_devuelve409() throws Exception {
        when(service.crear(any())).thenThrow(new ConflictException("RN-USU-01: el username 'juan.perez' ya existe"));

        mvc.perform(comoAdmin(post("/api/v2/usuarios").contentType(MediaType.APPLICATION_JSON).content(CREAR)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.message").value("RN-USU-01: el username 'juan.perez' ya existe"));
    }

    // ================================================================ listar / obtener

    @Test
    void listar_devuelveLaPaginaConSusMetadatosYUsaOrdenPorDefecto() throws Exception {
        when(service.listar(any(PageQuery.class))).thenReturn(new PageResult<>(
                List.of(usuario(1L, "admin", Rol.ADMIN), usuario(2L, "enc", Rol.ENCARGADO)), 0, 10, 2, 1));

        mvc.perform(comoAdmin(get("/api/v2/usuarios")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Usuarios obtenidos"))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.total_elements").value(2))
                .andExpect(jsonPath("$.total_pages").value(1))
                .andExpect(jsonPath("$.has_next").value(false));

        verify(service).listar(new PageQuery(0, 10, "username", "asc"));
    }

    @Test
    void listar_conParametrosDePaginacion_losPasaAlServicio() throws Exception {
        when(service.listar(any(PageQuery.class))).thenReturn(new PageResult<>(List.of(), 2, 5, 0, 0));

        mvc.perform(comoAdmin(get("/api/v2/usuarios?page=2&size=5&sortBy=rol&direction=DESC")))
                .andExpect(status().isOk());

        verify(service).listar(new PageQuery(2, 5, "rol", "DESC"));
    }

    @Test
    void listar_sinResultados_devuelve200ConMensajeNoResults() throws Exception {
        when(service.listar(any(PageQuery.class))).thenReturn(new PageResult<>(List.of(), 0, 10, 0, 0));

        mvc.perform(comoAdmin(get("/api/v2/usuarios")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("No results found"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void listar_conParametrosInvalidos_devuelve400() throws Exception {
        mvc.perform(comoAdmin(get("/api/v2/usuarios?size=101"))).andExpect(status().isBadRequest());
        mvc.perform(comoAdmin(get("/api/v2/usuarios?page=-1"))).andExpect(status().isBadRequest());
        mvc.perform(comoAdmin(get("/api/v2/usuarios?direction=sideways"))).andExpect(status().isBadRequest());
        mvc.perform(comoAdmin(get("/api/v2/usuarios?sortBy=passwordHash")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.startsWith("sortBy must be one of")));

        verify(service, never()).listar(any());
    }

    @Test
    void obtener_existente_devuelve200() throws Exception {
        when(service.obtener(5L)).thenReturn(usuario(5L, "juan.perez", Rol.ENCARGADO));

        mvc.perform(comoAdmin(get("/api/v2/usuarios/5")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].username").value("juan.perez"));
    }

    @Test
    void obtener_inexistente_devuelve404() throws Exception {
        when(service.obtener(9L)).thenThrow(new UsuarioNotFoundException(9L));

        mvc.perform(comoAdmin(get("/api/v2/usuarios/9")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Usuario con id 9 no existe"));
    }

    @Test
    void obtener_conIdNoNumerico_devuelve400() throws Exception {
        mvc.perform(comoAdmin(get("/api/v2/usuarios/abc"))).andExpect(status().isBadRequest());
    }

    // ================================================================ actualizar / password / estado

    @Test
    void actualizar_devuelve200() throws Exception {
        when(service.actualizar(eq(5L), any(UpdateUsuarioCommand.class))).thenReturn(usuario(5L, "juan.perez", Rol.ADMIN));

        mvc.perform(comoAdmin(put("/api/v2/usuarios/5").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Juan\",\"rol\":\"ADMIN\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Usuario actualizado"))
                .andExpect(jsonPath("$.data[0].rol").value("ADMIN"));

        verify(service).actualizar(5L, new UpdateUsuarioCommand("Juan", Rol.ADMIN));
    }

    @Test
    void actualizar_sinRol_devuelve400() throws Exception {
        mvc.perform(comoAdmin(put("/api/v2/usuarios/5").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Juan\"}")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actualizar_quitandoseAdminASiMismo_devuelve422() throws Exception {
        when(service.actualizar(eq(1L), any())).thenThrow(new DomainException("RN-USU-04: no puedes quitarte el rol ADMIN a ti mismo"));

        mvc.perform(comoAdmin(put("/api/v2/usuarios/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Admin\",\"rol\":\"ENCARGADO\"}")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(422));
    }

    @Test
    void cambiarPassword_devuelve204SinCuerpo() throws Exception {
        mvc.perform(comoAdmin(put("/api/v2/usuarios/5/password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"nuevaClave1\"}")))
                .andExpect(status().isNoContent());

        verify(service).cambiarPassword(5L, new ChangePasswordCommand("nuevaClave1"));
    }

    @Test
    void cambiarPassword_conClaveCorta_devuelve400() throws Exception {
        mvc.perform(comoAdmin(put("/api/v2/usuarios/5/password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"123\"}")))
                .andExpect(status().isBadRequest());

        verify(service, never()).cambiarPassword(any(), any());
    }

    @Test
    void activarYDesactivar_devuelven200() throws Exception {
        when(service.activar(5L)).thenReturn(usuario(5L, "juan.perez", Rol.ENCARGADO));
        when(service.desactivar(5L)).thenReturn(new UsuarioDto(5L, "N", "juan.perez", Rol.ENCARGADO, false, null));

        mvc.perform(comoAdmin(patch("/api/v2/usuarios/5/activar")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.message").value("Usuario activado"));
        mvc.perform(comoAdmin(patch("/api/v2/usuarios/5/desactivar")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].activo").value(false));
    }

    // ================================================================ auth

    @Test
    void login_esPublicoYDevuelveElToken() throws Exception {
        when(service.login(any(LoginCommand.class))).thenReturn(
                new LoginResponse("jwt-abc", "Bearer", 28800, usuario(1L, "admin", Rol.ADMIN)));

        mvc.perform(post("/api/v2/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"Admin12345\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login exitoso"))
                .andExpect(jsonPath("$.data[0].token").value("jwt-abc"))
                .andExpect(jsonPath("$.data[0].tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data[0].expiresInSeconds").value(28800))
                .andExpect(jsonPath("$.data[0].usuario.username").value("admin"));

        verify(service).login(new LoginCommand("admin", "Admin12345"));
    }

    @Test
    void login_conCredencialesIncorrectas_devuelve401() throws Exception {
        when(service.login(any())).thenThrow(new AuthenticationFailedException("Usuario o contraseña incorrectos"));

        mvc.perform(post("/api/v2/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"mala\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Usuario o contraseña incorrectos"));
    }

    @Test
    void login_sinCampos_devuelve400() throws Exception {
        mvc.perform(post("/api/v2/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        verify(service, never()).login(any());
    }

    @Test
    void me_devuelveElUsuarioAutenticadoSacadoDelToken() throws Exception {
        when(service.obtener(ENCARGADO_ID)).thenReturn(usuario(ENCARGADO_ID, "encargado", Rol.ENCARGADO));

        mvc.perform(comoEncargado(get("/api/v2/auth/me")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].username").value("encargado"));

        verify(service).obtener(ENCARGADO_ID);
    }

    @Test
    void me_sinToken_devuelve401() throws Exception {
        mvc.perform(get("/api/v2/auth/me")).andExpect(status().isUnauthorized());
    }
}
